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
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
public class ProjectIssueDeserializer extends JsonDeserializer<ProjectIssue> implements FilterUrl {
    private final Logger LOGGER = LoggerFactory.getLogger(ProjectIssueDeserializer.class);
    @Autowired
    private ProjectIssueRepository projectIssueRepository;
    @Autowired
    private BasicQueryService basicQueryService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProjectPullRepository projectPullRepository;

    @Override
    public ProjectIssue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String owner = node.path("url").asText().split("/")[4];
        String projectName = node.path("url").asText().split("/")[5];
        if (node.path("html_url").asText().split("/")[5].equals("pull")) {
            return null;
        }
        ProjectIssue projectIssue = null;
        if (node.get("number") != null && node.get("url") != null) {
            Project project = parseToProject(node.get("url").asText());
            if (project != null) {
                //TODO improve performance
//                Optional<ProjectIssue> foundIssue = projectIssueRepository.findById(new IssueID(owner, projectName, node.get("number").asLong()));
//                if (foundIssue.isPresent()) {
//                    return foundIssue.get();
//                }
            }
        }
        if (projectIssue == null) {
            projectIssue = new ProjectIssue();
        }
        projectIssue.setProjectOwner(owner);
        projectIssue.setProjectName(projectName);
        List<Timeline> timelines;
        JsonNode timelineUrl = node.get("timeline_url");
        if (timelineUrl != null) {
            if (projectIssue.getProjectPull() == null) {

                JsonNode timeLineRaw = basicQueryService.getRowData(timelineUrl.asText());
                timelines = objectMapper.convertValue(timeLineRaw, new TypeReference<>() {
                });

                if (timelines != null && !timelines.isEmpty()) {
                    for (Timeline timeline : timelines) {
                        if (timeline == null) {
                            LOGGER.warn("Timeline without node_id.");
                        } else {
                            timeline.setProjectIssue(projectIssue);
                            projectIssue.addTimeLine(timeline);
                            if (timeline.getPullIds() != null) {
                                for (Long pullId : timeline.getPullIds()) {
                                    Optional<ProjectPull> foundPull = projectPullRepository.findById(new PullID(owner, projectName, pullId));
                                    if (foundPull.isPresent()) {
                                        projectIssue.addProjectPull(foundPull.get());
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        if (node.get("title") != null) {
            projectIssue.setTitle(node.get("title").asText().substring(0, Math.min(node.get("title").asText().length(), 40)));
        }

        if (node.get("reactions") != null&& (node.path("reactions").path("total_count").asInt() != 0 ||
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
            projectIssue.setReaction(reaction);
        }

        if (node.get("number") != null) {
            projectIssue.setState(node.get("state").asText());
        }
        if (node.get("labels") != null) {
            Set<Map> labels = new ObjectMapper().convertValue(node.get("labels"), HashSet.class);
            Set<String> rawLabels = labels.stream().map(
                    e -> e.get("name").toString()
            ).collect(Collectors.toSet());
            projectIssue.setLabels(rawLabels);
        }
        if (node.get("number") != null) {
            projectIssue.setId(node.get("number").asLong());
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
        projectIssue.setRawIssue(node.toString());

        JsonNode htmlIssue = node.get("html_url");
        if (htmlIssue != null) {
            try {

                String htmlIssueData = basicQueryService.getHtmlData(htmlIssue.asText());


                Document doc = Jsoup.parse(htmlIssueData);

                // Parse the HTML to find the specific element containing the pull request URL
                Elements spans = doc.select("span[data-issue-and-pr-hovercards-enabled]");
                for (Element span : spans) {
                    Elements links = span.select("a[data-hovercard-type=pull_request]");
                    for (Element link : links) {
                        String prUrl = link.attr("href");
                        String fixPR = StringUtils.substringAfterLast(prUrl, "/");
                        projectIssue.setFixPrNum(Long.parseLong(fixPR));
                        Optional<ProjectPull> foundPull = projectPullRepository.findById(new PullID(owner, projectName, Long.parseLong(fixPR)));
                        if (foundPull.isPresent()) {
                            ProjectPull fix = foundPull.get();
                            fix.setBugFix(true);
                            projectIssue.setFixPR(fix);
                        }

                    }
                }
            }catch (Exception e){
                LOGGER.error(e.getMessage());
            }

        }
        return projectIssue;
    }
}
