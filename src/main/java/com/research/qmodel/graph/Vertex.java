package com.research.qmodel.graph;

import java.util.HashSet;
import java.util.Set;

public class Vertex {
    String sha;
    Set<String> neighbors; // Neighbors are child commits in this context
    Set<String> branches;
    private int arc__failed;
    private int arc__passed;

    public Vertex(String sha) {
        this.sha = sha;
        this.neighbors = new HashSet<>();
        this.branches = new HashSet<>();
    }

    public void addNeighbor(String neighborSha) {
        neighbors.add(neighborSha);
    }

    public void addBranch(String branch) {
        branches.add(branch);
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
}
