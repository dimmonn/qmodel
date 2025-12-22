package com.research.qmodel.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.TimelineDeserializer;
import jakarta.persistence.*;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "timeline")
@JsonDeserialize(using = TimelineDeserializer.class)
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public class Timeline {
    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @Column(columnDefinition = "LONGTEXT")
    private String rawData;

    @ToString.Exclude
    @Column(columnDefinition = "LONGTEXT")
    private String message;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private ProjectIssue projectIssue;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private ProjectPull projectPull;
    @ElementCollection
    private Set<Long> pullIds;

    @EqualsAndHashCode.Exclude
    @Temporal(TemporalType.TIMESTAMP)
    @ToString.Include
    private Date createdAt;

    public void addPullId(Long timelineId) {
        if (pullIds == null) {
            pullIds = new HashSet<>();
        }
        pullIds.add(timelineId);
    }
}
