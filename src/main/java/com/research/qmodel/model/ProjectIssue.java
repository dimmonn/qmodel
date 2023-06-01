package com.research.qmodel.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.ProjectIssueDeserializer;
import com.research.qmodel.annotations.ProjectPullDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;
import java.util.Objects;

@Data
@Entity
@Table(name = "project_issue")
@JsonDeserialize(using = ProjectIssueDeserializer.class)
public class ProjectIssue implements BaseMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "LONGTEXT")
    private String issue;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumns({
            @JoinColumn(name = "project_owner", referencedColumnName = "owner"),
            @JoinColumn(name = "project_project_name", referencedColumnName = "project_name")
    })
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectIssue)) return false;
        ProjectIssue that = (ProjectIssue) o;
        return issue.equals(that.issue) && created_at.equals(that.created_at) && Objects.equals(closed_at, that.closed_at) && Objects.equals(updated_at, that.updated_at);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issue, created_at, closed_at, updated_at);
    }
}
