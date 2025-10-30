package com.research.qmodel.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.repos.CommitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

@Component
@Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
public class Graph extends GitMaintainable {
    private final Logger LOGGER = LoggerFactory.getLogger(Graph.class);

    // Final per-node metrics live here during the build
    private final Map<String, Vertex> vertices = new LinkedHashMap<>();

    // Reverse adjacency: child -> parents, for fast depth computation
    private final Map<String, Set<String>> parents = new HashMap<>();

    // Caches for depth DP
    private final Map<String, Integer> depthCacheMax = new HashMap<>();
    private final Map<String, Integer> depthCacheMin = new HashMap<>();

    // Keep a running edge count (so avg degree is O(1))
    private long edgeCount = 0L;

    @Autowired private CommitRepository commitRepository;
    @Autowired private ObjectMapper objectMapper; // keep if used elsewhere

    /** Ensure a vertex exists */
    public void addVertex(String sha) {
        vertices.putIfAbsent(sha, new Vertex(sha));
    }

    /** Add directed edge parent -> child, maintaining degrees, reverse map, and edge count */
    public void addEdge(String parentSha, String childSha) {
        addVertex(parentSha);
        addVertex(childSha);
        Vertex parent = vertices.get(parentSha);
        Vertex child = vertices.get(childSha);

        // Use the set to avoid double-counting the same edge
        if (parent.getNeighbors().add(childSha)) {
            parent.incrementOutDegree();
            child.incrementInDegree();
            parents.computeIfAbsent(childSha, k -> new HashSet<>()).add(parentSha);
            edgeCount++;
        }
    }
    public Map<String, Vertex> getVerticesMap() {
        return vertices;
    }

    /** Number of vertices currently in memory */
    public int getNumberOfVertices() {
        return vertices.size();
    }

    /** Number of edges (maintained incrementally) */
    public long getNumberOfEdges() {
        return edgeCount;
    }

    /** Global average degree (edges / vertices) */
    public double getAverageDegree() {
        int v = getNumberOfVertices();
        return v == 0 ? 0.0 : (double) edgeCount / v;
    }

    /** O(#parents) lookup thanks to reverse map */
    public Set<String> findParents(String sha) {
        return parents.getOrDefault(sha, Collections.emptySet());
    }

    /** Max depth to a root (memoized) */
    public int getMaximumDepth(String sha) {
        Integer cached = depthCacheMax.get(sha);
        if (cached != null) return cached;

        Set<String> ps = findParents(sha);
        if (ps.isEmpty()) {
            depthCacheMax.put(sha, 0);
            return 0;
        }
        int max = Integer.MIN_VALUE;
        for (String p : ps) {
            max = Math.max(max, getMaximumDepth(p) + 1);
        }
        depthCacheMax.put(sha, max);
        return max;
    }

