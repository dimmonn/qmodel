package com.research.qmodel.dto;

import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Project;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectAGraph {
    private Project project;
    private AGraph aGraph;
}
