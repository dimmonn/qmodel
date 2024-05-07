package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitType;
import com.research.qmodel.model.GITHUB_DEFINITION;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import static com.research.qmodel.model.CommitType.MERGE;
import static com.research.qmodel.model.CommitType.REGULAR;

public class CommitDeserializer extends JsonDeserializer<Commit> {
    @Override
    public Commit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node != null) {
            Commit commit = new Commit();
            JsonNode author = node.get("author");
            List<JsonNode> parents = node.findValues("parents");
            if (parents != null) {
                for (JsonNode parent : parents) {
                    JsonNode parentSha = parent.get("sha");
                    if (parentSha != null) {
                        Commit parentCommit = new Commit();
                        parentCommit.setSha(parentSha.asText());
                        commit.getParents().add(parentCommit);
                    }
                }

            }
            JsonNode tree = node.get("tree");
            JsonNode commentCount = node.get("comment_count");
            if (commentCount != null) {
                commit.setCommentCount(commentCount.asInt());
            }
            JsonNode message = node.get("message");
            if (message != null) {
                commit.setMessage(message.asText());
                if (message.asText().contains(GITHUB_DEFINITION.MERGE.getValue())) {
                    commit.setCommitType(MERGE);
                } else {
                    commit.setCommitType(REGULAR);
                }
            }
            if (tree != null) {
                String sha = tree.get("sha") != null ? tree.get("sha").asText() : null;
                commit.setSha(sha);
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
            return commit;
        }
        return null;
    }
}