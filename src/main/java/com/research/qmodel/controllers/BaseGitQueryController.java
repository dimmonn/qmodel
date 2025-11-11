package com.research.qmodel.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(value = {"api/v1/metrics"})
public class BaseGitQueryController extends GitMaintainable implements FileJsonReader {
    private final BasicQueryService basicQueryService;
    private final DataPersistance dataPersistance;
    private final BasicBugFinder basicBugFinder;
    private final ProjectIssueRepository projectIssueRepository;
    @Value("${app.page_size}")
    private int PAGE_SIZE;
    private final ProjectNameCache projectNameCache;

    public BaseGitQueryController(
            BasicQueryService basicQueryService, DataPersistance dataPersistance, ProjectNameCache projectNameCache, ProjectIssueRepository projectIssueRepository, BasicBugFinder basicBugFinder) {
        this.basicQueryService = basicQueryService;
        this.dataPersistance = dataPersistance;
        this.projectNameCache = projectNameCache;
        this.projectIssueRepository = projectIssueRepository;
        this.basicBugFinder = basicBugFinder;
    }

    @GetMapping(value = "/repos")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Project>> listProjects() {
        return new ResponseEntity<>(dataPersistance.retrieveProjects(), HttpStatus.OK);
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
                basicQueryService.retrieveMetrics(
                        "%s" + String.format("repos/%s/%s/actions/runs", owner, repo),
                        new TypeReference<>() {
                        },
                        "?",
                        "&",
                        false);
        return new ResponseEntity<>(
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
            String repo,
            @RequestParam(required = false, defaultValue = "1") int depth) {
        basicBugFinder.traceCommitsToOrigin(owner, repo, depth);
    }

    private Set<String> getForks(String owner, String repo) {

        List<Map<String, JsonNode>> retrievemetrics =
                basicQueryService.retrieveMetrics(
                        "%s" + String.format("repos/%s/%s/forks", owner, repo) + "%s",
                        new TypeReference<>() {
                        },
                        "?",
                        "&",
                        false);
        return retrievemetrics.parallelStream()
                .map(o -> o.get("full_name").asText())
                .collect(Collectors.toSet());
    }


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
            String repo,
            @PathVariable(value = "sha")
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
        final int FLUSH_EVERY = PAGE_SIZE / 300;
        basicQueryService.retrieveMetricsWithStreaming(
                "%s" + String.format("repos/%s/%s/commits", owner, repo) + "%s",
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                },
                "?",
                "&",
                false,
                FLUSH_EVERY,
                (AGraph agraphBatch) -> dataPersistance.persistGraph(
                        List.of(new Project(owner, repo)),
                        Map.of(new Project(owner, repo), agraphBatch))
        );

        return new ResponseEntity<>("Streaming import completed", HttpStatus.OK);
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
    public Set<String> baseQueryForks(
            @PathVariable(value = "owner")
            @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
            String owner,
            @PathVariable(value = "repo")
            @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
            String repo)
            throws Exception {
        String path = "/Users/dima/" + owner + "_" + repo;
        cloneRepo(owner, repo, path);
        return getForkedCommits(path);
    }

    @GetMapping(value = "/repos/ag")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<ProjectAGraph>> baseQueryBunchAg(@RequestBody List<Project> repos) {
        Map<Project, AGraph> ags =
                queryProvider(repos, "repos/%s/%s/commits", new TypeReference<>() {
                });
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
                basicQueryService.retrieveMetrics(
                        "%s" + String.format("repos/%s/%s/pulls", owner, repo) + "%s&state=closed",
                        new TypeReference<>() {
                        },
                        "?",
                        "&",
                        false);
        return new ResponseEntity<>(
                dataPersistance.persistPulls(
                        List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), projectPull)),
                HttpStatus.OK);
    }

    @GetMapping(value = "/repos/pulls")
    @ResponseStatus(HttpStatus.OK)
    public Object baseQueryBunchPulls(@RequestBody List<Project> repos) {
        Map<Project, List<ProjectPull>> pulls =
                queryProvider(repos, "repos/%s/%s/pulls", new TypeReference<>() {
                });
        return new ResponseEntity<>(dataPersistance.persistPulls(repos, pulls), HttpStatus.OK);
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
                basicQueryService.retrieveMetrics(
                        "%s" + String.format("repos/%s/%s/issues", owner, repo) + "%s" + "?state=closed",
                        new TypeReference<>() {
                        },
                        "?",
                        "?",
                        true);
        return projectIssues.size();
    }

    @GetMapping(value = "/repos/fixtime")
    @ResponseStatus(HttpStatus.OK)
    public Object baseQueryBunchResolutionTime(@RequestBody List<Project> repos) {
        Map<Project, List<ProjectIssue>> pulls =
                queryProvider(repos, "repos/%s/%s/issues", new TypeReference<>() {
                });
        return new ResponseEntity<>(dataPersistance.persistIssues(repos, pulls), HttpStatus.OK);
    }

    private <T> Map<Project, T> queryProvider(
            @RequestBody List<Project> repos, String url, TypeReference<? extends T> targetType) {
        Map<Project, T> pulls = new HashMap<>();
        for (Project project : repos) {
            pulls.put(
                    project,
                    basicQueryService.retrieveMetrics(
                            "%s"
                                    + String.format(url, project.getProjectOwner(), project.getProjectName())
                                    + "%s&state=closed",
                            targetType,
                            "?",
                            "?",
                            false));
        }
        return pulls;
    }
}
