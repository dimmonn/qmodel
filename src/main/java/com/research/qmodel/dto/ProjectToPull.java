package com.research.qmodel.dto;

import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectPull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProjectToPull {
    private Project project;
    private List<ProjectPull> projectPull;
}