package com.research.qmodel.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.graph.Graph;
import com.research.qmodel.graph.Vertex;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.model.ProjectPull;
import com.research.qmodel.service.BasicQueryService;
import com.research.qmodel.service.DataPersistance;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"api"})
public class GraphController implements FileJsonReader {
  @Autowired private Graph gitGraph;

  @GetMapping(value = "/health")
  @ResponseStatus(HttpStatus.OK)
  public void health() {}

  @GetMapping(value = "/graph/fields")
  @ResponseStatus(HttpStatus.OK)
  public String getFields() {
    return readJsonFile("/Users/dpolishchuk/IdeaProjects/qmodel/graph_fields.json");
  }

  @GetMapping(value = "/graph/data")
  @ResponseStatus(HttpStatus.OK)
  public String getData(
      @RequestParam(required = false) String owner, @RequestParam(required = false) String repo)
      throws GitAPIException, IOException {
    String path = "/Users/dpolishchuk/" + owner + "_" + repo;
    gitGraph.cloneRepo(owner, repo, path);
    Graph graph = gitGraph.buildGraph(owner, repo, path);
    String filePath = "/Users/dpolishchuk/IdeaProjects/qmodel/" + owner + "_" + repo + ".json";
    gitGraph.exportGraph(filePath, graph.getVerticesMap());
    Graph b = new Graph();
    Map<String, Vertex> verticesMap = b.getVerticesMap();
    return readJsonFile("/Users/dpolishchuk/IdeaProjects/qmodel/" + owner + "_" + repo + ".json");
  }
}
