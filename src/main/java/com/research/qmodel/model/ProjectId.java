package com.research.qmodel.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
public class ProjectId implements Serializable {
    private String owner;
    private String projectName;
}
