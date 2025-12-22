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

import static com.research.qmodel.dto.PARSING_PROPERTIES.*;

public class FileChangesDeserializer extends JsonDeserializer<List<FileChange>> implements ChangePatchProcessor {
    private final Logger LOGGER = LoggerFactory.getLogger(FileChangesDeserializer.class);
    private final BasicQueryService basicQueryService;

    public FileChangesDeserializer(BasicQueryService basicQueryService) {
        this.basicQueryService = basicQueryService;
    }

    @SneakyThrows
    @Override
    public List<FileChange> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode rowData = basicQueryService.getRowDataWithCursor(node.get("url").asText());
        if (rowData == null) {
            return null;
        }
        LOGGER.info("Getting commit details {}", node.get("url").asText());
        JsonNode modifiedFiles = rowData.get(FILES.key());
        if (modifiedFiles != null) {
            List<FileChange> result = new ArrayList<>();
            for (JsonNode file : modifiedFiles) {
                int additions = file.get(ADDITIONS.key()) != null ? file.get(ADDITIONS.key()).asInt() : 0;
                int deletions = file.get(DELETIONS.key()) != null ? file.get(DELETIONS.key()).asInt() : 0;
                int changes = file.get(CHANGES.key()) != null ? file.get(CHANGES.key()).asInt() : 0;
                String filename = file.get(FILENAME.key()) != null ? file.get(FILENAME.key()).asText() : null;
                String status = file.path(STATUS.key()).asText();
                JsonNode commit = node.get(COMMIT.key());
                String patch = file.path(PATCH.key()).asText();
                Set<Integer> changedLineNumbers = getChangedLineNumbers(patch);
                String sha = file.path(SHA.key()).asText();
                JsonNode rowDate = commit.path(AUTHOR.key()).path(DATE.key());
                DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
                Instant instant = Instant.from(formatter.parse(rowDate.asText()));
                Date date = Date.from(instant);
                result.add(new FileChange(null, status, sha, date, null, additions, deletions, changes, filename, patch, changedLineNumbers, rowData.toString()));
            }
            LOGGER.info("File Change is discovered");
            return result;
        }
        return null;
    }

}
