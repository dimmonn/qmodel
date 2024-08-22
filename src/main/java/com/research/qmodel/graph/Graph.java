package com.research.qmodel.graph;

import aj.org.objectweb.asm.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.research.qmodel.model.Actions;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.model.ProjectID;
import com.research.qmodel.repos.ActionsRepository;
import com.research.qmodel.repos.CommitRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

@Component
@Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
public class Graph extends GitMaintainable {

    @Autowired
    private CommitRepository commitRepository;

    private Map<String, Vertex> vertices;
    private final Logger LOGGER = LoggerFactory.getLogger(Graph.class);
    @Autowired
    private ActionsRepository actionsRepository;
    @Autowired
    private ObjectMapper objectMapper;

    public Graph() {
        this.vertices = new HashMap<>();
    }

    public void addVertex(String sha) {
        vertices.putIfAbsent(sha, new Vertex(sha));
    }

    public void addEdge(String parentSha, String childSha) {
        vertices.get(parentSha).addNeighbor(childSha);
    }

    public void addBranch(String sha, String branch) {
        vertices.get(sha).addBranch(branch);
    }

    public Set<String> getVertices() {
        return vertices.keySet();
    }

    public Map<String, Vertex> getVerticesMap() {
        return vertices;
    }

    public Vertex getVertex(String sha) {
        return vertices.get(sha);
    }

    // Method to find all parents of a commit
    public Set<String> findParents(String sha) {
        Set<String> parents = new HashSet<>();
        for (Map.Entry<String, Vertex> entry : vertices.entrySet()) {
            if (entry.getValue().neighbors.contains(sha)) {
                parents.add(entry.getKey());
            }
        }
        return parents;
    }

    // Method to find all children of a commit
    public Set<String> findChildren(String sha) {
        return vertices.get(sha).neighbors;
    }

    // Method to find common ancestors of a set of commits
    public Set<String> findCommonAncestors(Set<String> shas) {
        Set<String> commonAncestors = new HashSet<>();
        if (shas.isEmpty()) {
            return commonAncestors;
        }

        Iterator<String> it = shas.iterator();
        Set<String> firstCommitAncestors = findAllAncestors(it.next());
        commonAncestors.addAll(firstCommitAncestors);

        while (it.hasNext()) {
            Set<String> nextCommitAncestors = findAllAncestors(it.next());
            commonAncestors.retainAll(nextCommitAncestors);
        }

        return commonAncestors;
    }

    // Helper method to find all ancestors of a commit
    private Set<String> findAllAncestors(String sha) {
        Set<String> ancestors = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(sha);

        while (!stack.isEmpty()) {
            String current = stack.pop();
            Set<String> parents = findParents(current);
            for (String parent : parents) {
                if (ancestors.add(parent)) {
                    stack.push(parent);
                }
            }
        }

        return ancestors;
    }

    // Methods to compute complexity metrics

    public int getNumberOfVertices() {
        return vertices.size();
    }

    public int getNumberOfEdges() {
        int edgeCount = 0;
        for (Vertex vertex : vertices.values()) {
            edgeCount += vertex.neighbors.size();
        }
        return edgeCount;
    }

    public int getNumberOfBranches() {
        Set<String> branches = new HashSet<>();
        for (Vertex vertex : vertices.values()) {
            branches.addAll(vertex.branches);
        }
        return branches.size();
    }

    public double getAverageDegree() {
        int edgeCount = getNumberOfEdges();
        int vertexCount = getNumberOfVertices();
        return vertexCount == 0 ? 0 : (double) edgeCount / vertexCount;
    }

    public int getMaxDegree() {
        int maxDegree = 0;
        for (Vertex vertex : vertices.values()) {
            maxDegree = Math.max(maxDegree, vertex.neighbors.size());
        }
        return maxDegree;
    }

    private int getDepthFromInitialCommit(String sha, Map<String, Integer> depthCache) {
        if (depthCache.containsKey(sha)) {
            return depthCache.get(sha);
        }

        Set<String> visited = new HashSet<>();
        int depth = getDepthFromInitialCommitHelper(sha, visited, depthCache);
        depthCache.put(sha, depth);
        return depth;
    }

