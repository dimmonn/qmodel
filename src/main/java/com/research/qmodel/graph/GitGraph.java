package com.research.qmodel.graph;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GitGraph {

    private final Logger LOGGER = LoggerFactory.getLogger(GitGraph.class);

    public static void main(String[] args) throws GitAPIException, IOException {
        Graph gitGraph = new Graph();
        String path = "/Users/dpolishchuk/dimmonn_test";
        gitGraph.cloneRepo("dimmonn", "test", path);

        Graph graph = gitGraph.buildGraph(null, null, path);
//        graph.exportGraph("graph_data.json");
//
//        System.out.println("Number of vertices (commits): " + graph.getNumberOfVertices());
//        System.out.println("Number of edges (parent-child relationships): " + graph.getNumberOfEdges());
//        System.out.println("Number of branches: " + graph.getNumberOfBranches());
//        System.out.println("Average degree: " + graph.getAverageDegree());
//        System.out.println("Maximum degree: " + graph.getMaxDegree());
//        System.out.println("Depth of commit history: " + graph.getDepthOfCommitHistory());
    }
}