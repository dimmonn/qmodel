package com.research.qmodel.graph;

import java.util.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vertex {
    String sha;
    Set<String> neighbors;
    List<String> branches;
    List<String> subGraphNodes;
    private int arc__failed;
    private int arc__passed;
    private Date timestamp;
    private int numberOfVertices;
    private int numberOfBranches;
    private int numberOfEdges;
    private double averageDegree;
    private int maxDepthOfCommitHistory;
    private int minDepthOfCommitHistory;
    private boolean isMerge;
    private int inDegree;
    private int outDegree;
    private int mergeCount;

    public Vertex() {
    }

    public void incrementInDegree() {
        this.inDegree++;
    }

    public void incrementOutDegree() {
        this.outDegree++;
    }

    public Vertex(String sha) {
        this.sha = sha;
        this.neighbors = new HashSet<>();
    }

    public void addNeighbor(String neighborSha) {
        neighbors.add(neighborSha);
    }

    public void incrementFailed() {
        this.arc__failed++;
    }

    public void incrementPassed() {
        this.arc__passed++;
    }

    public int getArcFailed() {
        return arc__failed;
    }

    public int getArcPassed() {
        return arc__passed;
    }

    public void addSnapshotProps(
            List<String> subGraphNodes,
            List<String> branches,
            int numberOfVertices,
            int mergeCount,
            int numberOfBranches,
            int numberOfEdges,
            double averageDegree,
            int maxDepthOfCommitHistory,
            int minDepthOfCommitHistory,
            boolean isMerge) {
        this.numberOfVertices = numberOfVertices;
        this.numberOfBranches = numberOfBranches;
        this.numberOfEdges = numberOfEdges;
        this.averageDegree = averageDegree;
        this.maxDepthOfCommitHistory = maxDepthOfCommitHistory;
        this.minDepthOfCommitHistory = minDepthOfCommitHistory;
        this.isMerge = isMerge;
        this.branches = branches;
        this.mergeCount = mergeCount;
        this.subGraphNodes = subGraphNodes;
    }

    public int getNumberOfBranches() {
        if (branches != null) {
            return branches.size();
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vertex vertex)) return false;
        return maxDepthOfCommitHistory == vertex.maxDepthOfCommitHistory && minDepthOfCommitHistory == vertex.minDepthOfCommitHistory && isMerge == vertex.isMerge && inDegree == vertex.inDegree && outDegree == vertex.outDegree && mergeCount == vertex.mergeCount && Objects.equals(sha, vertex.sha) && Objects.equals(neighbors, vertex.neighbors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sha, neighbors, maxDepthOfCommitHistory, minDepthOfCommitHistory, isMerge, inDegree, outDegree, mergeCount);
    }
}
