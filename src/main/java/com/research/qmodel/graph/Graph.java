package com.research.qmodel.graph;

import com.research.qmodel.repos.CommitRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

/**
 * Computes per-commit graph metrics and persists them.
 *
 * Metrics:
 *  - in/out degree
 *  - min/max depth (to any root)
 *  - distToBranchStart: FP hops to start of current FP segment
 *  - numBranches (time-aware): number of current heads (local + remote + HEAD) whose tips reach
 *      the commit AND whose FP-segment start time <= commit time
 *  - upstreamHeadsUnique: number of distinct source FP-segments merged into the current FP-segment
 *      strictly BEFORE the commit
 *  - daysSinceLastMerge: days since the last merge on the same FP-segment strictly BEFORE the commit
 */
@Component
@Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
public class Graph extends GitMaintainable {
    // NEW: for debugging/inspection — path from FP-segment start to this commit (inclusive)
    private final Map<String, List<String>> fpPath = new HashMap<>();

    // (optional) segment start for each sha — handy to group commits by segment
    private final Map<String, String> fpSegmentStartOf = new HashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);
    private static final int SECS_PER_DAY = 24 * 60 * 60;

    // Core per-node and adjacency
    private final Map<String, Vertex> vertices          = new LinkedHashMap<>();
    private final Map<String, Set<String>> parents      = new HashMap<>(); // child -> parents
    private final Map<String, Set<String>> children     = new HashMap<>(); // parent -> children
    private final Map<String, String> firstParent       = new HashMap<>(); // child -> parent(0)
    private final Map<String, Integer> commitTimeSec    = new HashMap<>(); // sha -> epoch sec
    private final Map<String, String> fpSegStartMemo    = new HashMap<>(); // sha -> FP segment start sha

    private long edgeCount = 0L;

    @Autowired
    private CommitRepository commitRepository;

    /* ============================ Build helpers ============================ */

    private void addVertex(String sha) {
        vertices.putIfAbsent(sha, new Vertex(sha));
    }

    /** Add directed edge parent -> child (idempotent) and maintain degrees/adjacency/FP pointer. */
    private void addEdge(RevCommit parent, RevCommit child, int parentIndex) {
        String p = parent.getId().getName();
        String c = child.getId().getName();

        addVertex(p);
        addVertex(c);
        Vertex vp = vertices.get(p);
        Vertex vc = vertices.get(c);

        if (vp.getNeighbors().add(c)) {
            vp.incrementOutDegree();
            vc.incrementInDegree();
            parents.computeIfAbsent(c, k -> new HashSet<>()).add(p);
            children.computeIfAbsent(p, k -> new HashSet<>()).add(c);
            edgeCount++;
        }
        if (parentIndex == 0) {
            firstParent.put(c, p);
        }
    }
    /** Compute distToBranchStart AND choose firstParent to maximize continuity. */
    private Map<String, Integer> computeDistToBranchStartDynamic(List<String> topo) {
        Map<String, Integer> dist = new HashMap<>(vertices.size() * 2);

        for (String sha : topo) {
            Set<String> ps = parents.getOrDefault(sha, Collections.emptySet());
            if (ps.isEmpty()) {
                // root-like node
                dist.put(sha, 0);
                firstParent.put(sha, null);
                continue;
            }

            String bestP = null;
            int bestDist = -1;

            for (String p : ps) {
                // Split ABOVE if p has more than one child
                int parentOut = children.getOrDefault(p, Collections.emptySet()).size();
                int cand = (parentOut > 1) ? 0 : (dist.getOrDefault(p, 0) + 1);

                if (cand > bestDist) {
                    bestDist = cand;
                    bestP = p;
                } else if (cand == bestDist && bestP != null) {
                    // Tie-breaker: earlier parent commit time wins (older = more "mainline-ish")
                    int tp = commitTimeSec.getOrDefault(p, Integer.MAX_VALUE);
                    int tb = commitTimeSec.getOrDefault(bestP, Integer.MAX_VALUE);
                    if (tp < tb) bestP = p;
                    // (optional second tie-breaker: lexicographic SHA)
                    // else if (tp == tb && p.compareTo(bestP) < 0) bestP = p;
                }
            }

            // Record chosen FP and distance
            firstParent.put(sha, bestP);
            dist.put(sha, Math.max(0, bestDist));
        }
        return dist;
    }

    private double averageDegree() {
        int v = vertices.size();
        return v == 0 ? 0.0 : (double) edgeCount / (double) v;
    }

    /* ============================ FP segment ============================ */

    /** First-parent segment start: walk FP back until hitting a split (parent's out-degree>1) or root. */
    private String fpSegmentStart(String sha) {
        String cached = fpSegStartMemo.get(sha);
        if (cached != null) return cached;

        String cur = sha;
        while (true) {
            String p = firstParent.get(cur);
            if (p == null) break;
            int parentOut = children.getOrDefault(p, Collections.emptySet()).size();
            if (parentOut > 1) break; // split above; current is segment start
            cur = p;
        }
        fpSegStartMemo.put(sha, cur);
        return cur;
    }

    /* ============================ DP metrics ============================ */

    /** min/max depth to any root via O(V+E) DP over topo (parents before children). */
    private void computeDepthsDP(List<String> topo) {
        Map<String, Integer> minDepth = new HashMap<>(vertices.size() * 2);
        Map<String, Integer> maxDepth = new HashMap<>(vertices.size() * 2);

        for (String sha : topo) {
            Set<String> ps = parents.getOrDefault(sha, Collections.emptySet());
            int mn, mx;
            if (ps.isEmpty()) {
                mn = 0; mx = 0;
            } else {
                mn = Integer.MAX_VALUE;
                mx = Integer.MIN_VALUE;
                for (String p : ps) {
                    int pmn = minDepth.getOrDefault(p, 0) + 1;
                    int pmx = maxDepth.getOrDefault(p, 0) + 1;
                    if (pmn < mn) mn = pmn;
                    if (pmx > mx) mx = pmx;
                }
            }
            minDepth.put(sha, mn);
            maxDepth.put(sha, mx);
            Vertex v = vertices.get(sha);
            v.setMinDepthOfCommitHistory(mn);
            v.setMaxDepthOfCommitHistory(mx);
        }
    }

    /** distToBranchStart for every commit: FP hops to segment start. */
    private Map<String, Integer> computeDistToBranchStart(List<String> topo) {
        Map<String, Integer> dist = new HashMap<>(vertices.size() * 2);
        for (String sha : topo) {
            String p = firstParent.get(sha);
            if (p == null) {
                dist.put(sha, 0);
            } else {
                int parentOut = children.getOrDefault(p, Collections.emptySet()).size();
                if (parentOut > 1) {
                    dist.put(sha, 0);
                }
                else {
                    dist.put(sha, dist.getOrDefault(p, 0) + 1);
                }
            }
        }
        return dist;
    }

    /**
     * upstreamHeadsUnique (distinct merges before the commit):
     * Along the FP chain, accumulate unique source FP-segment-start shas from non-FP parents.
     * For node sha, count BEFORE sha (so merges at sha are NOT counted).
     */
    private Map<String, Integer> computeDistinctMergesBefore(List<String> topo) {
        Map<String, Set<String>> inclAt = new HashMap<>(vertices.size() * 2);
        Map<String, Integer> before    = new HashMap<>(vertices.size() * 2);

        for (String sha : topo) {
            String fp = firstParent.get(sha);
            Set<String> base = (fp == null) ? Collections.emptySet()
                    : inclAt.getOrDefault(fp, Collections.emptySet());
            // Count before current
            before.put(sha, base.size());

            // Build "including" set for children by adding merges at sha
            Set<String> here = new HashSet<>(base);
            Set<String> ps = parents.getOrDefault(sha, Collections.emptySet());
            if (ps.size() > 1) {
                for (String p : ps) {
                    if (fp != null && fp.equals(p)) continue;
                    here.add(fpSegmentStart(p));
                }
            }
            inclAt.put(sha, here);
        }
        return before;
    }

    /**
     * daysSinceLastMerge on the same FP segment (strictly before sha).
     * We scan topo (parents before children) and track last merge time per FP segment.
     */
    private Map<String, Integer> computeDaysSinceLastMerge(List<String> topo) {
        Map<String, Integer> outDays = new HashMap<>(vertices.size() * 2);
        Map<String, Integer> lastMergeTimeBySeg = new HashMap<>();

        for (String sha : topo) {
            String seg = fpSegmentStart(sha);
            Integer last = lastMergeTimeBySeg.get(seg);
            int t = commitTimeSec.getOrDefault(sha, 0);

            if (last == null) outDays.put(sha, 0);
            else {
                int deltaSec = Math.max(0, t - last);
                outDays.put(sha, deltaSec / SECS_PER_DAY);
            }

            // Update last merge time if current is a merge (not counted "before" current)
            if (parents.getOrDefault(sha, Collections.emptySet()).size() > 1) {
                lastMergeTimeBySeg.put(seg, t);
            }
        }
        return outDays;
    }

    /* ============================ Time-aware branches ============================ */

    private static final class HeadInfo {
        final RevCommit tip;
        final String tipSha;
        final String segStartSha;
        final int    segStartTime;
        HeadInfo(RevCommit tip, String tipSha, String segStartSha, int segStartTime) {
            this.tip = tip; this.tipSha = tipSha; this.segStartSha = segStartSha; this.segStartTime = segStartTime;
        }
    }

    /** Collect local+remote+HEAD tips; dedupe by tip commit id; compute seg start and its time. */
    private List<HeadInfo> collectHeadsInfo(Repository repo) throws Exception {
        Map<String, HeadInfo> byTip = new LinkedHashMap<>();
        try (RevWalk w = new RevWalk(repo)) {
            // locals
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/heads/")) {
                ObjectId id = r.getObjectId(); if (id == null) continue;
                RevCommit tip = w.parseCommit(id);
                String tipSha = tip.getId().getName();
                String base   = fpSegmentStart(tipSha);
                int baseTime  = commitTimeSec.getOrDefault(base, Integer.MIN_VALUE);
                byTip.putIfAbsent(tipSha, new HeadInfo(tip, tipSha, base, baseTime));
            }
            // remotes (ignore origin/HEAD alias)
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/remotes/")) {
                if ("refs/remotes/origin/HEAD".equals(r.getName())) continue;
                ObjectId id = r.getObjectId(); if (id == null) continue;
                RevCommit tip = w.parseCommit(id);
                String tipSha = tip.getId().getName();
                String base   = fpSegmentStart(tipSha);
                int baseTime  = commitTimeSec.getOrDefault(base, Integer.MIN_VALUE);
                byTip.putIfAbsent(tipSha, new HeadInfo(tip, tipSha, base, baseTime));
            }
            // HEAD
            ObjectId headId = repo.resolve("HEAD");
            if (headId != null) {
                RevCommit tip = w.parseCommit(headId);
                String tipSha = tip.getId().getName();
                String base   = fpSegmentStart(tipSha);
                int baseTime  = commitTimeSec.getOrDefault(base, Integer.MIN_VALUE);
                byTip.putIfAbsent(tipSha, new HeadInfo(tip, tipSha, base, baseTime));
            }
        }
        return new ArrayList<>(byTip.values());
    }

    /** Precompute reachable sets per head tip for deterministic branch counting. */
    private List<Set<String>> computeReachableSetsPerHead(Repository repo, List<HeadInfo> heads) throws Exception {
        List<Set<String>> reachSets = new ArrayList<>(heads.size());
        for (HeadInfo hi : heads) {
            Set<String> seen = new HashSet<>();
            try (RevWalk w = new RevWalk(repo)) {
                w.markStart(hi.tip);
                for (RevCommit c : w) {
                    seen.add(c.getId().getName());
                }
            }
            reachSets.add(seen);
        }
        return reachSets;
    }

    /**
     * numBranches (time-aware):
     * Count heads whose reachable set contains sha AND whose FP-segment start time <= t(sha).
     */
    private Map<String, Integer> computeBranchCountsTA(Repository repo, List<String> topo) throws Exception {
        List<HeadInfo> heads = collectHeadsInfo(repo);
        List<Set<String>> reach = computeReachableSetsPerHead(repo, heads);

        Map<String, Integer> out = new HashMap<>(topo.size() * 2);
        for (String sha : topo) {
            int t = commitTimeSec.getOrDefault(sha, Integer.MIN_VALUE);
            int cnt = 0;
            for (int i = 0; i < heads.size(); i++) {
                HeadInfo h = heads.get(i);
                if (h.segStartTime <= t && reach.get(i).contains(sha)) cnt++;
            }
            out.put(sha, cnt);
        }
        return out;
    }

    /* ============================ Build + persist ============================ */

    @Transactional
    public Graph buildGraph(String repoPath) {
        vertices.clear();
        parents.clear();
        children.clear();
        firstParent.clear();
        commitTimeSec.clear();
        fpSegStartMemo.clear();
        edgeCount = 0L;

        try (Git git = Git.open(new File(repoPath));
             Repository repo = git.getRepository();
             RevWalk walk = new RevWalk(repo)) {

            // Cover full reachable graph: mark local + remote heads
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/heads/")) {
                ObjectId id = r.getObjectId(); if (id != null) walk.markStart(walk.parseCommit(id));
            }
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/remotes/")) {
                ObjectId id = r.getObjectId(); if (id != null) walk.markStart(walk.parseCommit(id));
            }
            walk.sort(RevSort.TOPO);
            walk.sort(RevSort.REVERSE);

            List<String> topo = new ArrayList<>(128_000);

            // Build DAG
            for (RevCommit c : walk) {
                String sha = c.getId().getName();
                topo.add(sha);
                addVertex(sha);
                commitTimeSec.put(sha, c.getCommitTime());

                int pc = c.getParentCount();
                for (int i = 0; i < pc; i++) {
                    addEdge(c.getParent(i), c, i);
                }
                Vertex v = vertices.get(sha);
                v.setMerge(pc > 1);
                v.setTimestamp(new Date(1000L * (long) c.getCommitTime()));
            }
            LOG.info("DAG built: nodes={}, edges={}", vertices.size(), edgeCount);

            // Metrics
            computeDepthsDP(topo);
            Map<String, Integer> distToStart     = computeDistToBranchStartWithPath(topo);
            Map<String, Integer> upstreamUnique  = computeDistinctMergesBefore(topo);
            Map<String, Integer> daysSinceLast   = computeDaysSinceLastMerge(topo);
            Map<String, Integer> branchCountTA   = computeBranchCountsTA(repo, topo);
            final double avgDeg = averageDegree();

            // Persist
            int updated = 0;
            for (String sha : topo) {
                Vertex v = vertices.get(sha);
                int inDeg    = v.getInDegree();
                int outDeg   = v.getOutDegree();
                int mergeCnt = v.isMerge() ? 1 : 0;
                int minDepth = v.getMinDepthOfCommitHistory();
                int maxDepth = v.getMaxDepthOfCommitHistory();
                int branches = branchCountTA.getOrDefault(sha, 0);
                int dist     = distToStart.getOrDefault(sha, 0);
                int upstream = upstreamUnique.getOrDefault(sha, 0);
                int days     = daysSinceLast.getOrDefault(sha, 0);

                if ((updated % 2000) == 0) {
                    LOG.info("sha={} branchesTA={} dist={} upstream={} daysSinceLast={}",
                            sha, branches, dist, upstream, days);
                }

                try {
                    commitRepository.updateGraphMetrics(
                            sha,
                            inDeg,
                            outDeg,
                            mergeCnt,
                            minDepth,
                            maxDepth,
                            branches,
                            avgDeg,
                            dist,
                            upstream,
                            days
                    );
                } catch (Exception ex) {
                    LOG.debug("updateGraphMetrics failed for {}: {}", sha, ex.getMessage());
                }
                updated++;
            }
            LOG.info("Persisted {} rows. avgDeg={}", updated, avgDeg);

        } catch (Exception e) {
            LOG.error("Graph build failed: {}", e.getMessage(), e);
        }
        return this;
    }



    /**
     * Computes distToBranchStart for every commit AND stores the FP-segment path for each sha.
     * Semantics:
     *  - distance resets at the first child after a split (parent's out-degree > 1)
     *  - path is the list from the FP-segment start to sha (inclusive)
     */
    private Map<String, Integer> computeDistToBranchStartWithPath(List<String> topo) {
        Map<String, Integer> dist = new HashMap<>(vertices.size() * 2);
        fpPath.clear();
        fpSegmentStartOf.clear();

        for (String sha : topo) {
            String p = firstParent.get(sha);

            // ROOT or no FP parent: start a new segment
            if (p == null) {
                dist.put(sha, 0);
                List<String> path = new ArrayList<>(1);
                path.add(sha);
                fpPath.put(sha, path);
                fpSegmentStartOf.put(sha, sha);
                continue;
            }

            // Is there a split directly above (parent has multiple children)?
            int parentOut = children.getOrDefault(p, Collections.emptySet()).size();
            boolean splitAbove = parentOut > 1;

            if (splitAbove) {
                // reset: sha is the first node in a new FP segment
                dist.put(sha, 0);
                List<String> path = new ArrayList<>(1);
                path.add(sha);
                fpPath.put(sha, path);
                // the segment start is sha itself
                fpSegmentStartOf.put(sha, sha);
            } else {
                // continue the parent's FP path
                int dParent = dist.getOrDefault(p, 0);
                dist.put(sha, dParent + 1);

                List<String> parentPath = fpPath.get(p);
                if (parentPath == null) {
                    // Shouldn't happen if topo order is consistent, but guard anyway:
                    parentPath = new ArrayList<>();
                    parentPath.add(p);
                }
                List<String> path = new ArrayList<>(parentPath.size() + 1);
                path.addAll(parentPath);
                path.add(sha);
                fpPath.put(sha, path);

                // segment start follows parent's
                fpSegmentStartOf.put(sha, fpSegmentStartOf.getOrDefault(p, p));
            }
        }
        return dist;
    }

    /* Optional debug helper */
    public List<RevCommit> getLinearAncestors(Repository repo, RevCommit startCommit) throws Exception {
        List<RevCommit> ancestors = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit cur = startCommit;
            while (cur.getParentCount() > 0) {
                RevCommit p = walk.parseCommit(cur.getParent(0));
                ancestors.add(p);
                cur = p;
            }
        }
        return ancestors;
    }
    /** Returns an unmodifiable view of the FP path (from segment start to sha). */
    public List<String> getFpPath(String sha) {
        List<String> path = fpPath.get(sha);
        return (path == null) ? Collections.emptyList() : Collections.unmodifiableList(path);
    }

    /** Returns the segment start sha of this commit (equals sha if distance==0). */
    public String getFpSegmentStart(String sha) {
        return fpSegmentStartOf.getOrDefault(sha, sha);
    }

    public Map<String, Vertex> getVertices() { return vertices; }
}
