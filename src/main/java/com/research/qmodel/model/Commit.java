package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.CommitDeserializer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Exclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.relational.core.mapping.Table;

@Entity
@Table(name = "commit")
@JsonDeserialize(using = CommitDeserializer.class)
@Getter
@Setter
@RequiredArgsConstructor
@IdClass(CommitID.class)
@ToString(onlyExplicitlyIncluded = true)
public class Commit {
    @Id
    @Column(name = "sha")
    @ToString.Include
    private String sha;

    @ToString.Exclude
    @Column(columnDefinition = "LONGTEXT")
    private String rawData;

    @EqualsAndHashCode.Exclude
    @Temporal(TemporalType.TIMESTAMP)
    @ToString.Include
    private Date commitDate;

    @Exclude
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private AGraph aGraph;

    @ManyToMany(
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @ToString.Exclude
    private List<FileChange> fileChanges;

    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private int numOfFilesChanged;
    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private String author;
    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private String email;

    @Column(columnDefinition = "LONGTEXT")
    @EqualsAndHashCode.Exclude
    private String message;

    @ToString.Include
    @Column
    @EqualsAndHashCode.Exclude
    private int commentCount;

    @Column
    @EqualsAndHashCode.Exclude
    Integer numberOfVertices;
    @Column
    @EqualsAndHashCode.Exclude
    Integer numberOfBranches;
    @Column
    @EqualsAndHashCode.Exclude
    Integer inDegree;
    @Column
    @EqualsAndHashCode.Exclude
    Integer outDegree;
    @Column
    @EqualsAndHashCode.Exclude
    Double averageDegree;
    @Column
    @EqualsAndHashCode.Exclude
    Integer minDepthOfCommitHistory;
    @Column
    @EqualsAndHashCode.Exclude
    Integer maxDepthOfCommitHistory;
    @Column
    @EqualsAndHashCode.Exclude
    Integer mergeCount;
    @Column
    @EqualsAndHashCode.Exclude
    Boolean isMerge;
    @Column
    @EqualsAndHashCode.Exclude
    String projectName;
    @Column
    @EqualsAndHashCode.Exclude
    String projectOwner;
    @Column
    private String state;
    @ElementCollection
    private Set<String> branches;
    @ElementCollection
    private Set<String> subGraphNodes;
    /**
     * Distance to current branch start (child-after-most-recent split = 0).
     */
    @Column(name = "distance_to_branch_start", nullable = true)
    @EqualsAndHashCode.Exclude
    private Integer distanceToBranchStart;

    /**
     * Number of distinct heads merged into current FP segment (via non-FP sides).
     */
    @Column(name = "upstream_heads_unique_on_segment", nullable = true)
    @EqualsAndHashCode.Exclude
    private Integer upstreamHeadsUniqueOnSegment;

    /**
     * Days since last merge on current FP segment (0 if none).
     */
    @Column(name = "days_since_last_merge_on_segment", nullable = true)
    @EqualsAndHashCode.Exclude
    private Integer daysSinceLastMergeOnSegment;


    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass =
                o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer()
                        .getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass =
                this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                        .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        Commit commit = (Commit) o;
        return getSha() != null && Objects.equals(getSha(), commit.getSha());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(sha);
    }

}
