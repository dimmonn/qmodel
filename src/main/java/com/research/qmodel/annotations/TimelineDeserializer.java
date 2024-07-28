package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.Timeline;
import com.research.qmodel.repos.TimelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class TimelineDeserializer extends JsonDeserializer<Timeline> implements FilterUrl {
    @Autowired
    private TimelineRepository timelineRepository;
    private final Logger LOGGER = LoggerFactory.getLogger(TimelineDeserializer.class);

    @Override
    public Timeline deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
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
