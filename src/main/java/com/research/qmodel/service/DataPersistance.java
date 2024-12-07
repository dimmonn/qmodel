package com.research.qmodel.service;

import com.research.qmodel.dto.ProjectAGraph;
import com.research.qmodel.dto.ProjectToIssue;
import com.research.qmodel.dto.ProjectToPull;
import com.research.qmodel.model.*;
import com.research.qmodel.repos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DataPersistance {
  private final AGraphRepository aGraphRepository;
  private final ProjectIssueRepository projectIssueRepository;
  private final ProjectPullRepository projectPullRepository;
  private final ProjectRepository projectRepository;
  private final Logger LOGGER = LoggerFactory.getLogger(DataPersistance.class);
  private final ActionsRepository actionsRepository;

  public DataPersistance(
      AGraphRepository aGraphRepository,
      ProjectIssueRepository projectIssueRepository,
      ProjectPullRepository projectPullRepository,
      ProjectRepository projectRepository,
      ActionsRepository actionsRepository) {
    this.aGraphRepository = aGraphRepository;
    this.projectIssueRepository = projectIssueRepository;
    this.projectPullRepository = projectPullRepository;
    this.projectRepository = projectRepository;
    this.actionsRepository = actionsRepository;
  }

  public List<ProjectAGraph> persistGraph(List<Project> repos, Map<Project, AGraph> ags) {
    List<ProjectAGraph> result = new ArrayList<>();
    for (Project repo : repos) {
      Optional<Project> foundProject =
          projectRepository.findById(new ProjectID(repo.getProjectOwner(), repo.getProjectName()));
      AGraph aGraph = ags.get(repo);
      Project project = null;
      if (!foundProject.isPresent()) {
        project = new Project(repo.getProjectOwner(), repo.getProjectName());
      } else {
        project = foundProject.get();
      }
      if (aGraph != null && aGraph.getGraph() != null && !aGraph.getGraph().equals("[]")) {
        project.setAGraph(aGraph);
        aGraph.setProject(project);
        try{

        aGraphRepository.save(aGraph);
        }catch (Exception e){
          System.out.println();
        }
        //projectRepository.save(project);
      }
      result.add(new ProjectAGraph(project, aGraph));
    }
    return result;
  }

  public List<ProjectToPull> persistPulls(
      List<Project> repos, Map<Project, List<ProjectPull>> ppmap) {
    List<ProjectToPull> result = new ArrayList<>();
    for (Project repo : repos) {
      Optional<Project> foundProject =
          projectRepository.findById(new ProjectID(repo.getProjectOwner(), repo.getProjectName()));
      List<ProjectPull> projectPull = ppmap.get(repo);
      Project project = null;
      if (!foundProject.isPresent()) {
        project = new Project(repo.getProjectOwner(), repo.getProjectName());
      } else {
        project = foundProject.get();
      }
      if (projectPull != null) {
        for (int i = 0; i < projectPull.size(); i++) {
          project.addProjectPull(projectPull.get(i));
        }
        projectRepository.save(project);
      }
      result.add(new ProjectToPull(project, projectPull));
    }
    return result;
  }

  public List<ProjectToIssue> persistIssues(
      List<Project> repos, Map<Project, List<ProjectIssue>> pimap) {
    List<ProjectToIssue> result = new ArrayList<>();
    for (Project repo : repos) {
      Optional<Project> foundProject =
          projectRepository.findById(new ProjectID(repo.getProjectOwner(), repo.getProjectName()));

      List<ProjectIssue> projectIssue = pimap.get(repo);
      Project project;
      if (!foundProject.isPresent()) {
        project = new Project(repo.getProjectOwner(), repo.getProjectName());
      } else {
        project = foundProject.get();
      }
      if (projectIssue != null) {
        for (int i = 0; i < projectIssue.size(); i++) {
          if (projectIssue.get(i) != null) {
            project.addProjectIssue(projectIssue.get(i));
          }
        }
        projectRepository.save(project);
      }
      result.add(new ProjectToIssue(project, projectIssue));
    }
    return result;
  }

  public Object persistActions(List<Project> projects, Map<Project, Actions> projectActionsMap) {
    Optional<Project> foundProject =
        projectRepository.findById(
            new ProjectID(projects.get(0).getProjectOwner(), projects.get(0).getProjectName()));
    Actions actions = projectActionsMap.get(projects.get(0));
    actions.setProjectName(projects.get(0).getProjectName());
    actions.setProjectOwner(projects.get(0).getProjectOwner());
    if (foundProject.isPresent()) {
      actions.setProject(foundProject.get());
      actionsRepository.save(actions);
      return actions;
    }
    actions.setProject(projects.get(0));
    actionsRepository.save(actions);
    return actions;
  }

  public void persistCommits(List<Commit> commitsWithDefects) {
    for (Commit commit : commitsWithDefects) {
      LOGGER.info("Persisting commit with defects: {}", commit);
    }
    // TODO: Implement actual commit persistence with defects
  }

  public List<Project> retrieveProjects() {
    return projectRepository.findAll();
  }
}
