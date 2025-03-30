package com.research.qmodel.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GitMaintainable {
  Logger LOGGER = LoggerFactory.getLogger(GitMaintainable.class);

  @Value("${github.clone.timeout:3000}")
  private int REQUEST_TIMEOUT;

  public void cloneRepo(String owner, String projectName, String cloneDirectoryPath) {
    try {
      String cloneUrl = "https://github.com/" + owner + "/" + projectName;
      File cloneDirectory = new File(cloneDirectoryPath);

      if (cloneDirectory.exists()) {
        LOGGER.warn("Project " + owner + "/" + projectName + " xexists.");
        return;
      }
      Git.cloneRepository()
          .setURI(cloneUrl)
          .setDirectory(cloneDirectory)
          .setTimeout(REQUEST_TIMEOUT)
          /*.setProgressMonitor(new SimpleProgressMonitor())*/
          .call();
      LOGGER.info("Repository " + owner + "/" + projectName + " cloned successfully.");
    } catch (GitAPIException e) {
      e.printStackTrace();
    }
  }

  public void exportGraph(String filePath, Map<String, Vertex> vertices) {
    // Create nodes and edges lists
    List<Map<String, Object>> nodes = new ArrayList<>();
    List<Map<String, Object>> edges = new ArrayList<>();

    int edgeId = 1; // Unique ID for each edge
    for (Map.Entry<String, Vertex> entry : vertices.entrySet()) {
      Vertex vertex = entry.getValue();

      // Create node
      Map<String, Object> node = new HashMap<>();
      node.put("id", vertex.sha);
      node.put("title", "Commit " + vertex.sha);
      node.put("subTitle", "Branches: " + String.join(", ", vertex.branches));
      node.put("arc__failed", vertex.getArcFailed());
      node.put("arc__passed", vertex.getArcPassed());
      node.put("detail__zone", "Zone " + vertex.sha);
      if (vertex.getTimestamp() != null) {
        node.put("timestamp", vertex.getTimestamp().toString());
      }
      node.put("numberOfVertices", vertex.getNumberOfVertices());
      node.put("numberOfBranches", vertex.getNumberOfBranches());
      node.put("numberOfEdges", vertex.getNumberOfEdges());
      node.put("inDegree", vertex.getInDegree());
      node.put("outDegree", vertex.getOutDegree());
      node.put("averageDegree", vertex.getAverageDegree());
      node.put("maxDepthOfCommitHistory", vertex.getMaxDepthOfCommitHistory());
      node.put("minDepthOfCommitHistory", vertex.getMinDepthOfCommitHistory());
      node.put("isMerge", vertex.isMerge());

      nodes.add(node);

      for (String neighbor : vertex.neighbors) {
        Map<String, Object> edge = new HashMap<>();
        edge.put("id", String.valueOf(edgeId++));
        edge.put("source", vertex.sha);
        edge.put("target", neighbor);
        edge.put("mainStat", "53/s"); // Replace with actual logic
        edges.add(edge);
      }
    }

    // Create the final graph structure
    Map<String, Object> graph = new HashMap<>();
    graph.put("nodes", nodes);
    graph.put("edges", edges);

    // Serialize to JSON and write to file
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    try (FileWriter writer = new FileWriter(filePath)) {
      gson.toJson(graph, writer);
    } catch (IOException e) {
      LOGGER.error(e.getMessage());
    }
  }

  public Set<String> getAllCommits(String repoPath) throws Exception {
    Set<String> commitHashes = new HashSet<>();

    // Open the Git repository
    try (Git git = Git.open(new File(repoPath))) {
      Repository repository = git.getRepository();

      // Get all references (branches, tags, remotes, etc.)
      List<Ref> allRefs = git.getRepository().getRefDatabase().getRefs();

      // Use RevWalk to traverse the commit history
      try (RevWalk revWalk = new RevWalk(repository)) {
        for (Ref ref : allRefs) {
          ObjectId refObjectId = ref.getObjectId();
          if (refObjectId != null) {
            revWalk.markStart(revWalk.parseCommit(refObjectId));
          }
        }

        // Traverse all commits
        for (RevCommit commit : revWalk) {
          commitHashes.add(commit.getName());
        }
      }
    }

    return commitHashes;
  }

  public static Set<String> getReferencedCommits(String repoPath) throws Exception {
    Set<String> referencedHashes = new HashSet<>();

    try (Git git = Git.open(new File(repoPath))) {
      Collection<Ref> refs = git.getRepository().getRefDatabase().getRefs();
      try (RevWalk revWalk = new RevWalk(git.getRepository())) {
        for (Ref ref : refs) {
          ObjectId refObjectId = ref.getObjectId();
          if (refObjectId != null) {
            RevCommit startCommit = revWalk.parseCommit(refObjectId);
            revWalk.markStart(startCommit);
          }
        }
        for (RevCommit commit : revWalk) {
          referencedHashes.add(commit.getName());
        }
      }
    }

    return referencedHashes;
  }

  public Set<String> getForkedCommits(String repoPath) throws Exception {
    Set<String> allCommits = getAllCommits(repoPath);
    Set<String> referencedCommits = getReferencedCommits(repoPath);
    allCommits.removeAll(referencedCommits);
    return allCommits;
  }
}
