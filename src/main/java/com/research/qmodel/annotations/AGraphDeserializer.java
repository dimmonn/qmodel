package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.repos.AGraphRepository;
import com.research.qmodel.service.BasicQueryService;
import java.util.Objects;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class AGraphDeserializer extends JsonDeserializer<AGraph> {
  private final BasicQueryService basicQueryService;
  private final ObjectMapper objectMapper;
  private final Logger LOGGER = LoggerFactory.getLogger(AGraphDeserializer.class);
  private final AGraphRepository aGraphRepository;

  public AGraphDeserializer(
      BasicQueryService basicQueryService,
      ObjectMapper objectMapper,
      AGraphRepository aGraphRepository) {
    this.basicQueryService = basicQueryService;
    this.objectMapper = objectMapper;
    this.aGraphRepository = aGraphRepository;
  }

  @SneakyThrows
  @Override
  public AGraph deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    if (node.isEmpty()) {
      LOGGER.warn("AGraph JSON is empty");
      return new AGraph();
    }
    AGraph aGraph = null;
    JsonNode url = node.get(0).get("url");
    if (url != null && url.isTextual()) {
      String[] parts = url.asText().split("/");
      Optional<AGraph> foundGraph =
          aGraphRepository.findByRepoOwnerAndRepoProjectName(parts[4], parts[5]);
      aGraph = foundGraph.orElseGet(AGraph::new);
    }
    if (aGraph == null) {
      return null;
    }
    aGraph.setGraph(node.toString());
    for (JsonNode rawCommit : node) {
      if (rawCommit.get("commit") != null
          && rawCommit.get("commit").get("author") != null
          && rawCommit.get("commit").get("author").get("date") != null
          && rawCommit.get("url") != null) {
        Commit commit = objectMapper.convertValue(rawCommit.get("commit"), Commit.class);
        objectMapper.enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY);
        JsonNode sha = rawCommit.get("sha");
        if (sha != null) {
          commit.setSha(sha.asText());
          commit.setRawData(rawCommit.toString());
        }

        SimpleModule module = new SimpleModule();
        module.addDeserializer(List.class, new FileChangesDeserializer(basicQueryService));
        objectMapper.registerModule(module);
        List<FileChange> fileChanges =
            objectMapper.convertValue(rawCommit, new TypeReference<>() {});
        if (fileChanges == null) {
          LOGGER.error("Failed to pull out files for commit {}, retrying", rawCommit);
          fileChanges = objectMapper.convertValue(rawCommit, new TypeReference<>() {});
          if (fileChanges == null) {
            LOGGER.error("Failed to pull out files for commit {}", rawCommit);
            continue;
          }
        }
        List<FileChange> files = fileChanges.stream().filter(Objects::nonNull).toList();
        commit.setNumOfFilesChanged(fileChanges.size());
        aGraph.addCommit(commit);
        commit.setFileChanges(fileChanges);
        for (FileChange fileChange : files) {
          fileChange.addCommit(commit);
        }
      }
    }
    return aGraph;
  }
}
