package com.research.qmodel.service.findbugs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.model.ProjectPull;
import com.research.qmodel.model.PullID;
import com.research.qmodel.repos.CommitRepository;
import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.repos.ProjectPullRepository;
import com.research.qmodel.service.BasicQueryService;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BasicBugFinder {
  @Autowired private CommitRepository commitRepository;
  @Autowired private ProjectIssueRepository projectIssueRepository;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private BasicQueryService basicQueryService;
  @Autowired private ProjectPullRepository projectPullRepository;

  public List<Commit> findAllBugsIntroducingCommits(String repoName, String repoOwner, int depth)
      throws JsonProcessingException {
    List<ProjectIssue> projectIssues =
        projectIssueRepository.finAllFixedIssues(repoName, repoOwner);
    List<Commit> commits = new ArrayList<>();
    for (ProjectIssue projectIssue : projectIssues) {
      ProjectPull fixPR = projectIssue.getFixPR();
      if (fixPR == null) {
        continue;
      }

      String pr = fixPR.getRawPull();
      JsonNode rawPr = objectMapper.readTree(pr);
      String commitsUrl = rawPr.path("commits_url").asText();
      JsonNode rowData = basicQueryService.getRowData(commitsUrl);
      List<Commit> foundCommits = objectMapper.convertValue(rowData, new TypeReference<>() {});
      foundCommits =
          foundCommits.stream()
              .filter(Objects::nonNull)
              .filter(c -> c.getSha() != null)
              .collect(Collectors.toList());

      if (foundCommits != null) {
        projectIssue.addCommits(foundCommits);
        try {
          projectIssueRepository.save(projectIssue);
        } catch (Exception e) {
          System.out.println(e);
        }
      }
      if (fixPR != null) {
        fixPR.addCommits(foundCommits);
        try {

          projectPullRepository.save(fixPR);
        } catch (Exception e) {
          System.out.println(e);
        }
      }
      commits.addAll(foundCommits);
    }
    return commits;
  }

  // Function to find bug-introducing commits using a pull request number
  public Set<String> findBugIntroducingCommits(String repoPath, String prNumber, int depth)
      throws IOException {
    // Step 1: Get the list of commits in the pull request
    List<String> commitsInPR = getCommitsFromPR(repoPath, prNumber);
    if (commitsInPR.isEmpty()) {
      throw new IllegalStateException("No commits found in the PR");
    }

    // Step 2: Use the last commit in the PR as the "bug-fixing" commit
    String bugFixingCommit = commitsInPR.get(commitsInPR.size() - 1);

    // Step 3: Identify the files modified in the bug-fixing commit
    List<String> modifiedFiles = getModifiedFiles(repoPath, bugFixingCommit);

    Set<String> candidateCommits = new HashSet<>();

    // Step 4: Trace the history of each modified line in each modified file
    for (String file : modifiedFiles) {
      List<String> modifiedLines = getModifiedLines(repoPath, bugFixingCommit, file);
      for (String line : modifiedLines) {
        String initialCommit = traceLineToOrigin(repoPath, file, line, bugFixingCommit, depth);
        if (initialCommit != null) {
          candidateCommits.add(initialCommit);
        }
      }
    }

    return candidateCommits;
  }

  // Fetch commits from the PR
  private List<String> getCommitsFromPR(String repoPath, String prNumber) throws IOException {
    String command =
        String.format(
            "git log --pretty=format:%%H --merges --grep=^Merge.*pull request #%s", prNumber);
    return runGitCommand(repoPath, command);
  }

  // Get the list of files modified in a commit
  private List<String> getModifiedFiles(String repoPath, String commitId) throws IOException {
    String command = String.format("git diff-tree --no-commit-id --name-only -r %s", commitId);
    return runGitCommand(repoPath, command);
  }

  // Get the modified lines in a file within a commit
  private List<String> getModifiedLines(String repoPath, String commitId, String file)
      throws IOException {
    String command = String.format("git diff %s~1 %s", commitId, file);
    // Parsing diff output to extract modified lines
    return parseDiffForModifiedLines(runGitCommand(repoPath, command));
  }

  // Trace the origin of a modified line using git blame
  private String traceLineToOrigin(
      String repoPath, String file, String line, String startingCommit, int depth)
      throws IOException {
    String currentCommit = startingCommit;

    for (int i = 0; i < depth; i++) {
      String command = String.format("git blame -L %s,%s --porcelain %s", line, line, file);
      currentCommit = runGitCommandAndExtractCommit(repoPath, command);
      if (currentCommit == null) break;
    }

    return currentCommit;
  }

  // Run a git command and return the output lines
  private List<String> runGitCommand(String repoPath, String command) throws IOException {
    ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
    pb.directory(new File(repoPath));
    Process process = pb.start();

    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String line;
    List<String> output = new ArrayList<>();
    while ((line = reader.readLine()) != null) {
      output.add(line);
    }

    return output;
  }

  // Parse the output of git blame to extract the commit
  private String runGitCommandAndExtractCommit(String repoPath, String command) throws IOException {
    List<String> output = runGitCommand(repoPath, command);
    // Example parsing logic for commit extraction from blame output
    if (!output.isEmpty()) {
      String commitLine = output.get(0);
      return commitLine.split(" ")[0]; // Assuming the commit hash is the first word
    }
    return null;
  }

  private List<String> parseDiffForModifiedLines(List<String> diffOutput) {
    // Implement parsing logic to extract line numbers of modified lines from git diff output
    return Arrays.asList("1", "2", "3"); // Placeholder example
  }
}
