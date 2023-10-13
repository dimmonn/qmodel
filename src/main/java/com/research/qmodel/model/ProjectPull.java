package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.ProjectPullDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;
import java.util.Objects;

@Data
@Entity
@Table(name = "project_pull")
@JsonDeserialize(using = ProjectPullDeserializer.class)
public class ProjectPull implements BaseMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "LONGTEXT")
    private String pull;
    @Column(columnDefinition = "LONGTEXT")
    private String title;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumns({
            @JoinColumn(name = "project_owner", referencedColumnName = "owner"),
            @JoinColumn(name = "project_project_name", referencedColumnName = "project_name")
    })

    private Project project;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created_at;
    @Temporal(TemporalType.TIMESTAMP)
    private Date closed_at;
    @Temporal(TemporalType.TIMESTAMP)
    private Date merged_at;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated_at;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectPull)) return false;
        ProjectPull that = (ProjectPull) o;
        return pull.equals(that.pull) && created_at.equals(that.created_at) && Objects.equals(closed_at, that.closed_at) && Objects.equals(merged_at, that.merged_at);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pull, created_at, closed_at, merged_at);
    }
}
