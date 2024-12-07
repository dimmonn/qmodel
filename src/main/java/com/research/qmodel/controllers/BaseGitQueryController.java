package com.research.qmodel.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.*;
import com.research.qmodel.service.BasicQueryService;
import com.research.qmodel.service.DataPersistance;
import com.research.qmodel.service.findbugs.BasicBugFinder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(value = {"api/v1/metrics"})
public class BaseGitQueryController implements FileJsonReader {
  private final BasicQueryService basicQueryService;
  private final DataPersistance dataPersistance;
  @Autowired private BasicBugFinder basicBugFinder;

  public BaseGitQueryController(
      BasicQueryService basicQueryService, DataPersistance dataPersistance) {
    this.basicQueryService = basicQueryService;
    this.dataPersistance = dataPersistance;
  }

  @GetMapping(value = "/repos")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Project>> listProjects() {
    return new ResponseEntity(dataPersistance.retrieveProjects(), HttpStatus.OK);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/actions")
  @ResponseStatus(HttpStatus.OK)
  public Object getActions(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo) {

    Actions actions =
        basicQueryService.retrievemetrics(
            "%s" + String.format("repos/%s/%s/actions/runs", owner, repo),
            new TypeReference<>() {});
    return new ResponseEntity(
        dataPersistance.persistActions(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), actions)),
        HttpStatus.OK);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/retrieveFixingCommits")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Commit>> searchFixingCommits(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo)
      throws JsonProcessingException {
    return new ResponseEntity<>(
        basicBugFinder.findAllBugsFixingCommits(repo, owner, 2), HttpStatus.OK);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/{id}/retrieveBugIntroducingCommits")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Commit>> searchDefectsCommits(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo,
      @PathVariable(value = "id")
          @Parameter(name = "id", in = ParameterIn.PATH, description = "Issue id")
          Long id)
      throws IOException, GitAPIException {
    return new ResponseEntity<>(
        basicBugFinder.findBugIntroducingCommits(owner, repo, id, 2), HttpStatus.OK);
  }


  @GetMapping(value = "/repos/{owner}/{repo}/retrieveBugIntroducingCommits")
  @ResponseStatus(HttpStatus.OK)
  public void searchAllDefectsCommits(
      @PathVariable(value = "owner")
      @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
      String owner,
      @PathVariable(value = "repo")
      @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
      String repo)
      throws IOException, GitAPIException {

        basicBugFinder.traceCommitsToOrigin(owner, repo, 2);
  }

  @GetMapping(value = "/repos/{owner}/{repo}")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryAg(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo) {
    AGraph ag =
        basicQueryService.retrievemetrics(
            "%s" + String.format("repos/%s/%s/commits", owner, repo) + "%s",
            new TypeReference<>() {});
    return new ResponseEntity(
        dataPersistance.persistGraph(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), ag)),
        HttpStatus.OK);
  }

  @GetMapping(value = "/repos/ag")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<JsonNode> baseQueryBunchAg(@RequestBody List<Project> repos) {
    Map<Project, AGraph> ags =
        queryProvider(repos, "repos/%s/%s/commits", new TypeReference<>() {});
    return new ResponseEntity(dataPersistance.persistGraph(repos, ags), HttpStatus.OK);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/pulls")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryPull(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo) {
    List<ProjectPull> projectPull =
        basicQueryService.retrievemetrics(
            "%s" + String.format("repos/%s/%s/pulls", owner, repo) + "%s&state=closed",
            new TypeReference<>() {});
    return new ResponseEntity(
        dataPersistance.persistPulls(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), projectPull)),
        HttpStatus.OK);
  }

  @GetMapping(value = "/repos/pulls")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryBunchPulls(@RequestBody List<Project> repos) {
    Map<Project, List<ProjectPull>> pulls =
        queryProvider(repos, "repos/%s/%s/pulls", new TypeReference<>() {});
    return new ResponseEntity(dataPersistance.persistPulls(repos, pulls), HttpStatus.OK);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/issues")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryIssueResolutionTime(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo) {
    List<ProjectIssue> projectIssues =
        basicQueryService.retrievemetrics(
            "%s" + String.format("repos/%s/%s/issues", owner, repo) + "%s" + "&state=closed",
            new TypeReference<>() {});
    return new ResponseEntity(
        dataPersistance.persistIssues(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), projectIssues)),
        HttpStatus.OK);
  }

  @GetMapping(value = "/repos/fixtime")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryBunchResolutionTime(@RequestBody List<Project> repos) {
    Map<Project, List<ProjectIssue>> pulls =
        queryProvider(repos, "repos/%s/%s/issues", new TypeReference<>() {});
    return new ResponseEntity(dataPersistance.persistIssues(repos, pulls), HttpStatus.OK);
  }

  private <T> Map<Project, T> queryProvider(
      @RequestBody List<Project> repos, String url, TypeReference<? extends T> targetType) {
    Map<Project, T> pulls = new HashMap<>();
    for (Project project : repos) {
      pulls.put(
          project,
          basicQueryService.retrievemetrics(
              "%s"
                  + String.format(url, project.getProjectOwner(), project.getProjectName())
                  + "%s&state=closed",
              targetType));
    }
    return pulls;
  }
}
