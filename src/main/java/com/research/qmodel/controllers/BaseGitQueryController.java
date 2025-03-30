package com.research.qmodel.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.research.qmodel.dto.ProjectAGraph;
import com.research.qmodel.graph.GitMaintainable;
import com.research.qmodel.model.*;
import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.service.BasicQueryService;
import com.research.qmodel.service.DataPersistance;
import com.research.qmodel.service.findbugs.BasicBugFinder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.io.IOException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(value = {"api/v1/metrics"})
public class BaseGitQueryController extends GitMaintainable implements FileJsonReader {
  private final BasicQueryService basicQueryService;
  private final DataPersistance dataPersistance;
  @Autowired private BasicBugFinder basicBugFinder;
  @Autowired private ProjectIssueRepository projectIssueRepository;

  @Autowired private ProjectNameCache projectNameCache;

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
            new TypeReference<>() {},
            "?",
            "&");
    return new ResponseEntity(
        dataPersistance.persistActions(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), actions)),
        HttpStatus.OK);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/retrieveFixingCommits")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<String>> searchFixingCommits(
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
          String repo) {
    basicBugFinder.traceCommitsToOrigin(owner, repo, 2);
  }

  /*
  TODO
  */
  private Set<String> getForks(String owner, String repo) {

    List<Map<String, JsonNode>> retrievemetrics =
        basicQueryService.retrievemetrics(
            "%s" + String.format("repos/%s/%s/forks", owner, repo) + "%s",
            new TypeReference<>() {},
            "?",
            "&");
    return retrievemetrics.parallelStream()
        .map(o -> o.get("full_name").asText())
        .collect(Collectors.toSet());
  }

  //
  //  private static Set<String> getCommits(String repo) throws IOException {
  //    Set<String> commits = new HashSet<>();
  //    String url = GITHUB_API_BASE + repo + "/commits?per_page=" + MAX_RESULTS;
  //
  //    JsonNode response = makeApiRequest(url);
  //    if (response != null) {
  //      for (JsonNode commit : response) {
  //        commits.add(commit.get("sha").asText());
  //      }
  //    }
  //    return commits;
  //  }

  /*
  TODO
  */

  @GetMapping(value = "/repos/{owner}/{repo}/forked")
  @ResponseStatus(HttpStatus.OK)
  public Set<String> forkedCommits(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo) {
    projectNameCache.setProjectOwner(owner);
    projectNameCache.setProjectName(repo);
    Set<String> forks = getForks(owner, repo);
    AGraph aGraph = basicQueryService.retrieveForks(owner, repo, forks);
    dataPersistance.persistGraph(
        List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), aGraph));
    return forks;
  }


  @GetMapping(value = "/repos/{owner}/{repo}/commits/{sha}")
  @ResponseStatus(HttpStatus.OK)
  public Object retrieveCommit(
      @PathVariable(value = "owner")
      @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
      String owner,
      @PathVariable(value = "repo")
      @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
      String repo, @PathVariable(value = "sha")
  @Parameter(name = "sha", in = ParameterIn.PATH, description = "commit sha")
  String sha) {
    projectNameCache.setProjectOwner(owner);
    projectNameCache.setProjectName(repo);

    AGraph graphWithoutForks = basicQueryService.retrieveCommitBySha(owner, repo, sha);

    return new ResponseEntity<>(
        dataPersistance.persistGraph(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), graphWithoutForks)),
        HttpStatus.OK);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/commits")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryAg(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo) {
    projectNameCache.setProjectOwner(owner);
    projectNameCache.setProjectName(repo);
/*    AGraph graphWithForks = basicQueryService.retrieveForks(owner, repo, getForks(owner, repo));
    dataPersistance.persistGraph(
        List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), graphWithForks));*/
    AGraph graphWithoutForks =
        basicQueryService.retrievemetrics(
            "%s" + String.format("repos/%s/%s/commits", owner, repo) + "%s",
            new TypeReference<>() {},
            "?",
            "&");

    return new ResponseEntity<>(
        dataPersistance.persistGraph(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), graphWithoutForks)),
        HttpStatus.OK);
  }

  @DeleteMapping(value = "/repos/{owner}/{repo}/issues")
  @ResponseStatus(HttpStatus.OK)
  public void delete(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo) {
    List<ProjectIssue> issues = projectIssueRepository.findByProject(owner, repo);
    projectIssueRepository.deleteAll(issues);
  }

  @GetMapping(value = "/repos/{owner}/{repo}/forks")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryForks(
      @PathVariable(value = "owner")
          @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
          String owner,
      @PathVariable(value = "repo")
          @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
          String repo)
      throws Exception {
    String path = "/Users/dpolishchuk/" + owner + "_" + repo;
    cloneRepo(owner, repo, path);
    Set<String> forkedCommits = getForkedCommits(path);
    return null;
  }

  @GetMapping(value = "/repos/ag")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<ProjectAGraph>> baseQueryBunchAg(@RequestBody List<Project> repos) {
    Map<Project, AGraph> ags =
        queryProvider(repos, "repos/%s/%s/commits", new TypeReference<>() {});
    return new ResponseEntity<>(dataPersistance.persistGraph(repos, ags), HttpStatus.OK);
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
            new TypeReference<>() {},
            "?",
            "?");
    return new ResponseEntity<>(
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
            new TypeReference<>() {},
            "?",
            "?");
    return new ResponseEntity<>(
        dataPersistance.persistIssues(
            List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), projectIssues)),
        HttpStatus.OK);
  }

  @GetMapping(value = "/repos/fixtime")
  @ResponseStatus(HttpStatus.OK)
  public Object baseQueryBunchResolutionTime(@RequestBody List<Project> repos) {
    Map<Project, List<ProjectIssue>> pulls =
        queryProvider(repos, "repos/%s/%s/issues", new TypeReference<>() {});
    return new ResponseEntity<>(dataPersistance.persistIssues(repos, pulls), HttpStatus.OK);
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
              targetType,
              "?",
              "?"));
    }
    return pulls;
  }
}
