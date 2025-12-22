package com.research.qmodel.model;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.ActionsDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;


@Data
@Entity
@Table(name = "action")
@JsonDeserialize(using = ActionsDeserializer.class)
@NoArgsConstructor
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    @Column(name = "raw_check_runs", columnDefinition = "LONGTEXT")
    private String rawCheckRuns;

    @Temporal(TemporalType.TIMESTAMP)
    @ToString.Include
    private Date startedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @ToString.Include
    private Date completedAt;

    private String status;
    private String result;
    private String title;
    private String summary;
    @Column(name = "text", columnDefinition = "LONGTEXT")
    private String text;
    private String name;
    @Lob
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "events", columnDefinition = "LONGTEXT")
    private String events;

    private int failed;
    private int passed;
    private int other;
    private int total;
    private double failedPercent;
    private double passedPercent;
    private double otherPercent;


    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "commit_sha")
    private Commit commit;
}
