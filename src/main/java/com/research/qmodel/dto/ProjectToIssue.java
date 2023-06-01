package com.research.qmodel.dto;

import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.model.ProjectPull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProjectToIssue {
    private Project project;
    private List<ProjectIssue> projectIssue;
}