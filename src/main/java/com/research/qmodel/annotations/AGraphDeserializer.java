package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.service.BasicQueryService;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;

@Component
public class AGraphDeserializer extends JsonDeserializer<AGraph> {
    private BasicQueryService basicQueryService;
    private final ObjectMapper objectMapper;
    private final Logger LOGGER = LoggerFactory.getLogger(AGraphDeserializer.class);

    public AGraphDeserializer(BasicQueryService basicQueryService, ObjectMapper objectMapper) {
        this.basicQueryService = basicQueryService;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public AGraph deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        AGraph aGraph = new AGraph();
        aGraph.setGraph(node.toString());
        for (JsonNode rawCommit : node) {
            if (rawCommit.get("commit") != null && rawCommit.get("commit").get("author") != null && rawCommit.get("commit").get("author").get("date") != null && rawCommit.get("url") != null) {
                Commit commit = objectMapper.convertValue(rawCommit.get("commit"), Commit.class);
                objectMapper.enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY);
                SimpleModule module = new SimpleModule();
                module.addDeserializer(List.class, new FileChangesDeserializer(basicQueryService));
                objectMapper.registerModule(module);
                List<FileChange> fileChanges = objectMapper.convertValue(rawCommit, new TypeReference<>() {
                });
                if (fileChanges == null || fileChanges.isEmpty()) {
                    LOGGER.error("Failed to pull out files for commit " + rawCommit);
                    continue;
                }
                commit.setNumOfFilesChanged(fileChanges.size());
                aGraph.addCoommit(commit);
                if (fileChanges == null) {
                    break;
                }
                commit.setFileChanges(fileChanges);
                for (FileChange fileChange : fileChanges) {
                    fileChange.addCommit(commit);
                }
            }
        }
        return aGraph;
    }
}