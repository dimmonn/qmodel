package com.research.qmodel.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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
                LOGGER.warn("Project " + owner + "/" + projectName + " exists.");
                return;
            }
            Git.cloneRepository()
                    .setURI(cloneUrl)
                    .setDirectory(cloneDirectory)
                    .setTimeout(REQUEST_TIMEOUT)
                    .setProgressMonitor(new SimpleProgressMonitor())
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

        int edgeId = 1;  // Unique ID for each edge
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
            node.put("maxDegree", vertex.getMaxDegree());
            node.put("averageDegree", vertex.getAverageDegree());
            node.put("depthOfCommitHistory", vertex.getDepthOfCommitHistory());


            nodes.add(node);

            // Create edges
            for (String neighbor : vertex.neighbors) {
                Map<String, Object> edge = new HashMap<>();
                edge.put("id", String.valueOf(edgeId++));
                edge.put("source", vertex.sha);
                edge.put("target", neighbor);
                edge.put("mainStat", "53/s");  // Replace with actual logic
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
}
