package com.research.qmodel.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.repos.ActionsRepository;
import com.research.qmodel.repos.CommitRepository;
import java.io.IOException;
import java.util.Map.Entry;
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

import java.io.File;
import java.time.Instant;
import java.util.*;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

@Component
@Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
public class Graph extends GitMaintainable {
  Map<String, Integer> depthCacheMax = new HashMap<>();
  Map<String, Integer> depthCacheMin = new HashMap<>();
  @Autowired private CommitRepository commitRepository;

  private final Map<String, Vertex> vertices;
  private final Logger LOGGER = LoggerFactory.getLogger(Graph.class);
  @Autowired private ActionsRepository actionsRepository;
  @Autowired private ObjectMapper objectMapper;

  public Graph() {
    this.vertices = new LinkedHashMap<>();
  }

  public void addVertex(String sha) {
    vertices.putIfAbsent(sha, new Vertex(sha));
  }

  public void addEdge(String parentSha, String childSha) {
    Vertex parent = vertices.get(parentSha);
    Vertex child = vertices.get(childSha);

    if (parent != null && child != null) {
      parent.addNeighbor(childSha);
      parent.incrementOutDegree();
      child.incrementInDegree();
    }
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

  public int getNumberOfVertices() {
    return vertices.size();
  }

  public int getNumberOfEdges() {
    int edgeCount = 0;
    // TODO It's a defect, most likely we need particular neighbors
    for (Vertex vertex : vertices.values()) {
      edgeCount += vertex.neighbors.size();
    }
    return edgeCount;
  }

  public double getAverageDegree() {
    int edgeCount = getNumberOfEdges();
    int vertexCount = getNumberOfVertices();
    return vertexCount == 0 ? 0 : (double) edgeCount / vertexCount;
  }

  public Graph buildGraph(String owner, String path, String repoPath) {
    List<Commit> allGraphCommits = new ArrayList<>();
    try (Git git = Git.open(new File(repoPath))) {

      List<Ref> branches = git.branchList().setListMode(ListMode.ALL).call();
      RevWalk revWalk = new RevWalk(git.getRepository());
      Map<RevCommit, List<Ref>> commitMeta = new HashMap<>();
      for (Ref branch : branches) {
        try {
          if (branch.getName().equals("HEAD") || branch.getName().contains("refs/heads/master")) {
            continue;
          }
          git.reset().setMode(ResetType.HARD).call();
          git.clean().setCleanDirectories(true).setForce(true).call();
          git.checkout().setName(branch.getName()).call();
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
      int counter = 0;
      Queue<RevCommit> revCommits = new LinkedList<>(allCommits);
      while (!revCommits.isEmpty() ) {
        RevCommit commit = revCommits.poll();
        LOGGER.info("{} are left to process.", allCommits.size() - (++counter));
        String sha = commit.getId().getName();
        Commit foundCommit = commitRepository.findById(new CommitID(sha)).orElse(null);
        if (foundCommit != null
            && foundCommit.getMinDepthOfCommitHistory() != null
            && foundCommit.getMinDepthOfCommitHistory() > 0) {
          LOGGER.info(
              "Commit {} is already processed, min depth is {}",
              sha,
              foundCommit.getMinDepthOfCommitHistory());
          continue;
        }

        boolean isMerge = commit.getParentCount() > 1;
        if (!vertices.containsKey(sha)) {
          addVertex(sha);
        }
        List<Ref> refs = commitMeta.get(commit);
        List<String> branchNames =
            refs.stream()
                .map(Ref::getName)
                .filter(name -> !name.contains("/remotes/origin/HEAD"))
                .toList();
        int mergeCount = 0;

        List<RevCommit> linearAncestors = getLinearAncestors(git.getRepository(), commit);
        List<String> subGraphNodes = linearAncestors.stream().map(AnyObjectId::getName).toList();
        for (RevCommit linearAncestor : linearAncestors) {
          if (linearAncestor.getParentCount() > 1) {
            mergeCount++;
          }
        }

        Vertex vertex = getVertex(sha);

        int parentCount = commit.getParentCount();
        for (int i = 0; i < parentCount; i++) {
          String parentSha = commit.getParent(i).getId().getName();
          addVertex(parentSha);
          if (!getVertex(parentSha).neighbors.contains(sha)) {
            addEdge(parentSha, sha);
          }
        }
        int numberOfVertices = linearAncestors.size();

        int numberOfEdges = subGraphNodes.size();
        int inDegree = vertex.getInDegree();
        int outDegree = vertex.getOutDegree();
        double averageDegree = getAverageDegree();
        int minDepthOfCommitHistory = getMinimumDepth(sha);
        int maxDepthOfCommitHistory = getMaximumDepth(sha);
        if (foundCommit != null) {
          try {
            LOGGER.info(
                "Saving commit graph properties commit id# {}, mergeCount# {}, numberOfVertices# {}, numberOfEdges# {},inDegree# {},outDegree# {},averageDegree# {}, minDepthOfCommitHistory# {}, maxDepthOfCommitHistory# {}, isMerge# {}",
                foundCommit.getSha(),
                mergeCount,
                numberOfVertices,
                numberOfEdges,
                inDegree,
                outDegree,
                averageDegree,
                minDepthOfCommitHistory,
                maxDepthOfCommitHistory,
                isMerge);

            foundCommit.setMergeCount(mergeCount);
            foundCommit.setNumberOfVertices(subGraphNodes.size());
            foundCommit.setNumberOfEdges(numberOfEdges);
            foundCommit.setInDegree(inDegree);
            foundCommit.setOutDegree(outDegree);
            foundCommit.setNumberOfBranches(branchNames.size());
            foundCommit.setAverageDegree(averageDegree);
            foundCommit.setMinDepthOfCommitHistory(minDepthOfCommitHistory);
            foundCommit.setMaxDepthOfCommitHistory(maxDepthOfCommitHistory);
            foundCommit.setIsMerge(isMerge);
            foundCommit.setBranches(new HashSet<>(branchNames));
            // foundCommit.setSubGraphNodes(new HashSet<>(subGraphNodes));
            allGraphCommits.add(foundCommit);
          } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
          }
        }

        vertex.addSnapshotProps(
            subGraphNodes,
            branchNames,
            subGraphNodes.size(),
            mergeCount,
            branchNames.size(),
            numberOfEdges,
            averageDegree,
            maxDepthOfCommitHistory,
            minDepthOfCommitHistory,
            isMerge);
        int commitTime = commit.getCommitTime();

        Instant instant = Instant.ofEpochSecond(commitTime);
        vertex.setTimestamp(Date.from(instant));
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
    for (Commit commit : allGraphCommits) {
      if (commit.getOutDegree().equals(0)) {
        Vertex vertex = vertices.get(commit.getSha());
        if (vertex != null) {
          commit.setOutDegree(vertex.getOutDegree());
          commit.setInDegree(vertex.getInDegree());
        }
      }
    }
    commitRepository.saveAll(allGraphCommits);
    return this;
  }

  public int getMinimumDepth(String sha) {

    if (depthCacheMin.containsKey(sha)) {
      return depthCacheMin.get(sha);
    }
    Set<String> parents = findParents(sha);
    if (parents.isEmpty()) {
      depthCacheMin.put(sha, 0);
      return 0;
    }
    int minDepth = Integer.MAX_VALUE;
    for (String parentSha : parents) {
      int parentDepth = getMinimumDepth(parentSha);
      minDepth = Math.min(minDepth, parentDepth + 1);
    }
    depthCacheMin.put(sha, minDepth);
    return minDepth;
  }

  public int getMaximumDepth(String sha) {
    if (depthCacheMax.containsKey(sha)) {
      return depthCacheMax.get(sha);
    }

    Set<String> parents = findParents(sha);
    if (parents.isEmpty()) {
      depthCacheMax.put(sha, 0);
      return 0;
    }
    int maxDepth = Integer.MIN_VALUE;
    for (String parentSha : parents) {
      int parentDepth = getMaximumDepth(parentSha);
      maxDepth = Math.max(maxDepth, parentDepth + 1);
    }

    depthCacheMax.put(sha, maxDepth);
    return maxDepth;
  }

  public List<RevCommit> getLinearAncestors(Repository repo, RevCommit startCommit)
      throws IOException {
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
}