    /** First-parent chain (your existing notion of "linear ancestors") */
    public List<RevCommit> getLinearAncestors(Repository repo, RevCommit startCommit) throws IOException {
        List<RevCommit> ancestors = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit current = startCommit;
            while (current.getParentCount() > 0) {
                RevCommit parent = walk.parseCommit(current.getParent(0)); // first parent only
                ancestors.add(parent);
                current = parent;
            }
        }
        return ancestors;
    }

    /**
     * Build graph in memory (no DB writes inside the loop), then push final metrics in batched UPDATEs.
     */
    @Transactional
    public Graph buildGraph(String owner, String path, String repoPath) {
        vertices.clear();
        parents.clear();
        depthCacheMin.clear();
        depthCacheMax.clear();
        edgeCount = 0L;

        try (Git git = Git.open(new File(repoPath))) {

            // 1) Collect commits reachable from all branches (current impl uses checkout; can replace with RevWalk later)
            List<Ref> branches = git.branchList().setListMode(ListMode.ALL).call();
            Map<RevCommit, List<Ref>> commitMeta = new HashMap<>();

            for (Ref branch : branches) {
                String name = branch.getName();
                try {
                    if ("HEAD".equals(name) || name.contains("refs/heads/master")) continue;
                    git.reset().setMode(ResetType.HARD).call();
                    git.clean().setCleanDirectories(true).setForce(true).call();
                    git.checkout().setName(name).call();

                    Iterable<RevCommit> commits = git.log().call();
                    for (RevCommit rc : commits) {
                        commitMeta.computeIfAbsent(rc, k -> new ArrayList<>()).add(branch);
                    }
                } catch (CheckoutConflictException e) {
                    LOGGER.warn("Checkout conflict for branch {}: {}", name, e.getMessage());
                }
            }

            // 2) Build the graph in chronological order (no DB interaction here)
            List<RevCommit> allCommits = new ArrayList<>(commitMeta.keySet());
            allCommits.sort(Comparator.comparingInt(RevCommit::getCommitTime));

            int total = allCommits.size();
            for (int idx = 0; idx < total; idx++) {
                RevCommit commit = allCommits.get(idx);
                if (idx % 5000 == 0) {
                    LOGGER.info("Processed {}/{} commits...", idx, total);
                }

                String sha = commit.getId().getName();
                addVertex(sha);
                Vertex v = vertices.get(sha);

                List<String> branchNames = Optional.ofNullable(commitMeta.get(commit))
                        .orElseGet(List::of)
                        .stream()
                        .map(Ref::getName)
                        .filter(n -> !n.contains("/remotes/origin/HEAD"))
                        .collect(Collectors.toList());

                // "Linear ancestors" as per your method; used for your current subgraph stats
                List<RevCommit> linearAncestors = getLinearAncestors(git.getRepository(), commit);
                int mergeCountAlongMainline = 0;
                for (RevCommit anc : linearAncestors) {
                    if (anc.getParentCount() > 1) mergeCountAlongMainline++;
                }

                // Connect edges parent -> sha
                int parentCount = commit.getParentCount();
                for (int i = 0; i < parentCount; i++) {
                    String parentSha = commit.getParent(i).getId().getName();
                    addEdge(parentSha, sha);
                }

                // Compute depths now that reverse map is updated
                int minDepth = getMinimumDepth(sha);
                int maxDepth = getMaximumDepth(sha);

                // Snapshot per-node stats into Vertex (avoid huge subGraphNodes lists if not needed)
                List<String> subGraphNodes = linearAncestors.stream().map(AnyObjectId::getName).toList();
                LOGGER.info("Adding snapshot props for commit {}: subGraphNodes={}, branches={}, mergeCount={}, minDepth={}, maxDepth={}",
                        sha, subGraphNodes.size(), branchNames.size(), mergeCountAlongMainline, minDepth, maxDepth);
                v.addSnapshotProps(
                        subGraphNodes,
                        branchNames,
                        subGraphNodes.size(),                // beware: nodes != edges; this mirrors your current field semantics
                        mergeCountAlongMainline,
                        branchNames.size(),
                        subGraphNodes.size(),
                        0.0,                                  // placeholder; we set global avg later
                        maxDepth,
                        minDepth,
                        commit.getParentCount() > 1
                );
                v.setTimestamp(Date.from(Instant.ofEpochSecond(commit.getCommitTime())));
            }

        } catch (Exception e) {
            LOGGER.error("Graph build failed: {}", e.getMessage(), e);
        }

        // 3) Compute global average degree once
        final double avgDegGlobal = getAverageDegree();

        // 4) Post-pass: push final metrics to DB in batches (no entities in memory)
        final int BATCH = 1000;
        int n = 0;
        for (Map.Entry<String, Vertex> e : vertices.entrySet()) {
            String sha = e.getKey();
            Vertex v = e.getValue();
            try {
                commitRepository.updateGraphMetrics(
                        sha,
                        v.getInDegree(),
                        v.getOutDegree(),
                        v.getMergeCount(),
                        v.getMinDepthOfCommitHistory(),
                        v.getMaxDepthOfCommitHistory(),
                        v.getNumberOfBranches(),
                        avgDegGlobal
                );
            } catch (Exception ex) {
                // If a sha is missing in DB, log at debug and continue
                LOGGER.debug("updateGraphMetrics failed for {}: {}", sha, ex.getMessage());
            }
        }
        return this;
    }
    /** Min depth to a root (memoized) */
    public int getMinimumDepth(String sha) {
        Integer cached = depthCacheMin.get(sha);
        if (cached != null) return cached;

        Set<String> ps = findParents(sha);
        if (ps.isEmpty()) {
            depthCacheMin.put(sha, 0);
            return 0;
        }
        int min = Integer.MAX_VALUE;
        for (String p : ps) {
            min = Math.min(min, getMinimumDepth(p) + 1);
        }
        depthCacheMin.put(sha, min);
        return min;
    }

}
