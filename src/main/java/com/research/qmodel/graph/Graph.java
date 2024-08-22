package com.research.qmodel.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.model.Actions;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.repos.ActionsRepository;
import com.research.qmodel.repos.CommitRepository;
import java.util.Map.Entry;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
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
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

@Component
@Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
public class Graph extends GitMaintainable {

  @Autowired private CommitRepository commitRepository;

  private Map<String, Vertex> vertices;
  private final Logger LOGGER = LoggerFactory.getLogger(Graph.class);
  @Autowired private ActionsRepository actionsRepository;
  @Autowired private ObjectMapper objectMapper;

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
    for (Entry<String, Vertex> entry : vertices.entrySet()) {
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

  public int calculateBranchLength(String branchName) {
    int length = 0;
    for (Vertex vertex : vertices.values()) {
      if (vertex.branches.contains(branchName)) {
        length++;
      }
    }
    return length;
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

  public Graph buildGraph(String owner, String path, String repoPath) {
    try (Git git = Git.open(new File(repoPath))) {
      List<Ref> branches = git.branchList().setListMode(ListMode.REMOTE).call();

      Map<RevCommit, List<Ref>> commitMeta = new HashMap<>();
      for (Ref branch : branches) {
        try {
          git.reset().setMode(ResetType.HARD).call();
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
      allCommits.sort(Comparator.comparingInt(RevCommit::getCommitTime));
      int mergeCount = 0;
      int counter = 0;
      Queue<RevCommit> revCommits =
          new LinkedList<>(allCommits);
      while (!revCommits.isEmpty()) {
        RevCommit commit = revCommits.poll();
        LOGGER.info("{} are left to process.", allCommits.size() - (++counter));
        String sha = commit.getId().getName();
        Commit foundCommit = commitRepository.findById(new CommitID(sha)).orElse(null);
        if (foundCommit != null) {
          if (foundCommit.getMinDepthOfCommitHistory() != null) {
            LOGGER.info("{} has been processed already, continuing.", sha);
            continue;
          }
        }

        boolean isMerge = false;
        if (commit.getParentCount() > 1) {
          mergeCount++;
          if (foundCommit != null) {
            isMerge = true;
          }
        }

        addVertex(sha);
        List<Ref> refs = commitMeta.get(commit);
        Vertex vertex = getVertex(sha);
        for (Ref ref : refs) {
          addBranch(sha, ref.getName());
          if (foundCommit != null) {
            foundCommit.setBranchLength(vertex.branches.size());
          }
        }

        int parentCount = commit.getParentCount();
        for (int i = 0; i < parentCount; i++) {
          String parentSha = commit.getParent(i).getId().getName();
          addVertex(parentSha);
          addEdge(parentSha, sha);
        }

        int numberOfVertices = getNumberOfVertices();

        int numberOfEdges = getNumberOfEdges();
        int maxDegree = getMaxDegree();
        double averageDegree = getAverageDegree();
        int minDepthOfCommitHistory = getMinimumDepth(sha, new HashMap<>());
        int maxDepthOfCommitHistory = getMaximumDepth(sha, new HashMap<>());

        if (foundCommit != null) {
          try {
            LOGGER.info(
                "Saving commit graph properties commit id# {}, mergeCount# {}, numberOfVertices# {}, numberOfEdges# {},maxDegree# {},averageDegree# {}, minDepthOfCommitHistory# {}, maxDepthOfCommitHistory# {}, isMerge# {}",
                foundCommit.getSha(),
                mergeCount,
                numberOfVertices,
                numberOfEdges,
                maxDegree,
                averageDegree,
                minDepthOfCommitHistory,
                maxDepthOfCommitHistory,
                isMerge);
            foundCommit.setMergeCount(mergeCount);
            foundCommit.setNumberOfVertices(numberOfVertices);
            foundCommit.setNumberOfEdges(numberOfEdges);
            foundCommit.setMaxDegree(maxDegree);
            foundCommit.setNumberOfBranches(vertex.branches.size());
            foundCommit.setAverageDegree(averageDegree);
            foundCommit.setMinDepthOfCommitHistory(minDepthOfCommitHistory);
            foundCommit.setMaxDepthOfCommitHistory(maxDepthOfCommitHistory);
            foundCommit.setIsMerge(isMerge);
            commitRepository.save(foundCommit);
          } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
          }
        }

        if (vertex != null) {
          vertex.addSnapshotProps(
              numberOfVertices,
              vertex.getNumberOfBranches(),
              numberOfEdges,
              maxDegree,
              averageDegree,
              maxDepthOfCommitHistory,
              minDepthOfCommitHistory,
              isMerge);
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

  public int getMinimumDepth(String sha, Map<String, Integer> depthCache) {
    if (depthCache.containsKey(sha)) {
      return depthCache.get(sha);
    }
    Set<String> parents = findParents(sha);
    if (parents.isEmpty()) {
      depthCache.put(sha, 0);
      return 0;
    }
    int minDepth = Integer.MAX_VALUE;
    for (String parentSha : parents) {
      int parentDepth = getMinimumDepth(parentSha, depthCache);
      minDepth = Math.min(minDepth, parentDepth);
    }
    depthCache.put(sha, minDepth + 1);
    return minDepth + 1;
  }

  public int getMaximumDepth(String sha, Map<String, Integer> depthCache) {
    if (depthCache.containsKey(sha)) {
      return depthCache.get(sha);
    }

    Set<String> parents = findParents(sha);
    if (parents.isEmpty()) {
      depthCache.put(sha, 0);
      return 0;
    }
    int maxDepth = Integer.MIN_VALUE;
    for (String parentSha : parents) {
      int parentDepth = getMaximumDepth(parentSha, depthCache);
      maxDepth = Math.max(maxDepth, parentDepth);
    }

    depthCache.put(sha, maxDepth + 1);
    return maxDepth + 1;
  }

  private void updateActionResullt(Optional<Actions> foundAction, String sha)
      throws JsonProcessingException {
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
