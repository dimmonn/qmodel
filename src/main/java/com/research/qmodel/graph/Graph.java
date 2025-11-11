package com.research.qmodel.graph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.research.qmodel.repos.CommitRepository;
import lombok.Getter;
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

@Component
@Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
public class Graph extends GitMaintainable {

    private final Map<String, List<String>> fpPath = new HashMap<>();

    private final Map<String, String> fpSegmentStartOf = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);

    private static final int SECS_PER_DAY = 24 * 60 * 60;

    @JsonProperty("vertices")
    private final Map<String, Vertex> vertices = new LinkedHashMap<>();

    private final Map<String, Set<String>> parents = new HashMap<>();

    private final Map<String, Set<String>> children = new HashMap<>();

    private final Map<String, String> firstParent = new HashMap<>();

    private final Map<String, Integer> commitTimeSec = new HashMap<>();

    private final Map<String, String> fpSegStartMemo = new HashMap<>();


    private long edgeCount = 0L;

    @Autowired

    private CommitRepository commitRepository;

    private void addVertex(String sha) {
        vertices.putIfAbsent(sha, new Vertex(sha));
    }

    private void addEdge(RevCommit parent, RevCommit child) {
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
    }

    private String chooseFirstParentByContinuity(String sha, Map<String, Integer> distSoFar) {
        Set<String> ps = parents.getOrDefault(sha, Collections.emptySet());
        if (ps.isEmpty()) return null;
        if (ps.size() == 1) return ps.iterator().next();

        String best = null;
        int bestCand = Integer.MIN_VALUE;

        for (String p : ps) {
            int parentOut = children.getOrDefault(p, Collections.emptySet()).size();
            int cand = (parentOut > 1) ? 0 : (distSoFar.getOrDefault(p, 0) + 1);
            if (cand > bestCand) {
                bestCand = cand;
                best = p;
            } else if (cand == bestCand && best != null) {
                int tp = commitTimeSec.getOrDefault(p, Integer.MAX_VALUE);
                int tb = commitTimeSec.getOrDefault(best, Integer.MAX_VALUE);
                if (tp < tb || (tp == tb && p.compareTo(best) < 0)) {
                    best = p;
                }
            }
        }
        return best;
    }

    private Map<String, Integer> computeFpContinuityAndPaths(List<String> topo) {
        Map<String, Integer> dist = new HashMap<>(vertices.size() * 2);

        firstParent.clear();
        fpPath.clear();
        fpSegmentStartOf.clear();
        fpSegStartMemo.clear();

        for (String sha : topo) {
            Set<String> ps = parents.getOrDefault(sha, Collections.emptySet());
            if (ps.isEmpty()) {
                firstParent.put(sha, null);
                dist.put(sha, 0);
                fpPath.put(sha, new ArrayList<>(List.of(sha)));
                fpSegmentStartOf.put(sha, sha);
                continue;
            }
            String fp = chooseFirstParentByContinuity(sha, dist);
            firstParent.put(sha, fp);

            if (fp == null) {
                dist.put(sha, 0);
                fpPath.put(sha, new ArrayList<>(List.of(sha)));
                fpSegmentStartOf.put(sha, sha);
                continue;
            }
            int parentOut = children.getOrDefault(fp, Collections.emptySet()).size();
            if (parentOut > 1) {
                dist.put(sha, 0);
                fpPath.put(sha, new ArrayList<>(List.of(sha)));
                fpSegmentStartOf.put(sha, sha);
            } else {
                int dParent = dist.getOrDefault(fp, 0);
                dist.put(sha, dParent + 1);

                List<String> parentPath = fpPath.get(fp);
                if (parentPath == null) parentPath = new ArrayList<>(List.of(fp));
                List<String> path = new ArrayList<>(parentPath.size() + 1);
                path.addAll(parentPath);
                path.add(sha);
                fpPath.put(sha, path);

                fpSegmentStartOf.put(sha, fpSegmentStartOf.getOrDefault(fp, fp));
            }
        }
        return dist;
    }


    private double averageDegree() {
        int v = vertices.size();
        return v == 0 ? 0.0 : (double) edgeCount / (double) v;
    }

    private String fpSegmentStart(String sha) {
        String cached = fpSegStartMemo.get(sha);
        if (cached != null) return cached;

        String cur = sha;
        while (true) {
            String p = firstParent.get(cur);
            if (p == null) break;
            int parentOut = children.getOrDefault(p, Collections.emptySet()).size();
            if (parentOut > 1) break;
            cur = p;
        }
        fpSegStartMemo.put(sha, cur);
        return cur;
    }

    private void computeDepthsDP(List<String> topo) {
        Map<String, Integer> minDepth = new HashMap<>(vertices.size() * 2);
        Map<String, Integer> maxDepth = new HashMap<>(vertices.size() * 2);

        for (String sha : topo) {
            Set<String> ps = parents.getOrDefault(sha, Collections.emptySet());
            int mn, mx;
            if (ps.isEmpty()) {
                mn = 0;
                mx = 0;
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
                } else {
                    dist.put(sha, dist.getOrDefault(p, 0) + 1);
                }
            }
        }
        return dist;
    }

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

            if (parents.getOrDefault(sha, Collections.emptySet()).size() > 1) {
                lastMergeTimeBySeg.put(seg, t);
            }
        }
        return outDays;
    }

    private static final class HeadInfo {
        final RevCommit tip;
        final String tipSha;
        final String segStartSha;
        final int segStartTime;

        HeadInfo(RevCommit tip, String tipSha, String segStartSha, int segStartTime) {
            this.tip = tip;
            this.tipSha = tipSha;
            this.segStartSha = segStartSha;
            this.segStartTime = segStartTime;
        }
    }

    private List<HeadInfo> collectHeadsInfo(Repository repo) throws Exception {
        Map<String, HeadInfo> byTip = new LinkedHashMap<>();
        try (RevWalk w = new RevWalk(repo)) {
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/heads/")) {
                ObjectId id = r.getObjectId();
                if (id == null) continue;
                RevCommit tip = w.parseCommit(id);
                String tipSha = tip.getId().getName();
                String base = fpSegmentStart(tipSha);
                int baseTime = commitTimeSec.getOrDefault(base, Integer.MIN_VALUE);
                byTip.putIfAbsent(tipSha, new HeadInfo(tip, tipSha, base, baseTime));
            }
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/remotes/")) {
                if ("refs/remotes/origin/HEAD".equals(r.getName())) continue;
                ObjectId id = r.getObjectId();
                if (id == null) continue;
                RevCommit tip = w.parseCommit(id);
                String tipSha = tip.getId().getName();
                String base = fpSegmentStart(tipSha);
                int baseTime = commitTimeSec.getOrDefault(base, Integer.MIN_VALUE);
                byTip.putIfAbsent(tipSha, new HeadInfo(tip, tipSha, base, baseTime));
            }
            ObjectId headId = repo.resolve("HEAD");
            if (headId != null) {
                RevCommit tip = w.parseCommit(headId);
                String tipSha = tip.getId().getName();
                String base = fpSegmentStart(tipSha);
                int baseTime = commitTimeSec.getOrDefault(base, Integer.MIN_VALUE);
                byTip.putIfAbsent(tipSha, new HeadInfo(tip, tipSha, base, baseTime));
            }
        }
        return new ArrayList<>(byTip.values());
    }

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


    private final Map<String, Set<String>> distinctSourcesBeforeSet = new HashMap<>();

    private Map<String, Integer> computeDistinctMergesBefore(List<String> topo) {
        Map<String, Set<String>> inclAt = new HashMap<>(vertices.size() * 2);
        Map<String, Integer> before = new HashMap<>(vertices.size() * 2);

        for (String sha : topo) {
            String fp = firstParent.get(sha);
            Set<String> base = (fp == null) ? Collections.emptySet()
                    : inclAt.getOrDefault(fp, Collections.emptySet());

            before.put(sha, base.size());
            distinctSourcesBeforeSet.put(sha, new HashSet<>(base));

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
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/heads/")) {
                ObjectId id = r.getObjectId();
                if (id != null) walk.markStart(walk.parseCommit(id));
            }
            for (Ref r : repo.getRefDatabase().getRefsByPrefix("refs/remotes/")) {
                ObjectId id = r.getObjectId();
                if (id != null) walk.markStart(walk.parseCommit(id));
            }
            walk.sort(RevSort.TOPO);
            walk.sort(RevSort.REVERSE);

            List<String> topo = new ArrayList<>(128_000);

            for (RevCommit c : walk) {
                String sha = c.getId().getName();
                topo.add(sha);
                addVertex(sha);
                commitTimeSec.put(sha, c.getCommitTime());

                int pc = c.getParentCount();
                for (int i = 0; i < pc; i++) {
                    addEdge(c.getParent(i), c);
                }
                Vertex v = vertices.get(sha);
                v.setMerge(pc > 1);
                v.setTimestamp(new Date(1000L * (long) c.getCommitTime()));
            }
            LOG.info("DAG built: nodes={}, edges={}", vertices.size(), edgeCount);

            computeDepthsDP(topo);
            Map<String, Integer> distToStart = computeFpContinuityAndPaths(topo);
            Map<String, Integer> upstreamUnique = computeDistinctMergesBefore(topo);
            Map<String, Integer> daysSinceLast = computeDaysSinceLastMerge(topo);
            Map<String, Integer> branchCountTA = computeBranchCountsTA(repo, topo);
            final double avgDeg = averageDegree();
            int updated = 0;
            for (String sha : topo) {
                Vertex v = vertices.get(sha);
                int inDeg = v.getInDegree();
                int outDeg = v.getOutDegree();
                int mergeCnt = v.isMerge() ? 1 : 0;
                int minDepth = v.getMinDepthOfCommitHistory();
                int maxDepth = v.getMaxDepthOfCommitHistory();
                int branches = branchCountTA.getOrDefault(sha, 0);
                int dist = distToStart.getOrDefault(sha, 0);
                int upstream = upstreamUnique.getOrDefault(sha, 0);
                int days = daysSinceLast.getOrDefault(sha, 0);

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

    public Map<String, Vertex> getVertices() {
        return Map.copyOf(vertices);
    }
}
