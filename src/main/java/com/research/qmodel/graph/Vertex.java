package com.research.qmodel.graph;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
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
  private double averageDegree;
  private int maxDepthOfCommitHistory;
  private int minDepthOfCommitHistory;
  private boolean isMerge;
  private int inDegree;
  private int outDegree;

  public void incrementInDegree() {
    this.inDegree++;
  }

  public void incrementOutDegree() {
    this.outDegree++;
  }


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

  public void addSnapshotProps(
      int numberOfVertices,
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
  }

  public int getNumberOfBranches() {
    return branches.size();
  }
}
