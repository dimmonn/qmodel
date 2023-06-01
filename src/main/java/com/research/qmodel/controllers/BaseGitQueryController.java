package com.research.qmodel.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectPull;
import com.research.qmodel.service.BasicQueryService;
import com.research.qmodel.service.DataPersistance;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.*;


@RestController
@RequestMapping(value = {"api/v1/metrics"})
public class BaseGitQueryController {
    private final BasicQueryService basicQueryService;
    private final DataPersistance dataPersistance;

    public BaseGitQueryController(BasicQueryService basicQueryService, DataPersistance dataPersistance) {
        this.basicQueryService = basicQueryService;
        this.dataPersistance = dataPersistance;
    }

    @GetMapping(value = "/repos/{owner}/{repo}")
    @ResponseStatus(HttpStatus.OK)
    public Object baseQueryAg(@PathVariable(value = "owner")
                              @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
                                      String owner, @PathVariable(value = "repo")
                              @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
                                      String repo) throws URISyntaxException {
        AGraph ag = basicQueryService.retrievemetrics("%s" + String.format("repos/%s/%s/commits", owner, repo) + "%s", new TypeReference<>() {
        });
        return new ResponseEntity(dataPersistance.persistGraph(List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), ag)), HttpStatus.OK);
    }

    @GetMapping(value = "/repos/ag")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<JsonNode> baseQueryBunchAg(@RequestBody List<Project> repos) throws URISyntaxException {
        Map<Project, AGraph> ags = queryProvider(repos, "repos/%s/%s/commits/", new TypeReference<>() {
        });
        return new ResponseEntity(dataPersistance.persistGraph(repos, ags), HttpStatus.OK);
    }

    @GetMapping(value = "/repos/{owner}/{repo}/pulls")
    @ResponseStatus(HttpStatus.OK)
    public Object baseQueryPull(@PathVariable(value = "owner")
                                @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
                                        String owner, @PathVariable(value = "repo")
                                @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
                                        String repo) throws URISyntaxException {
        List<ProjectPull> projectPull = basicQueryService.retrievemetrics("%s" + String.format("repos/%s/%s/pulls", owner, repo) + "%s&state=closed", new TypeReference<>() {
        });
        return new ResponseEntity(dataPersistance.persistPulls(List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), projectPull)), HttpStatus.OK);
    }

    @GetMapping(value = "/repos/pulls")
    @ResponseStatus(HttpStatus.OK)
    public Object baseQueryBunchPulls(@RequestBody List<Project> repos) throws URISyntaxException {
        Map<Project, List<ProjectPull>> pulls = queryProvider(repos, "%s/repos/%s/%s/pulls%s&state=closed", new TypeReference<>() {
        });
        return new ResponseEntity(dataPersistance.persistPulls(repos, pulls), HttpStatus.OK);
    }

    @GetMapping(value = "/repos/{owner}/{repo}/issues")
    @ResponseStatus(HttpStatus.OK)
    public Object baseQueryIssueResolutionTime(@PathVariable(value = "owner")
                                               @Parameter(name = "owner", in = ParameterIn.PATH, description = "Owner of the project")
                                                       String owner, @PathVariable(value = "repo")
                                               @Parameter(name = "repo", in = ParameterIn.PATH, description = "Repo name")
                                                       String repo) throws URISyntaxException {
        List<ProjectIssue> projectIssues = basicQueryService.retrievemetrics("%s" + String.format("repos/%s/%s/issues", owner, repo) + "%s" + "&state=closed", new TypeReference<>() {
        });
        return new ResponseEntity(dataPersistance.persistIssues(List.of(new Project(owner, repo)), Map.of(new Project(owner, repo), projectIssues)), HttpStatus.OK);
    }

    @GetMapping(value = "/repos/fixtime")
    @ResponseStatus(HttpStatus.OK)
    public Object baseQueryBunchResolutionTime(@RequestBody List<Project> repos) throws URISyntaxException {
        Map<Project, List<ProjectIssue>> pulls = queryProvider(repos, "%s/repos/%s/%s/issues%s&state=closed", new TypeReference<>() {
        });
        return new ResponseEntity(dataPersistance.persistIssues(repos, pulls), HttpStatus.OK);
    }

    private <T> Map<Project, T> queryProvider(@RequestBody List<Project> repos, String url, TypeReference<? extends T> targetType) throws URISyntaxException {
        Map<Project, T> pulls = new HashMap<>();
        for (Project project : repos) {
            pulls.put(project, basicQueryService.retrievemetrics("%s/" + String.format(url, project.getOwner(), project.getProjectName()) + "%s&state=closed", targetType));
        }
        return pulls;
    }
}
