package com.research.qmodel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.ProjectIssueDeserializer;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

import java.util.*;

@Entity
@Table(name = "project_issue")
@IdClass(IssueID.class)
@JsonDeserialize(using = ProjectIssueDeserializer.class)
@Transactional
@Setter
@Getter
public class ProjectIssue implements BaseMetric {
  @Id private Long id;
  @Id private String projectOwner;
  @Id private String projectName;

  @Column(columnDefinition = "LONGTEXT")
  private String rawIssue;

  @ElementCollection private Set<String> labels;
  @Column private String state;

  @Id
  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private Project project;

  @Column(columnDefinition = "LONGTEXT")
  private String title;

  @Temporal(TemporalType.TIMESTAMP)
  private Date created_at;

  @Temporal(TemporalType.TIMESTAMP)
  private Date closed_at;

  @Temporal(TemporalType.TIMESTAMP)
  private Date updated_at;

  @Temporal(TemporalType.TIMESTAMP)
  private Date merged_at;

  @JsonIgnore
  @ToString.Exclude
  @ManyToMany(fetch = FetchType.LAZY)
  private List<Commit> fixingCommits;

  @JsonIgnore
  @ManyToMany(
      cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  private List<Commit> bugIntroducingCommits;

  /*Experimental mapping to fixing PR*/
  @ToString.Exclude
  @JsonIgnore
  @ManyToMany(fetch = FetchType.LAZY)
  private Set<ProjectPull> projectPull;

  @ToString.Exclude
  @JsonIgnore
  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private ProjectPull fixPR;

  @OneToOne(orphanRemoval = true, cascade = CascadeType.ALL)
  private Reaction reaction;

  @Column private long fixPr;

  public void setPrThatFixesIssue(long fixPr) {
    this.fixPr = fixPr;
  }

  @ToString.Exclude @EqualsAndHashCode.Exclude @ElementCollection
  private Set<String> assignees = new HashSet<>();

  @ToString.Exclude
  @JsonIgnore
  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Timeline> timeLine;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProjectIssue that = (ProjectIssue) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public void addTimeLine(Timeline timeline) {
    if (timeLine == null) {
      timeLine = new HashSet<>();
    }
    timeLine.add(timeline);
  }

  public void addAssignees(String assignee) {
    if (this.assignees == null) {
      this.assignees = new HashSet<>();
    }
    this.assignees.add(assignee);
  }

  public void addProjectPull(ProjectPull projectPull) {
    if (this.projectPull == null) {
      this.projectPull = new HashSet<>();
    }
    if (!this.projectPull.contains(projectPull)) {
      this.projectPull.add(projectPull);
    }
  }

  public void setFixPrNum(long fixPr) {
    this.fixPr = fixPr;
  }

  public void addCommits(List<Commit> foundCommits) {
    if (fixingCommits == null) {
      fixingCommits = new ArrayList<>();
    }
    for (Commit foundCommit : foundCommits) {
      if (fixingCommits.contains(foundCommit)) {
        continue;
      }
      fixingCommits.add(foundCommit);
    }
  }

  public void addBugIntroducing(Commit foundCommit) {
    if (bugIntroducingCommits == null) {
      bugIntroducingCommits = new ArrayList<>();
    }

    if (!bugIntroducingCommits.contains(foundCommit)) {

      bugIntroducingCommits.add(foundCommit);
    }
  }

  @Override
  public void setProject(Project project) {
    this.project = project;
  }

  @Override
  public void setReaction(Reaction reaction) {
    this.reaction = reaction;
  }
}
