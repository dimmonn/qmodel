package com.research.qmodel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.research.qmodel.annotations.ActionsDeserializer;
import com.research.qmodel.annotations.CommitDeserializer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Entity
@Table(name = "actions")
@IdClass(ProjectID.class)
@JsonDeserialize(using = ActionsDeserializer.class)
@NoArgsConstructor
public class Actions {

    @Id
    private String projectOwner;
    @Id
    private String projectName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String allActions;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Project project;

    public Actions(String allActions, Project project) {
        this.allActions = allActions;
        this.project = project;
    }
}