package com.research.qmodel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.ProjectPullDeserializer;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

import java.util.*;

@Data
@Entity
@Table(name = "project_pull")
@IdClass(PullID.class)
@Transactional
@JsonDeserialize(using = ProjectPullDeserializer.class)
public class ProjectPull implements BaseMetric {
  @Id
  @Column(length = 100)
  private Long id;

  @Id
  @Column(length = 100)
  private String projectOwner;

  @Id
  @Column(length = 100)
  private String projectName;

  @Column(columnDefinition = "LONGTEXT")
  private String rawPull;

  @JsonIgnore
  @ToString.Exclude
  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Timeline> timeLine;

  /*Not used yet*/
  @JsonIgnore
  @ToString.Exclude
  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<ProjectIssue> projectIssues;

  @JsonIgnore
  @ToString.Exclude
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Commit> commits;

  @Column(columnDefinition = "LONGTEXT")
  private String title;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Project project;

  @Temporal(TemporalType.TIMESTAMP)
  private Date created_at;

  @Temporal(TemporalType.TIMESTAMP)
  private Date closed_at;

  @Temporal(TemporalType.TIMESTAMP)
  private Date merged_at;

  @Temporal(TemporalType.TIMESTAMP)
  private Date updated_at;

  @Column private String state;
  @ElementCollection private Set<String> labels;

  @OneToOne(orphanRemoval = true, cascade = CascadeType.ALL)
  private Reaction reaction;

  /*Experimental mapping to fixing PR*/
  @JsonIgnore
  @ManyToMany(fetch = FetchType.LAZY)
  @ToString.Exclude
  private Set<ProjectIssue> projectIssue;

  @Column private boolean isBugFix;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectPull that = (ProjectPull) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public void addIssue(ProjectIssue projectIssue) {
    if (this.projectIssue == null) {
      this.projectIssue = new HashSet<>();
    }
    if (!this.projectIssue.contains(projectIssue)) {
      this.projectIssue.add(projectIssue);
    }
  }

  public void addTimeLine(Timeline timeline) {
    if (timeLine == null) {
      timeLine = new HashSet<>();
    }
    timeLine.add(timeline);
    timeline.setMessage(timeline.getMessage());
  }

  public void addCommits(List<Commit> foundCommits) {
    if (commits == null) {
      commits = new ArrayList<>();
    }
    for (Commit foundCommit : foundCommits) {
      if (commits.contains(foundCommit)) {
        continue;
      }
      commits.add(foundCommit);
    }
  }
  public static void main(String[] args){
    B b = new ProjectPull().new B();
    System.out.println();
  }
  class B{}
  class A extends B{}
}
