package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.repos.CommitRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

@Component
public class CommitDeserializer extends JsonDeserializer<Commit> {
  private final CommitRepository commitRepository;

  public CommitDeserializer(CommitRepository commitRepository) {
    this.commitRepository = commitRepository;
  }

  @Override
  public Commit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    if (node != null) {
      Optional<Commit> foundCommit =
          commitRepository.findById(
              new CommitID(StringUtils.substringAfterLast(node.get("url").asText(), "/")));
      Commit commit;
      commit = foundCommit.orElseGet(Commit::new);
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
        commit.setAuthor(author.path("name").asText());
        commit.setEmail(author.path("email").asText());
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
