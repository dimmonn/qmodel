package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.dto.Project;
import com.research.qmodel.model.*;
import com.research.qmodel.service.BasicQueryService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class ProjectPullDeserializer extends JsonDeserializer<ProjectPull>
        implements FilterUrl, BaseReactionBuilder {
    private final Logger LOGGER = LoggerFactory.getLogger(ProjectPullDeserializer.class);
    private final BasicQueryService basicQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public ProjectPull deserialize(
            JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node.get("number") != null && node.get("url") != null) {
            Project project = parseToProject(node.get("url").asText());
            //            if (project != null) {
            //                Optional<ProjectPull> foundPull = projectPullRepository.findById(new
            // PullID(project.projectOwner(), project.projectName(), node.get("number").asLong()));
            //                if (foundPull.isPresent()) {
            //                    return foundPull.get();
            //                }
            //            }
        }
        ProjectPull projectPull = new ProjectPull();
        if (StringUtils.isNotBlank(node.path("assignees").toString())
                && !node.path("assignees").toString().equals("[]")
                && !node.path("assignees").isNull()) {
            for (JsonNode assignee : node.path("assignees")) {
                projectPull.addAssignee(assignee.path("login").asText());
            }
        }

        String url = node.path("url").asText();
        if (StringUtils.isNotEmpty(url)) {
            url = url + "/reviews";
            JsonNode reviews = basicQueryService.getRowData(url);
            if (reviews != null) {
                for (JsonNode review : reviews) {
                    JsonNode reviewer = review.path("user").path("login");
                    if (reviewer != null && StringUtils.isNotEmpty(reviewer.asText())) {
                        projectPull.addReviewer(reviewer.asText());
                    }
                }
            }
        }

        if (node.get("title") != null) {
            projectPull.setTitle(node.get("title").asText());
        }
        if (node.get("number") != null) {
            projectPull.setId(node.get("number").asLong());
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        if (node.get("created_at") != null) {
            try {
                projectPull.setCreated_at(dateFormat.parse(node.get("created_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring created_at {}", e.getMessage());
            }
        }
        if (node.get("closed_at") != null) {
            try {
                projectPull.setClosed_at(dateFormat.parse(node.get("closed_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring closed_at {}", e.getMessage());
            }
        }
        if (node.get("updated_at") != null) {
            try {
                projectPull.setUpdated_at(dateFormat.parse(node.get("updated_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring updated_at {}", e.getMessage());
            }
        }
        if (node.get("merged_at") != null && !node.get("merged_at").asText().equals("null")) {
            try {
                projectPull.setMerged_at(dateFormat.parse(node.get("merged_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring merged_at {}", e.getMessage());
            }
        }
        projectPull.setRawPull(node.toString());
        projectPull.setState(node.path("state").asText());

        JsonNode rowData = basicQueryService.getRowData(node.path("issue_url").asText());
        if (rowData == null) {
            return null;
        }
        if (rowData.get("labels") != null) {
            Set<Map<String, String>> labels =
                    new ObjectMapper().convertValue(node.get("labels"), new TypeReference<>() {
                    });
            Set<String> rawLabels = labels.stream().map(e -> e.get("name")).collect(Collectors.toSet());
            projectPull.setLabels(rawLabels);
        }

        String owner = rowData.path("url").asText().split("/")[4];
        String projectName = rowData.path("url").asText().split("/")[5];
        projectPull.setProjectOwner(owner);
        projectPull.setProjectName(projectName);
        setupReaction(rowData, node, projectPull);

        List<Timeline> timelines;
        JsonNode rowComments = basicQueryService.getRowData(rowData.path("timeline_url").asText());
        if (rowComments != null) {

            timelines = objectMapper.convertValue(rowComments, new TypeReference<>() {
            });

            if (timelines != null && !timelines.isEmpty()) {

                for (Timeline timeline : timelines) {
                    if (timeline == null) {
                        LOGGER.warn("Timeline without node_id.");
                    } else {
                        timeline.setProjectPull(projectPull);
                        projectPull.addTimeLine(timeline);
                    }
                }
            }
        }

        LOGGER.info(
                "received project pull " + projectPull.getProjectName() + " id: " + projectPull.getId());
        return projectPull;
    }
}
