package com.research.qmodel.service;
import com.research.qmodel.dto.ProjectAGraph;
import com.research.qmodel.dto.ProjectToIssue;
import com.research.qmodel.dto.ProjectToPull;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.model.ProjectPull;
import com.research.qmodel.repos.AGraphRepository;
import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.repos.ProjectPullRepository;
import com.research.qmodel.repos.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataPersistance {
    private final AGraphRepository aGraphRepository;
    private final ProjectIssueRepository projectIssueRepository;
    private final ProjectPullRepository projectPullRepository;
    private final ProjectRepository projectRepository;
    private final Logger LOGGER = LoggerFactory.getLogger(DataPersistance.class);
    public DataPersistance(AGraphRepository aGraphRepository, ProjectIssueRepository projectIssueRepository, ProjectPullRepository projectPullRepository, ProjectRepository projectRepository) {
        this.aGraphRepository = aGraphRepository;
        this.projectIssueRepository = projectIssueRepository;
        this.projectPullRepository = projectPullRepository;
        this.projectRepository = projectRepository;
    }

    public List<ProjectAGraph> persistGraph(List<Project> repos, Map<Project, AGraph> ags) {
        List<ProjectAGraph> result = new ArrayList<>();
        for (Project repo : repos) {
            Project project = projectRepository.findByOwnerAndProjectName(repo.getOwner(), repo.getProjectName());
            AGraph aGraph = ags.get(repo);
            if (project == null) {
                project = new Project(repo.getOwner(), repo.getProjectName());
            }
            if (aGraph != null && aGraph.equals("[]")) {
                project.setAGraph(aGraph);
                aGraph.setProject(project);
                LOGGER.info(aGraph.toString());
                aGraphRepository.save(aGraph);
                projectRepository.save(project);
            }
            result.add(new ProjectAGraph(project, aGraph));
        }
        return result;
    }

    public List<ProjectToPull> persistPulls(List<Project> repos, Map<Project, List<ProjectPull>> ppmap) {
        List<ProjectToPull> result = new ArrayList<>();
        for (Project repo : repos) {
            Project project = projectRepository.findByOwnerAndProjectName(repo.getOwner(), repo.getProjectName());
            List<ProjectPull> projectPull = ppmap.get(repo);
            if (project == null) {
                project = new Project(repo.getOwner(), repo.getProjectName());
            }
            if (projectPull != null) {
                for (int i = 0; i < projectPull.size(); i++) {
                    if (project.addProjectPull(projectPull.get(i))) {
                        projectPullRepository.save(projectPull.get(i));
                    }
                }
                projectRepository.save(project);
            }
            result.add(new ProjectToPull(project, projectPull));
        }
        return result;
    }

    public List<ProjectToIssue> persistIssues(List<Project> repos, Map<Project, List<ProjectIssue>> pimap) {
        List<ProjectToIssue> result = new ArrayList<>();
        for (Project repo : repos) {
            Project project = projectRepository.findByOwnerAndProjectName(repo.getOwner(), repo.getProjectName());
            List<ProjectIssue> projectIssue = pimap.get(repo);
            if (project == null) {
                project = new Project(repo.getOwner(), repo.getProjectName());
            }
            if (projectIssue != null) {
                for (int i = 0; i < projectIssue.size(); i++) {
                    if (project.addProjectIssue(projectIssue.get(i))) {
                        projectIssueRepository.save(projectIssue.get(i));
                    }
                }
                projectRepository.save(project);
            }
            result.add(new ProjectToIssue(project, projectIssue));
        }
        return result;
    }
}
