package com.research.qmodel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.AGraphDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "graph")
@JsonDeserialize(using = AGraphDeserializer.class)
@ToString
public class AGraph implements BaseMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "LONGTEXT")
    private String graph;

    @JsonIgnore
    @OneToOne(orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "repo_owner", referencedColumnName = "owner"),
            @JoinColumn(name = "repo_project_name", referencedColumnName = "project_name")
    })
    private Project project;

    @JsonIgnore
    @OneToMany(mappedBy = "aGraph", cascade = CascadeType.ALL)
    private List<Commit> commits;

    public boolean addCoommit(Commit commit) {
        if (commits == null) {
            commits = new ArrayList<>();
        }
        if (!commits.contains(commit)) {
            commit.setAGraph(this);
            commits.add(commit);
            return true;
        }
        return false;
    }

}
