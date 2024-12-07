package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.CommitDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "commit")
@JsonDeserialize(using = CommitDeserializer.class)
@Data
@NoArgsConstructor
@IdClass(CommitID.class)
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public class Commit {
  @Id
  @Column(name = "sha")
  @ToString.Include
  private String sha;

  @ToString.Exclude
  @Column(name = "raw_data", columnDefinition = "LONGTEXT")
  private String rawData;

  @EqualsAndHashCode.Exclude
  @Temporal(TemporalType.TIMESTAMP)
  @ToString.Include
  private Date commitDate;

  @EqualsAndHashCode.Exclude
  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private AGraph aGraph;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<FileChange> fileChanges;

  @ToString.Include @Column @EqualsAndHashCode.Exclude private int numOfFilesChanged;
  @ToString.Include @Column @EqualsAndHashCode.Exclude private String author;
  @ToString.Include @Column @EqualsAndHashCode.Exclude private String email;

  @Column(columnDefinition = "LONGTEXT")
  @EqualsAndHashCode.Exclude
  private String message;

  @ToString.Include @Column @EqualsAndHashCode.Exclude private int commentCount;

  @Column @EqualsAndHashCode.Exclude int numberOfVertices = 0;

  @Column @EqualsAndHashCode.Exclude int numberOfBranches = 0;

  @Column @EqualsAndHashCode.Exclude int numberOfEdges = 0;

  @Column @EqualsAndHashCode.Exclude int maxDegree = 0;

  @Column @EqualsAndHashCode.Exclude double averageDegree = 0;

  @Column @EqualsAndHashCode.Exclude int depthOfCommitHistory = 0;

  @Column @EqualsAndHashCode.Exclude int mergeCount = 0;
  @Column @EqualsAndHashCode.Exclude int branchLength = 0;

  //    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  //    @JoinTable(
  //            name = "commit_parents",
  //            joinColumns = {
  //                    @JoinColumn(name = "child_sha", referencedColumnName = "sha")
  //            },
  //            inverseJoinColumns = {
  //                    @JoinColumn(name = "parent_sha", referencedColumnName = "sha")
  //            }
  //    )
  //    @EqualsAndHashCode.Exclude
  //    private List<Commit> parents = new ArrayList<>();

  //    public void addParent(Commit parent) {
  //        if (!parents.contains(parent)) {
  //            parents.add(parent);
  //        }
  //    }
}
