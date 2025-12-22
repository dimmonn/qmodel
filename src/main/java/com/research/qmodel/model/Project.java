package com.research.qmodel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "project")
@IdClass(ProjectID.class)
@NoArgsConstructor
public class Project {

  @Id private String projectOwner;
  @Id private String projectName;

  @JsonIgnore
  @OneToOne(mappedBy = "project", orphanRemoval = true, cascade = CascadeType.ALL)
  private AGraph aGraph;

  @JsonIgnore
  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProjectIssue> projectIssue;

  @JsonIgnore
  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProjectPull> projectPull;

  public Project(String projectOwner, String projectName) {
    this.projectOwner = projectOwner;
    this.projectName = projectName;
  }

  public void addProjectPull(ProjectPull pull) {
    if (pull == null) {
      return;
    }
    if (projectPull == null) {
      projectPull = new ArrayList<>();
    }
    if (!projectPull.contains(pull)) {
      pull.setProject(this);
      pull.setProjectOwner(projectOwner);
      pull.setProjectName(projectName);
      projectPull.add(pull);
    }
  }

  public void addProjectIssue(ProjectIssue issue) {
    if (projectIssue == null) {
      projectIssue = new ArrayList<>();
    }
    if (!projectIssue.contains(issue)) {
      issue.setProject(this);
      issue.setProjectName(projectName);
      issue.setProjectOwner(projectOwner);
      projectIssue.add(issue);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Project)) return false;
    Project project = (Project) o;
    return projectOwner.equals(project.projectOwner) && projectName.equals(project.projectName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectOwner, projectName);
  }

  @Override
  public String toString() {
    return "Project{"
        + "owner='"
        + projectOwner
        + '\''
        + ", projectName='"
        + projectName
        + '\''
        + '}';
  }
}
