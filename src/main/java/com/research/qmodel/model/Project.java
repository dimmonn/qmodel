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
@IdClass(ProjectId.class)
@NoArgsConstructor
public class Project {

    @Id
    @Column(name = "owner")
    private String owner;
    @Id
    @Column(name = "project_name")
    private String projectName;
    @JsonIgnore
    @MapsId
    @OneToOne(mappedBy = "project", orphanRemoval = true, cascade = CascadeType.ALL)
    private AGraph aGraph;

    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectIssue> projectIssue;

    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectPull> projectPull;

    public Project(String owner, String projectName) {
        this.owner = owner;
        this.projectName = projectName;
    }

    public boolean addProjectPull(ProjectPull pull) {
        if (projectPull == null) {
            projectPull = new ArrayList<>();
        }
        if (!projectPull.contains(pull)) {
            pull.setProject(this);
            projectPull.add(pull);
            return true;
        }
        return false;
    }

    public boolean addProjectIssue(ProjectIssue issue) {
        if (projectIssue == null) {
            projectIssue = new ArrayList<>();
        }
        if (!projectIssue.contains(issue)) {
            issue.setProject(this);
            projectIssue.add(issue);
            return true;
        }
        return false;
    }

    public void removeProjectPull(ProjectPull pull) {
        projectPull.remove(pull);
        pull.setProject(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return owner.equals(project.owner) && projectName.equals(project.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, projectName);
    }

    @Override
    public String toString() {
        return "Project{" +
                "owner='" + owner + '\'' +
                ", projectName='" + projectName + '\'' +
                '}';
    }
}