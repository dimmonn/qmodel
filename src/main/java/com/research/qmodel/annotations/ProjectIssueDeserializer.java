package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.model.*;
import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.repos.ProjectPullRepository;
import com.research.qmodel.repos.ProjectRepository;
import com.research.qmodel.service.BasicQueryService;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class ProjectIssueDeserializer extends JsonDeserializer<ProjectIssue>
    implements FilterUrl, BaseReactionBuilder {
  private final Logger LOGGER = LoggerFactory.getLogger(ProjectIssueDeserializer.class);
  private final BasicQueryService basicQueryService;
  private final ObjectMapper objectMapper;
  private final ProjectPullRepository projectPullRepository;
  private final ProjectIssueRepository projectIssueRepository;
  private final ProjectRepository projectRepository;

  @Value("${issue.reference.keywords}")
  private final List<String> keywords;

  @Override
  public ProjectIssue deserialize(
      JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    String owner = node.path("url").asText().split("/")[4];
    String projectName = node.path("url").asText().split("/")[5];

    if (node.path("html_url").asText().split("/")[5].equals("pull")) {
      return null;
    }
    ProjectIssue projectIssue = new ProjectIssue();
    projectIssue.setProjectOwner(owner);
    projectIssue.setProjectName(projectName);
    ProjectIssue foundIssue =
        projectIssueRepository.findIssueById(owner, projectName, node.get("number").asLong());
    if (foundIssue != null) {
      return foundIssue;
    }
    if (StringUtils.isNotBlank(node.path("assignees").toString())
        && !node.path("assignees").isNull()
        && !node.path("assignees").toString().equals("[]")) {
      for (JsonNode assignee : node.path("assignees")) {
        projectIssue.addAssignees(assignee.path("login").asText());
      }
    }

    List<Timeline> timelines;
    JsonNode timelineUrl = node.get("timeline_url");
    if (StringUtils.isNotEmpty(node.path("pull_request").asText())) {
      LOGGER.warn("Skipping pull request {}", node.path("html_url").asText());
      return null;
    }
    if (timelineUrl != null) {
      if (projectIssue.getProjectPull() == null) {

        JsonNode timeLineRaw = basicQueryService.getRowData(timelineUrl.asText());
        timelines = objectMapper.convertValue(timeLineRaw, new TypeReference<>() {});

        if (timelines != null && !timelines.isEmpty()) {
          for (Timeline timeline : timelines) {
            if (timeline == null) {
              LOGGER.warn("Timeline without node_id.");
            } else {
              timeline.setProjectIssue(projectIssue);
              projectIssue.addTimeLine(timeline);
              if (timeline.getPullIds() != null
                  && new ObjectMapper()
                      .readTree(timeline.getRawData())
                      .path("source")
                      .path("issue")
                      .path("timeline_url")
                      .asText()
                      .contains("/" + owner + "/" + projectName)) {
                for (Long pullId : timeline.getPullIds()) {
                  Optional<ProjectPull> foundPull =
                      projectPullRepository.findById(new PullID(owner, projectName, pullId));
                  foundPull.ifPresent(projectIssue::addProjectPull);
                }
              }
            }
          }
        }
      }
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    if (node.get("title") != null) {
      projectIssue.setTitle(
          node.get("title")
              .asText()
              .substring(0, Math.min(node.get("title").asText().length(), 40)));
    }

    setupReaction(node, node, projectIssue);

    if (node.get("number") != null) {
      projectIssue.setState(node.get("state").asText());
    }
    if (node.get("labels") != null) {
      Set<Map<String, String>> labels =
          new ObjectMapper().convertValue(node.path("labels"), new TypeReference<>() {});
      Set<String> rawLabels = labels.stream().map(e -> e.get("name")).collect(Collectors.toSet());
      projectIssue.setLabels(rawLabels);
    }
    if (node.get("number") != null) {
      projectIssue.setId(node.get("number").asLong());
    }
    if (node.get("created_at") != null && !node.get("created_at").asText().equals("null")) {
      try {
        projectIssue.setCreated_at(dateFormat.parse(node.get("created_at").asText()));
      } catch (ParseException e) {
        LOGGER.error("Ignoring created_at {}", e.getMessage());
      }
    }
    if (node.get("pull_request") != null
        && !node.get("pull_request").get("merged_at").asText().equals("null")) {
      try {
        projectIssue.setMerged_at(
            dateFormat.parse(node.get("pull_request").get("merged_at").asText()));
      } catch (ParseException e) {
        LOGGER.error("Ignoring merged_at {}", e.getMessage());
      }
    }
    if (node.get("closed_at") != null && !node.get("closed_at").asText().equals("null")) {
      try {
        projectIssue.setClosed_at(dateFormat.parse(node.get("closed_at").asText()));
      } catch (ParseException e) {
        LOGGER.error("Ignoring closed_at {}", e.getMessage());
      }
    }
    if (node.get("updated_at") != null && !node.get("updated_at").asText().equals("null")) {
      try {
        projectIssue.setUpdated_at(dateFormat.parse(node.get("updated_at").asText()));
      } catch (ParseException e) {
        LOGGER.error("Ignoring updated_at {}", e.getMessage());
      }
    }
    projectIssue.setRawIssue(node.toString());

    JsonNode htmlIssue = node.get("html_url");
    if (htmlIssue != null) {
      try {

        String htmlIssueData = basicQueryService.getHtmlData(htmlIssue.asText());
        Document doc = Jsoup.parse(htmlIssueData);
        Elements spans = doc.select("span");
        for (Element span : spans) {
          String spanText = span.text();
          if (spanText.startsWith("#")
              && span.parent() != null
              && StringUtils.isBlank(span.parent().text().split("#")[0])) {
            String prIdStr = spanText.substring(1);
            long fixPR = Long.parseLong(prIdStr);
            if (projectIssue.getId() != fixPR) {
              projectIssue.setFixPrNum(fixPR);

              Optional<ProjectPull> foundPull =
                  projectPullRepository.findById(new PullID(owner, projectName, fixPR));
              if (foundPull.isPresent()) {
                ProjectPull fix = foundPull.get();
                fix.setBugFix(true);
                projectIssue.setFixPR(fix);
              }
            }
          }
        }
      } catch (Exception e) {
        LOGGER.error(e.getMessage());
      }
    }

    if (projectIssue.getFixPR() == null && projectIssue.getProjectPull() != null) {
      Pattern pattern =
          Pattern.compile(
              "\\b(" + String.join("|", keywords) + ")\\s+(?:issue\\s*)?#?(\\d+)\\b",
              Pattern.CASE_INSENSITIVE);

      projectIssue.getProjectPull().stream()
          .filter(Objects::nonNull)
          .forEach(
              p -> {
                Set<Long> issueIds =
                    p.getTimeLine().stream()
                        .map(Timeline::getMessage)
                        .filter(Objects::nonNull)
                        .flatMap(
                            msg -> {
                              Matcher matcher = pattern.matcher(msg);
                              return matcher
                                  .results()
                                  .map(
                                      m ->
                                          Long.valueOf(
                                              m.group(
                                                  2))); // Java 9+ Matcher.results() for efficiency
                            })
                        .collect(Collectors.toSet()); // Use Set to avoid duplicate DB calls

                List<ProjectIssue> issues =
                    projectIssueRepository.findIssuesByIds(
                        p.getProjectName(), p.getProjectOwner(), issueIds);

                issues.forEach(
                    issue -> {
                      issue.setFixPR(p);
                      issue.setPrThatFixesIssue(p.getId());
                    });
              });
    }

    Optional<Project> foundProject = projectRepository.findById(new ProjectID(owner, projectName));
    Project project = foundProject.orElseGet(() -> new Project(owner, projectName));
    project.addProjectIssue(projectIssue);
    projectRepository.save(project);

    return projectIssue;
  }
}
