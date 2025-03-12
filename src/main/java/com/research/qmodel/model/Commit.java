package com.research.qmodel.model;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.CommitDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;
import javax.persistence.IdClass;
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
  @ToString.Include
  private String sha;

  @ToString.Exclude
  @Column(columnDefinition = "LONGTEXT")
  private String rawData;

  @EqualsAndHashCode.Exclude
  @Temporal(TemporalType.TIMESTAMP)
  @ToString.Include
  private Date commitDate;

  @EqualsAndHashCode.Exclude
  @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  private AGraph aGraph;

  @ManyToMany(
      cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  private List<FileChange> fileChanges;

  @ToString.Include @Column @EqualsAndHashCode.Exclude private int numOfFilesChanged;
  @ToString.Include @Column @EqualsAndHashCode.Exclude private String author;
  @ToString.Include @Column @EqualsAndHashCode.Exclude private String email;

  @Column(columnDefinition = "LONGTEXT")
  @EqualsAndHashCode.Exclude
  private String message;

  @ToString.Include @Column @EqualsAndHashCode.Exclude private int commentCount;

  @Column @EqualsAndHashCode.Exclude Integer numberOfVertices;
  @Column @EqualsAndHashCode.Exclude Integer numberOfBranches;
  @Column @EqualsAndHashCode.Exclude Integer numberOfEdges;
  @Column @EqualsAndHashCode.Exclude Integer inDegree;
  @Column @EqualsAndHashCode.Exclude Integer outDegree;
  @Column @EqualsAndHashCode.Exclude Double averageDegree;
  @Column @EqualsAndHashCode.Exclude Integer minDepthOfCommitHistory;
  @Column @EqualsAndHashCode.Exclude Integer maxDepthOfCommitHistory;
  @Column @EqualsAndHashCode.Exclude Integer mergeCount;
  @Column @EqualsAndHashCode.Exclude Integer branchLength;
  @Column @EqualsAndHashCode.Exclude Boolean isMerge;
  @Column @EqualsAndHashCode.Exclude String projectName;
  @Column
  @EqualsAndHashCode.Exclude String projectOwner;
}
