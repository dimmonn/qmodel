package com.research.qmodel.annotations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.research.qmodel.controllers.ProjectNameCache;
import com.research.qmodel.model.*;
import com.research.qmodel.repos.CommitRepository;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.repos.ProjectPullRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

@Component
public class CommitDeserializer extends JsonDeserializer<Commit> {
    private final CommitRepository commitRepository;
    private final Pattern PATTERN = Pattern.compile(".*/repos/([^/]+)/([^/]+)/.*");
    private final Logger LOGGER = LoggerFactory.getLogger(CommitDeserializer.class);
    private final ProjectNameCache projectNameCache;
    @Autowired
    private ProjectPullRepository projectPullRepository;
    @Autowired
    private ProjectIssueRepository projectIssueRepository;

    public CommitDeserializer(CommitRepository commitRepository, ProjectNameCache projectNameCache) {
        this.commitRepository = commitRepository;
        this.projectNameCache = projectNameCache;
    }

    @Override
    public Commit deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node != null) {
            Optional<Commit> foundCommit =
                    commitRepository.findById(
                            new CommitID(StringUtils.substringAfterLast(node.get("url").asText(), "/")));
            if (foundCommit.isPresent()) {
                JsonNode payload = node.path("verification").path("payload");
                String message = node.path("message").asText();
                if (StringUtils.isNotEmpty(message)) {
                    updateCommitToPr(foundCommit, message);
                }
                String rawPayload = payload.asText();
                if (!rawPayload.isEmpty()) {
                    updateCommitToPr(foundCommit, rawPayload);

                }
                return foundCommit.get();
            }
            Commit commit;
            commit = new Commit();
            JsonNode author = node.get("author");
            JsonNode commentCount = node.get("comment_count");
            if (commentCount != null) {
                commit.setCommentCount(commentCount.asInt());
            }

            commit.setProjectName(projectNameCache.getProjectName());
            commit.setProjectOwner(projectNameCache.getProjectOwner());

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

    private void updateCommitToPr(Optional<Commit> foundCommit, String message) {
        Commit commit = foundCommit.get();
        Pattern pattern = Pattern.compile("#(\\d+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String prNumber = matcher.group(1);
            Optional<ProjectPull> foundPull = projectPullRepository.findById(new PullID(commit.getProjectOwner(), commit.getProjectName(), Long.parseLong(prNumber)));
            if (foundPull.isPresent()) {
                ProjectPull refsPull = foundPull.get();
                refsPull.addCommits(List.of(commit));
                commit.setPr(refsPull);
                projectPullRepository.save(refsPull);
                commitRepository.save(commit);
                LOGGER.info("Commit {} added to pull request {}", commit.getSha(), refsPull.getId());
            } else {
                ProjectIssue issueById = projectIssueRepository.findIssueById(commit.getProjectOwner(), commit.getProjectName(), Long.parseLong(prNumber));
                if (issueById != null) {
                    issueById.addCommits(List.of(commit));
                    commit.setIssue(issueById);
                    projectIssueRepository.save(issueById);
                    commitRepository.save(commit);
                    LOGGER.info("Commit {} added to issue {}", commit.getSha(), issueById.getId());
                }
            }
            System.out.println("Pull Request Number: " + prNumber);
        } else {
            System.out.println("No pull request number found.");
        }
    }
}
