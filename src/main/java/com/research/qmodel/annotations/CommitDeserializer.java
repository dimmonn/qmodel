package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CommitDeserializer extends JsonDeserializer {
    @Override
    public Commit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Commit commit = new Commit();
        JsonNode rowCommit = node.get("commit");
        if (rowCommit != null) {
            JsonNode author = rowCommit.get("author");
            if (author != null) {
                JsonNode rowDate = author.get("date");
                if (rowDate != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
                    Instant instant = Instant.from(formatter.parse(rowDate.asText()));
                    Date date = Date.from(instant);
                    commit.setCommitDate(date);
                }
            }
        }
        return commit;
    }
}