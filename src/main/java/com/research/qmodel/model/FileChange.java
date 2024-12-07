package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.FileChangesDeserializer;
import jakarta.persistence.*;
import java.util.Set;
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
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public class FileChange {
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ToString.Include
    @Column(name = "sha")
    private String sha;
    @ToString.Include
    @Temporal(TemporalType.TIMESTAMP)
    @EqualsAndHashCode.Exclude
    private Date changeDate;
    @ManyToMany(mappedBy = "fileChanges", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private List<Commit> commit;
    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private int totalAdditions;
    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private int totalDeletions;
    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private int totalChanges;
    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private String fileName;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(name = "patch", columnDefinition = "LONGTEXT")
    private String patch;
    @ElementCollection
    @EqualsAndHashCode.Exclude
    private Set<Integer> changedLines;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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