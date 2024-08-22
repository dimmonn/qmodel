package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.service.BasicQueryService;
import java.util.Set;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileChangesDeserializer extends JsonDeserializer<List<FileChange>> implements ChangePatchProcessor{
    private final Logger LOGGER = LoggerFactory.getLogger(FileChangesDeserializer.class);
    private final BasicQueryService basicQueryService;

    public FileChangesDeserializer(BasicQueryService basicQueryService) {
        this.basicQueryService = basicQueryService;
    }

    @SneakyThrows
    @Override
    public List<FileChange> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode rowData = basicQueryService.getRowData(node.get("url").asText());
        if (rowData == null) {
            return null;
        }
        LOGGER.info("Getting commit details {}", node.get("url").asText());
        JsonNode modifiedFiles = rowData.get("files");
        if (modifiedFiles != null) {
            List<FileChange> result = new ArrayList<>();
            for (JsonNode file : modifiedFiles) {
                int additions = file.get("additions") != null ? file.get("additions").asInt() : 0;
                int deletions = file.get("deletions") != null ? file.get("deletions").asInt() : 0;
                int changes = file.get("changes") != null ? file.get("changes").asInt() : 0;
                String filename = file.get("filename") != null ? file.get("filename").asText() : null;
                String status = file.path("status").asText();
                JsonNode commit = node.get("commit");
                String patch = file.path("patch").asText();
              Set<Integer> changedLineNumbers = getChangedLineNumbers(patch);
              String sha = file.path("sha").asText();
                Date date = null;
                if (commit != null) {
                    JsonNode author = commit.get("author");
                    if (author != null) {
                        JsonNode rowDate = author.get("date");
                        if (rowDate != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
                            Instant instant = Instant.from(formatter.parse(rowDate.asText()));
                            date = Date.from(instant);
                        }
                    }
                }
                result.add(new FileChange(null,status, sha, date, null, additions, deletions, changes, filename, patch, changedLineNumbers, rowData.toString()));
            }
            return result;
        }
        return null;
    }

}
