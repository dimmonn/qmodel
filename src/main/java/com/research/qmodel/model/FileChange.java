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
@ToString(onlyExplicitlyIncluded = true)
public class FileChange {
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ToString.Include
    @Temporal(TemporalType.TIMESTAMP)
    private Date changeDate;
    @ManyToMany(mappedBy = "fileChanges", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<Commit> commit;
    @ToString.Include
    @Column
    private int totalAdditions;
    @ToString.Include
    @Column
    private int totalDeletions;
    @ToString.Include
    @Column
    private int totalChanges;
    @ToString.Include
    @Column
    private String fileName;
    @ToString.Exclude
    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

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