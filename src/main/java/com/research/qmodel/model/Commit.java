package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.CommitDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "commit")
@JsonDeserialize(using = CommitDeserializer.class)
@Data
public class Commit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Temporal(TemporalType.TIMESTAMP)
    private Date commitDate;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private AGraph aGraph;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<FileChange> fileChanges;
    @Column
    private int numOfFilesChanged;
}
