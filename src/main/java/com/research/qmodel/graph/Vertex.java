package com.research.qmodel.graph;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
@Getter
public class Vertex {
    String sha;
    Set<String> neighbors;
    Set<String> branches;
    private int arc__failed;
    private int arc__passed;
    private Date timestamp;
    private int numberOfVertices;
    private int numberOfBranches;
    private int numberOfEdges;
    private int maxDegree;
    private double averageDegree;
    private int depthOfCommitHistory;

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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void addSnapshotProps(int numberOfVertices, int numberOfBranches, int numberOfEdges, int maxDegree, double averageDegree, int depthOfCommitHistory) {
        this.numberOfVertices = numberOfVertices;
        this.numberOfBranches = numberOfBranches;
        this.numberOfEdges = numberOfEdges;
        this.maxDegree = maxDegree;
        this.averageDegree = averageDegree;
        this.depthOfCommitHistory = depthOfCommitHistory;


    }


}
