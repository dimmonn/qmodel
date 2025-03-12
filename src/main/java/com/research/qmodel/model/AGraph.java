package com.research.qmodel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.AGraphDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "graph")
@JsonDeserialize(using = AGraphDeserializer.class)
@ToString(onlyExplicitlyIncluded = true)
public class AGraph implements BaseMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;
    @Column(columnDefinition = "LONGTEXT")
    private String graph;

    @ToString.Include
    @JsonIgnore
    @OneToOne(orphanRemoval = true, cascade = CascadeType.MERGE)
    private Project project;

    @JsonIgnore
    @OneToMany(mappedBy = "aGraph", cascade = CascadeType.MERGE)
    private Set<Commit> commits;

    public void addCommit(Commit commit) {
        if (commits == null) {
            commits = new HashSet<>();
        }
        if (!commits.contains(commit)) {
            commit.setAGraph(this);
            commits.add(commit);
        }
    }

}
