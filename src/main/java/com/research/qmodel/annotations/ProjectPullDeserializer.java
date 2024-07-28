package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.dto.Project;
import com.research.qmodel.model.*;
import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.repos.ProjectPullRepository;
import com.research.qmodel.service.BasicQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProjectPullDeserializer extends JsonDeserializer<ProjectPull> implements FilterUrl {
    private final Logger LOGGER = LoggerFactory.getLogger(ProjectPullDeserializer.class);
    @Autowired
    private ProjectPullRepository projectPullRepository;
    @Autowired
    private ProjectIssueRepository projectIssueRepository;
    @Autowired
    private BasicQueryService basicQueryService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ProjectPull deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        ProjectPull projectPull = null;
        if (node.get("number") != null && node.get("url") != null) {
            Project project = parseToProject(node.get("url").asText());
//            if (project != null) {
//                Optional<ProjectPull> foundPull = projectPullRepository.findById(new PullID(project.projectOwner(), project.projectName(), node.get("number").asLong()));
//                if (foundPull.isPresent()) {
//                    return foundPull.get();
//                }
//            }
        }
        if (projectPull == null) {
            projectPull = new ProjectPull();
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
                projectPull.setMerged_at(dateFormat.parse(node.get("merged_at").asText()));
            } catch (ParseException e) {
                LOGGER.error("Ignoring merged_at " + e.getMessage());
            }
        }
        projectPull.setRawPull(node.toString());
        projectPull.setState(node.path("state").asText());

        JsonNode rowData = basicQueryService.getRowData(node.path("issue_url").asText());

        if (rowData.get("labels") != null) {
            Set<Map> labels = new ObjectMapper().convertValue(node.get("labels"), HashSet.class);
            Set<String> rawLabels = labels.stream().map(
                    e -> e.get("name").toString()
            ).collect(Collectors.toSet());
            projectPull.setLabels(rawLabels);
        }

        String owner = rowData.path("url").asText().split("/")[4];
        String projectName = rowData.path("url").asText().split("/")[5];
        projectPull.setProjectOwner(owner);
        projectPull.setProjectName(projectName);
        if (rowData.get("reactions") != null && (node.path("reactions").path("total_count").asInt() != 0 ||
                node.path("reactions").path("+1").asInt() != 0 ||
                node.path("reactions").path("-1").asInt() != 0 ||
                node.path("reactions").path("laugh").asInt() != 0 ||
                node.path("reactions").path("hooray").asInt() != 0 ||
                node.path("reactions").path("confused").asInt() != 0 ||
                node.path("reactions").path("heart").asInt() != 0 ||
                node.path("reactions").path("rocket").asInt() != 0 ||
                node.path("reactions").path("eyes").asInt() != 0)) {
            Reaction reaction = new Reaction();
            reaction.setUrl(node.path("reactions").path("url").asText());
            reaction.setTotalCount(node.path("reactions").path("total_count").asInt());
            reaction.setTotalCount(node.path("reactions").path("+1").asInt());
            reaction.setTotalCount(node.path("reactions").path("-1").asInt());
            reaction.setTotalCount(node.path("reactions").path("laugh").asInt());
            reaction.setTotalCount(node.path("reactions").path("hooray").asInt());
            reaction.setTotalCount(node.path("reactions").path("confused").asInt());
            reaction.setTotalCount(node.path("reactions").path("heart").asInt());
            reaction.setTotalCount(node.path("reactions").path("rocket").asInt());
            reaction.setTotalCount(node.path("reactions").path("eyes").asInt());
            projectPull.setReaction(reaction);
        }


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


        return projectPull;
    }
}