    private int getDepthFromInitialCommitHelper(String sha, Set<String> visited, Map<String, Integer> depthCache) {
        Deque<String> stack = new ArrayDeque<>();
        Map<String, Integer> depths = new HashMap<>();

        stack.push(sha);
        depths.put(sha, 1);

        int maxDepth = 0;

        while (!stack.isEmpty()) {
            String current = stack.pop();
            int currentDepth = depths.get(current);
            maxDepth = Math.max(maxDepth, currentDepth);

            Vertex vertex = getVertex(current);
            if (vertex != null) {
                for (String neighbor : vertex.neighbors) {
                    if (visited.add(neighbor)) {
                        stack.push(neighbor);
                        depths.put(neighbor, currentDepth + 1);
                    }
                }
            }
        }

        depthCache.put(sha, maxDepth);
        return maxDepth;
    }

    public Graph buildGraph(String owner, String path, String repoPath) throws IOException, GitAPIException {
        try (Git git = Git.open(new File(repoPath))) {
            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();

            Map<RevCommit, List<Ref>> commitMeta = new HashMap<>();
            for (Ref branch : branches) {
                try {
                    git.reset().setMode(ResetCommand.ResetType.HARD).call();
                    git.checkout().setName(branch.getName()).call();
                    RevWalk revWalk = new RevWalk(git.getRepository());
                    Iterable<RevCommit> commits = git.log().call();
                    for (RevCommit commit : commits) {
                        commitMeta.computeIfAbsent(commit, k -> new ArrayList<>()).add(branch);
                    }
                    revWalk.close();
                } catch (CheckoutConflictException e) {
                    LOGGER.warn("Checkout conflict for branch {}: {}", branch.getName(), e.getMessage());
                }
            }

            List<RevCommit> allCommits = new ArrayList<>(commitMeta.keySet());
            Collections.sort(allCommits, Comparator.comparingInt(RevCommit::getCommitTime));

            Map<String, Integer> depthCache = new HashMap<>();

            for (RevCommit commit : allCommits) {
                String sha = commit.getId().getName();
                addVertex(sha);
                List<Ref> refs = commitMeta.get(commit);
                for (Ref ref : refs) {
                    addBranch(sha, ref.getName());
                }

                int parentCount = commit.getParentCount();
                for (int i = 0; i < parentCount; i++) {
                    String parentSha = commit.getParent(i).getId().getName();
                    addVertex(parentSha);
                    addEdge(parentSha, sha); // parent -> child
                }

                int numberOfVertices = getNumberOfVertices();
                int numberOfBranches = getNumberOfBranches();
                int numberOfEdges = getNumberOfEdges();
                int maxDegree = getMaxDegree();
                double averageDegree = getAverageDegree();
                int depthOfCommitHistory = getDepthFromInitialCommit(sha, depthCache);

                Commit foundCommit = commitRepository.findById(new CommitID(sha)).orElse(null);
                if (foundCommit != null) {
                    try {
                        foundCommit.setNumberOfVertices(numberOfVertices);
                        foundCommit.setNumberOfBranches(numberOfBranches);
                        foundCommit.setNumberOfEdges(numberOfEdges);
                        foundCommit.setMaxDegree(maxDegree);
                        foundCommit.setAverageDegree(averageDegree);
                        foundCommit.setDepthOfCommitHistory(depthOfCommitHistory);
                        commitRepository.save(foundCommit);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }

                Vertex vertex = getVertex(sha);
                if (vertex != null) {
                    vertex.addSnapshotProps(numberOfVertices,
                            numberOfBranches,
                            numberOfEdges,
                            maxDegree,
                            averageDegree,
                            depthOfCommitHistory);
                    int commitTime = commit.getCommitTime();

                    Instant instant = Instant.ofEpochSecond(commitTime);
                    vertex.setTimestamp(Date.from(instant));
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return this;
    }

    private void updateActionResullt(Optional<Actions> foundAction, String sha) throws JsonProcessingException {
        if (foundAction.isPresent()) {
            Actions actions = foundAction.get();
            String allActions = actions.getAllActions();
            JsonNode rowActions = objectMapper.readTree(allActions);
            for (JsonNode rowAction : rowActions) {
                String actionSha = rowAction.path("head_sha").asText();
                if (actionSha.equals(sha)) {
                    String status = rowAction.path("status").asText();
                    String conclusion = rowAction.path("conclusion").asText();
                    Vertex vertex = getVertex(sha);
//                    if (vertex == null) {
//                        vertex = new Vertex(sha);
//                        addVertex(sha);
//                    }
                    if ("completed".equals(status)) {
                        if ("success".equals(conclusion)) {
                            vertex.incrementPassed();
                        } else {
                            vertex.incrementFailed();
                        }
                    }
                }
            }
        }
    }

}
