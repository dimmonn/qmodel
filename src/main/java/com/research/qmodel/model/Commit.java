package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.CommitDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class Commit {
    @Id
    @Column(name = "sha")
    private String sha;
    @Id
    @Column(name = "commit_type")
    private CommitType commitType;
    @Temporal(TemporalType.TIMESTAMP)
    private Date commitDate;
    @MapsId
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private AGraph aGraph;
    @MapsId
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<FileChange> fileChanges;
    @Column
    private int numOfFilesChanged;
    @Column
    private String author;
    @Column
    private String email;
    @Column(columnDefinition = "LONGTEXT")
    private String message;
    @Column
    private int commentCount;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "commit_parents",
            joinColumns = {
                    @JoinColumn(name = "child_sha", referencedColumnName = "sha"),
                    @JoinColumn(name = "child_commit_type", referencedColumnName = "commit_type")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "parent_sha", referencedColumnName = "sha"),
                    @JoinColumn(name = "parent_commit_type", referencedColumnName = "commit_type")
            }
    )
    private List<Commit> parents = new ArrayList<>();
}
