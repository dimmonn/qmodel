package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.Timeline;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TimelineDeserializer extends JsonDeserializer<Timeline> implements FilterUrl {
  private final Logger LOGGER = LoggerFactory.getLogger(TimelineDeserializer.class);

  @Override
  public Timeline deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    if (node == null) {
      return null;
    }

    // TODO fix with sha
    //        Optional<Timeline> foundTimeline = timelineRepository.findByRawData(node.toString());
    //        if (foundTimeline.isPresent()) {
    //            return foundTimeline.get();
    //        } else {
    //            timeline = new Timeline();
    //            timeline.setRawData(node.toString());
    //        }
    Timeline timeline = new Timeline();
    timeline.setRawData(node.toString());
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    try {
      timeline.setCreatedAt(dateFormat.parse(node.path("created_at").asText()));
    } catch (ParseException e) {
      LOGGER.error(e.getMessage());
    }

    if (node.get("message") != null) {
      timeline.setMessage(node.get("message").asText());
    }
    if (node.get("pull_request_url") != null) {
      Long timelineId = parseToID(node.get("pull_request_url").asText());
      timeline.addPullId(timelineId);
    }
    String prUrl = node.path("source").path("issue").path("pull_request").path("url").asText();
    if (prUrl != null && !prUrl.isEmpty()) {
      Long timelineId = parseToID(prUrl);
      timeline.addPullId(timelineId);
    }
    return timeline;
  }
}
