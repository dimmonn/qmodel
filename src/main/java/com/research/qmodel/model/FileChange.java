package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.FileChangesDeserializer;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "file_changes")
@JsonDeserialize(using = FileChangesDeserializer.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Temporal(TemporalType.TIMESTAMP)
    private Date changeDate;
    @ManyToMany(mappedBy = "fileChanges", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<Commit> commit;
    @Column
    private int totalAdditions;
    @Column
    private int totalDeletions;
    @Column
    private int totalChanges;
    @Column
    private String fileName;

    public void addCommit(Commit commit) {
        if (this.commit == null || this.commit.isEmpty()) {
            this.commit = new ArrayList<>();
            this.commit.add(commit);
        } else {
            if (this.commit.stream().filter(c -> c.getSha() == commit.getSha()).findFirst().isEmpty()) {
                this.commit.add(commit);
            }
        }
    }

}