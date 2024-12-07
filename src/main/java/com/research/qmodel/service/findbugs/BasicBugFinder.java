package com.research.qmodel.service.findbugs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.research.qmodel.annotations.AGraphDeserializer;
import com.research.qmodel.annotations.ChangePatchProcessor;
import com.research.qmodel.annotations.FileChangesDeserializer;
import com.research.qmodel.errors.IssueNotFoundException;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectID;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.model.ProjectPull;
import com.research.qmodel.repos.AGraphRepository;
import com.research.qmodel.repos.CommitRepository;
import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.repos.ProjectPullRepository;
import com.research.qmodel.repos.ProjectRepository;
import com.research.qmodel.service.BasicQueryService;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BasicBugFinder implements ChangePatchProcessor {
  @Autowired private CommitRepository commitRepository;
  @Autowired private ProjectIssueRepository projectIssueRepository;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private BasicQueryService basicQueryService;
  @Autowired private ProjectPullRepository projectPullRepository;
  @Autowired private ProjectRepository projectRepository;
  private final Logger LOGGER = LoggerFactory.getLogger(BasicBugFinder.class);
  @Autowired private AGraphRepository aGraphRepository;

  @Value("${qmodel.defect.labels:bug}")
  private List<String> LABELS;

  public List<Commit> findAllBugsFixingCommits(String repoName, String repoOwner, int depth)
      throws JsonProcessingException {
    AGraph aGraph;
    Optional<AGraph> foundGraph =
        aGraphRepository.findByRepoOwnerAndRepoProjectName(repoOwner, repoName);
    if (foundGraph.isPresent()) {
      aGraph = foundGraph.get();
    } else {
      return null;
    }

    Queue<ProjectIssue> projectIssues =
        new LinkedList<>(projectIssueRepository.finAllFixedIssues(repoName, repoOwner));
    while (!projectIssues.isEmpty()) {
      ProjectIssue projectIssue = projectIssues.poll();
      boolean isDefect = projectIssue.getLabels().stream().anyMatch(LABELS::contains);
      if (!isDefect) {
        continue;
      }
      Optional<Commit> hasNoFiles =
          projectIssue.getCommits().stream().filter(f -> f.getFileChanges().isEmpty()).findAny();
      if (!projectIssue.getCommits().isEmpty()) {

        if (!hasNoFiles.isPresent()) {
          continue;
        }
      }

      ProjectPull fixPR = projectIssue.getFixPR();
      if (fixPR == null) {
        continue;
      }
      // 20594L
      String pr = fixPR.getRawPull();
      JsonNode rawPr = objectMapper.readTree(pr);
      String commitsUrl = rawPr.path("commits_url").asText();
      JsonNode rowData = basicQueryService.getRowData(commitsUrl);
      List<Commit> retrievedCommits = new ArrayList<>();
      for (JsonNode commitRow : rowData) {
        JsonNode serializedCommit = commitRow.get("commit");
        Commit deserializedCommit = objectMapper.convertValue(serializedCommit, Commit.class);
        deserializedCommit.setSha(commitRow.get("sha").asText());
        deserializedCommit.setRawData(commitRow.toString());

        SimpleModule module = new SimpleModule();
        module.addDeserializer(List.class, new FileChangesDeserializer(basicQueryService));
        objectMapper.registerModule(module);
        List<FileChange> fileChanges =
            objectMapper.convertValue(commitRow, new TypeReference<>() {});
        if (fileChanges == null || fileChanges.isEmpty()) {
          LOGGER.error("Failed to pull out files for commit " + commitRow);
          continue;
        }
        List<FileChange> files =
            fileChanges.stream().filter(f -> f != null).collect(Collectors.toList());
        deserializedCommit.setNumOfFilesChanged(fileChanges.size());
        aGraph.addCoommit(deserializedCommit);
        deserializedCommit.setAGraph(aGraph);
        deserializedCommit.setFileChanges(fileChanges);
        for (FileChange fileChange : files) {
          fileChange.addCommit(deserializedCommit);
        }
        retrievedCommits.add(deserializedCommit);
      }
      if (retrievedCommits.isEmpty()) {
        continue;
      }

      List<Commit> foundCommitsInDb =
          retrievedCommits.stream()
              .filter(Objects::nonNull)
              .filter(c -> c.getSha() != null)
              .map(c -> commitRepository.findById(new CommitID(c.getSha())).orElse(null))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (projectIssue.getCommits() == null
          || projectIssue.getCommits().isEmpty()
          || hasNoFiles.isPresent()) {
        projectIssue.getCommits().clear();
        if (!foundCommitsInDb.isEmpty()
            && foundCommitsInDb.stream().anyMatch(e -> !e.getFileChanges().isEmpty())) {
          projectIssue.getCommits().addAll(foundCommitsInDb);
        } else {
          projectIssue.getCommits().addAll(retrievedCommits);
        }
        try {
          projectIssueRepository.save(projectIssue);
        } catch (Exception e) {
          System.out.println(e);
        }

        Optional<Commit> prHasNoFiles =
            fixPR.getCommits().stream().filter(f -> f.getFileChanges().isEmpty()).findAny();

        if (fixPR.getCommits().isEmpty() || prHasNoFiles.isEmpty()) {
          if (!foundCommitsInDb.isEmpty()) {

            fixPR.addCommits(foundCommitsInDb);
          } else {
            fixPR.addCommits(retrievedCommits);
          }
          try {

            projectPullRepository.save(fixPR);
          } catch (Exception e) {
            System.out.println(e);
          }
        }
      }
      if (retrievedCommits != null) {
        // commits.addAll(retrievedCommit);
      }
    }

    return new ArrayList<>();
  }

  public List<Commit> findBugIntroducingCommits(String owner, String repo, Long issueId, int depth)
      throws IOException, GitAPIException {
    ProjectIssue foundIssue = projectIssueRepository.findIssueById(repo, owner, issueId);
    if (foundIssue == null) {
      throw new IssueNotFoundException("Issue with id " + issueId + " is not found.");
    }

    List<Commit> candidateCommits = new ArrayList<>();
    List<Commit> commits = foundIssue.getCommits();

    for (Commit commit : commits) {
      String currentCommitSha = commit.getSha();
      String repoPath = "/Users/dpolishchuk/" + owner + "_" + repo;

      for (FileChange fileChange : commit.getFileChanges()) {
        Set<Integer> modifiedLines = getChangedLineNumbers(fileChange.getPatch());

        for (int line : modifiedLines) {
          List<Commit> bugIntroducingCommitsCandidates =
              traceLineToCommit(repoPath, fileChange.getFileName(), line, currentCommitSha, depth);

          if (bugIntroducingCommitsCandidates != null
              && !bugIntroducingCommitsCandidates.isEmpty()) {
            candidateCommits.addAll(bugIntroducingCommitsCandidates);
          }
        }
      }
    }
    return candidateCommits;
  }

  private String getRepositoryPath(String repo) {
    return null;
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

  public void traceCommitsToOrigin(String owner, String repo, int depth)
      throws IOException, GitAPIException {
    String repoPath = "/Users/dpolishchuk/" + owner + "_" + repo;
    Queue<ProjectIssue> projectIssues =
        new LinkedList<>(projectIssueRepository.finAllFixedIssues(repo, owner));

    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    try (Repository repository =
            builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build();
        Git git = new Git(repository)) {

      while (!projectIssues.isEmpty()) {
        ProjectIssue issue = projectIssues.poll();
        if (issue.getFixPR() == null
            || issue.getCommits() == null
            || issue.getCommits().isEmpty()
            || (issue.getBugIntroducingCommits() != null
                && !issue.getBugIntroducingCommits().isEmpty())) {
          continue;
        }

        // Precompute SHA list for the PR commits once per issue
        List<String> prCommitShas = issue.getCommits().stream().map(Commit::getSha).toList();

        for (Commit commit : issue.getCommits()) {
          List<Commit> bugIntroducingCommits = new ArrayList<>();

          String currentCommitSha = commit.getSha();
          lineLoop:
          for (FileChange file : commit.getFileChanges()) {
            List<Integer> changedLines = new ArrayList<>(file.getChangedLines());

            for (Integer line : changedLines) {
              // Set<String> visitedCommits = new HashSet<>();
              for (int i = 0; i < depth; i++) {
                try {
                  //                if (visitedCommits.contains(currentCommitSha)) {
                  //                  break lineLoop;
                  //                }
                  // visitedCommits.add(currentCommitSha);

                  String blamedCommitSha =
                      getBlamedCommit(git, repository, file.getFileName(), line, currentCommitSha);

                  // Skip commits that belong to the same PR
                  if (prCommitShas.contains(blamedCommitSha)) {
                    currentCommitSha = blamedCommitSha;
                    continue;
                  }

                  if (blamedCommitSha == null || blamedCommitSha.equals(currentCommitSha)) {
                    break lineLoop;
                  }

                  RevCommit blamedCommit =
                      repository.parseCommit(ObjectId.fromString(blamedCommitSha));

                  if (blamedCommit != null) {
                    Optional<Commit> foundCommit =
                        commitRepository.findById(new CommitID(blamedCommitSha));
                    if (foundCommit.isPresent()) {
                      issue.addBugIntroducing(foundCommit.get());
                      bugIntroducingCommits.add(foundCommit.get());
                      currentCommitSha = blamedCommitSha;
                    }
                  } else {
                    break;
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
        // Save the modified issue with added bug-introducing commits
        try {

          projectIssueRepository.save(issue);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      // Add detailed logging here for debugging purposes
      System.err.println("Error processing issues: " + e.getMessage());
    }
  }

  public List<Commit> traceLineToCommit(
      String repoPath, String file, int line, String startingCommit, int depth)
      throws IOException, GitAPIException {
    List<Commit> bugIntroducingCommits = new ArrayList<>();
    Set<String> visitedCommits = new HashSet<>();
    String currentCommitSha = startingCommit;

    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    try (Repository repository =
        builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build()) {
      try (Git git = new Git(repository)) {
        for (int i = 0; i < depth; i++) {
          if (visitedCommits.contains(currentCommitSha)) {
            break;
          }
          visitedCommits.add(currentCommitSha);
          String blamedCommitSha = getBlamedCommit(git, repository, file, line, currentCommitSha);
          if (blamedCommitSha == null || blamedCommitSha.equals(currentCommitSha)) {
            break;
          }

          // Retrieve the commit details and add to results
          RevCommit blamedCommit = repository.parseCommit(ObjectId.fromString(blamedCommitSha));
          if (blamedCommit != null) {
            Optional<Commit> foundCommit = commitRepository.findById(new CommitID(blamedCommitSha));
            if (foundCommit.isPresent()) {
              bugIntroducingCommits.add(foundCommit.get());
              currentCommitSha = blamedCommitSha;
            }
          } else {
            break;
          }
        }
      }
    }

    return bugIntroducingCommits;
  }

  private String getBlamedCommit(
      Git git, Repository repository, String file, int line, String startingCommit)
      throws GitAPIException, IOException {
    ObjectId commitId = repository.resolve(startingCommit);

    // Run blame on the specified file and line
    BlameResult blameResult = git.blame().setFilePath(file).setStartCommit(commitId).call();

    if (blameResult != null) {
      int totalLinesInBlame = blameResult.getResultContents().size();
      if (line - 1 < totalLinesInBlame && line - 1 >= 0) {
        RevCommit sourceCommit = blameResult.getSourceCommit(line - 1);
        if (sourceCommit != null) {
          return sourceCommit.getName();
        }
      } else {
        System.out.println(
            "Skipping line "
                + line
                + ": out of bounds for blame result with "
                + totalLinesInBlame
                + " lines.");
      }
    } else {
      System.out.println(
          "Blame result is null for file: " + file + " in commit: " + startingCommit);
    }
    return null;
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
