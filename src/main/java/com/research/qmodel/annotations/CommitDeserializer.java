package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.Commit;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class CommitDeserializer extends JsonDeserializer<Commit> {
    @Override
    public Commit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Commit commit = new Commit();
        if (node != null) {
            JsonNode author = node.get("author");
            JsonNode commentCount = node.get("comment_count");
            if (commentCount != null) {
                commit.setCommentCount(commentCount.asInt());
            }
            JsonNode message = node.get("message");
            if (message != null) {
                commit.setMessage(message.asText());
            }
            if (author != null) {
                JsonNode rowDate = author.get("date");
                commit.setAuthor(author.get("name").asText());
                commit.setEmail(author.get("email").asText());
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