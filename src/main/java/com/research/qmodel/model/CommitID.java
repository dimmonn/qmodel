package com.research.qmodel.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
public class CommitID implements Serializable {
    private String sha;
    private CommitType commitType;
}
