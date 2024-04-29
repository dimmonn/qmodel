package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.ProjectPull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ProjectPullDeserializer extends JsonDeserializer<ProjectPull> {
    private final Logger LOGGER = LoggerFactory.getLogger(ProjectPullDeserializer.class);

    @Override
    public ProjectPull deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        ProjectPull projectPull = new ProjectPull();
        if (node.get("title") != null) {
            projectPull.setTitle(node.get("title").asText());
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        if (node.get("created_at") != null) {
            try {
                projectPull.setCreated_at(dateFormat.parse(node.get("created_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring created_at " + e.getMessage());
            }
        }
        if (node.get("closed_at") != null) {
            try {
                projectPull.setClosed_at(dateFormat.parse(node.get("closed_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring closed_at " + e.getMessage());
            }
        }
        if (node.get("updated_at") != null) {
            try {
                projectPull.setUpdated_at(dateFormat.parse(node.get("updated_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring updated_at " + e.getMessage());
            }
        }
        if (node.get("merged_at") != null) {
            try {
                projectPull.setUpdated_at(dateFormat.parse(node.get("merged_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring merged_at " + e.getMessage());
            }
        }
        projectPull.setPull(node.toString());
        return projectPull;
    }
}
