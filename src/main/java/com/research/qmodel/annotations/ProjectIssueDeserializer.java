package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.model.ProjectIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ProjectIssueDeserializer extends JsonDeserializer<ProjectIssue> {
    private final Logger LOGGER = LoggerFactory.getLogger(ProjectIssueDeserializer.class);

    @Override
    public ProjectIssue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        ProjectIssue projectIssue = new ProjectIssue();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        if (node.get("title") != null) {
            projectIssue.setTitle(node.get("title").asText().substring(0, Math.min(node.get("title").asText().length(), 40)));
        }
        if (node.get("created_at") != null && !node.get("created_at").asText().equals("null")) {
            try {
                projectIssue.setCreated_at(dateFormat.parse(node.get("created_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring created_at " + e.getMessage());
            }
        }
        if (node.get("pull_request") != null && !node.get("pull_request").get("merged_at").asText().equals("null")) {
            try {
                projectIssue.setMerged_at(dateFormat.parse(node.get("pull_request").get("merged_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring merged_at " + e.getMessage());
            }
        }
        if (node.get("closed_at") != null && !node.get("closed_at").asText().equals("null")) {
            try {
                projectIssue.setClosed_at(dateFormat.parse(node.get("closed_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring closed_at " + e.getMessage());
            }
        }
        if (node.get("updated_at") != null && !node.get("updated_at").asText().equals("null")) {
            try {
                projectIssue.setUpdated_at(dateFormat.parse(node.get("updated_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring updated_at " + e.getMessage());
            }
        }
        projectIssue.setIssue(node.toString());
        return projectIssue;
    }
}
