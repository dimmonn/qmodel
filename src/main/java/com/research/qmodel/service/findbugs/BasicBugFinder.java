package com.research.qmodel.service.findbugs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.annotations.ChangePatchProcessor;
import com.research.qmodel.errors.IssueNotFoundException;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.model.ProjectPull;
import com.research.qmodel.model.Timeline;
import com.research.qmodel.repos.CommitRepository;
import com.research.qmodel.repos.ProjectIssueRepository;
import com.research.qmodel.repos.ProjectPullRepository;
import com.research.qmodel.service.BasicQueryService;
import com.research.qmodel.service.DataPersistance;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
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
  private final Logger LOGGER = LoggerFactory.getLogger(BasicBugFinder.class);

  @Value("${qmodel.defect.labels:bug}")
  private List<String> LABELS;

  @Autowired private DataPersistance dataPersistance;

  public List<String> findAllBugsFixingCommits(String repoName, String repoOwner, int depth)
      throws JsonProcessingException {
    Queue<ProjectIssue> fixedIssues =
        new LinkedList<>(projectIssueRepository.finAllFixedIssues(repoName, repoOwner));
    List<String> cachedCommits = new ArrayList<>();
    while (!fixedIssues.isEmpty()) {
      LOGGER.info("Issues still remains in the queue: {}", Optional.of(fixedIssues.size()));
      ProjectIssue projectIssue = fixedIssues.poll();
      if (projectIssue == null) {
        LOGGER.warn("Project issue is null, continuing..");
        continue;
      }
      if (projectIssue.getFixingCommits() != null && !projectIssue.getFixingCommits().isEmpty()) {
        LOGGER.warn(
            "Commits are not empty, had been added already: issue id# {}", projectIssue.getId());
        continue;
      }
      ProjectPull fixPR = projectIssue.getFixPR();
      if (fixPR == null) {
        LOGGER.warn("There is no PR that resolves the issue: issue id# {}", projectIssue.getId());
        Set<ProjectPull> fixingCandidates = projectIssue.getProjectPull();
        for (ProjectPull fixingCandidate : fixingCandidates) {
          Set<Timeline> timeLines = fixingCandidate.getTimeLine();
          for (Timeline timeline : timeLines) {
            if (timeline.getRawData().contains(String.valueOf(projectIssue.getId()))) {
              fixPR = fixingCandidate;
              break;
            }
          }
        }
        if (fixPR == null && !fixingCandidates.isEmpty()) {
          fixPR = fixingCandidates.stream().findAny().orElse(null);
        }
        if (fixPR == null) {
          continue;
        }
      }
      String pr = fixPR.getRawPull();
      JsonNode rawPr = objectMapper.readTree(pr);
      String commitsUrl = rawPr.path("commits_url").asText();
      JsonNode rowData = basicQueryService.getRowDataWithCursor(commitsUrl);
      if (rowData == null) {
        LOGGER.warn("commitsUrl is not available {}", commitsUrl);
        continue;
      }
      List<String> retrievedCommits = new ArrayList<>();
      for (JsonNode commitRow : rowData) {
        String sha = commitRow.path("sha").asText();
        if (sha.isEmpty()) {
          LOGGER.warn("sha is not found for issue: issue id# {}", projectIssue.getId());
          continue;
        }
        retrievedCommits.add(sha);
      }
      if (retrievedCommits.isEmpty()) {
        LOGGER.warn(
            "No commits had been found in PR {} for issue: issue id# {}",
            fixPR.getId(),
            projectIssue.getId());
        continue;
      }
      cachedCommits.addAll(retrievedCommits);
      List<Commit> foundCommitsInDb =
          retrievedCommits.stream()
              .filter(Objects::nonNull)
              .filter(StringUtils::isNotBlank)
              .map(
                  c ->
                      commitRepository
                          .findById(new CommitID(c))
                          .orElseGet(
                              () -> {
                                AGraph graphWithoutForks =
                                    basicQueryService.retrieveCommitBySha(repoOwner, repoName, c);
                                dataPersistance.persistGraph(
                                    List.of(new Project(repoOwner, repoName)),
                                    Map.of(new Project(repoOwner, repoName), graphWithoutForks));
                                return commitRepository.findById(new CommitID(c)).orElse(null);
                              }))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      if (foundCommitsInDb.isEmpty()) {
        LOGGER.warn(
            "No commits had been found in database for sha: {} and issue id# {}, maybe this commit does not belong to any branch on this repository, and may belong to a fork outside of the repository.",
            retrievedCommits,
            projectIssue.getId());
        continue;
      }
      if (projectIssue.getFixingCommits() == null || projectIssue.getFixingCommits().isEmpty()) {
        if (foundCommitsInDb.stream().anyMatch(e -> !e.getFileChanges().isEmpty())) {
          if (projectIssue.getFixingCommits() != null) {
            projectIssue.getFixingCommits().addAll(foundCommitsInDb);
          }
        } else {
          LOGGER.warn(
              "Changed files are empty for issue id# {}, and commits {}",
              projectIssue.getId(),
              foundCommitsInDb.stream().map(Commit::getSha).toList());
          continue;
        }
        try {
          projectIssueRepository.save(projectIssue);
          LOGGER.info(
              "Issues id# {} has been saved in the DB: commits are: {}",
              projectIssue.getId(),
              foundCommitsInDb.stream().map(Commit::getSha).toList());
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
          continue;
        }
        if (fixPR.getCommits().isEmpty()) {
          fixPR.addCommits(foundCommitsInDb);
          try {
            projectPullRepository.save(fixPR);
            LOGGER.info(
                "PR id# {} has been saved in the DB: commits are: {}",
                fixPR.getId(),
                foundCommitsInDb.stream().map(Commit::getSha).toList());
          } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
          }
        }
      }
    }
    return cachedCommits;
  }

  public List<Commit> findBugIntroducingCommits(String owner, String repo, Long issueId, int depth)
      throws IOException, GitAPIException {
    ProjectIssue foundIssue = projectIssueRepository.findIssueById(repo, owner, issueId);
    if (foundIssue == null) {
      throw new IssueNotFoundException("Issue with id " + issueId + " is not found.");
    }
    if (foundIssue.getBugIntroducingCommits() != null
        && !foundIssue.getBugIntroducingCommits().isEmpty()) {}

    List<Commit> candidateCommits = new ArrayList<>();
    List<Commit> commits = foundIssue.getFixingCommits();

    for (Commit commit : commits) {
      String currentCommitSha = commit.getSha();
      String repoPath = "/Users/dima/" + owner + "_" + repo;

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

  public void traceCommitsToOrigin(String owner, String repo, int depth) {
    String repoPath = "/Users/dima/" + owner + "_" + repo;
    Queue<ProjectIssue> projectIssues =
        new LinkedList<>(projectIssueRepository.finAllFixedIssues(repo, owner));

    FileRepositoryBuilder builder = new FileRepositoryBuilder();

    try (Repository repository =
            builder.setGitDir(new File(repoPath + "/.git")).readEnvironment().findGitDir().build();
        Git git = new Git(repository)) {

      while (!projectIssues.isEmpty()) {
        ProjectIssue issue = projectIssues.poll();
          if (!issue.getBugIntroducingCommits().isEmpty()) {
              LOGGER.info("Issue {} has been processed already", issue.getId());
              continue;
          }
        LOGGER.info("Issues still left in the queue: {}", Optional.of(projectIssues.size()));

        // Skip if no commits associated
        if (issue.getFixingCommits() == null || issue.getFixingCommits().isEmpty()) {
            LOGGER.warn("No commits associated with issue {}", issue.getId());
        }

        Set<String> visited = new HashSet<>();

        for (Commit fixCommit : issue.getFixingCommits()) {
          RevCommit revFix;
          try {
            revFix = repository.parseCommit(ObjectId.fromString(fixCommit.getSha()));
          } catch (Exception e) {
            checkoutOrphanedCommit(git, repository, fixCommit.getSha());
            try {
              revFix = repository.parseCommit(ObjectId.fromString(fixCommit.getSha()));
            } catch (Exception e1) {
              LOGGER.error("failed to pick up orphaned commit", e1);
              continue;
            }
          }
          if (revFix.getParentCount() == 0) {
            LOGGER.warn("No parent found for commit {}", fixCommit.getSha());
            continue;
          }

          for (RevCommit parent : revFix.getParents()) {
            for (FileChange file : fixCommit.getFileChanges()) {
              for (Integer line : file.getChangedLines()) {
                try {
                  recursivelyTraceLine(
                      git,
                      repository,
                      file.getFileName(),
                      line,
                      parent.getName(), // Start tracing from parent of the fix
                      depth,
                      visited,
                      issue);
                } catch (Exception e) {
                  LOGGER.error(
                      "Trace error for line {} in {}: {}",
                      line,
                      file.getFileName(),
                      e.getMessage(),
                      e);
                }
              }
            }
          }
        }

        try {
          projectIssueRepository.save(issue);
          LOGGER.info("Issue {} saved in the DB", issue.getId());
        } catch (Exception e) {
          LOGGER.error("Error saving issue {}: {}", issue.getId(), e.getMessage(), e);
        }
      }

    } catch (Exception e) {
      LOGGER.error("Fatal error during SZZ tracing: {}", e.getMessage(), e);
    }

    LOGGER.info("Trace to origin completed for repo {} and owner {}", repo, owner);
  }

  private void recursivelyTraceLine(
      Git git,
      Repository repo,
      String file,
      int line,
      String blameContextSha,
      int depth,
      Set<String> visitedLineCommitPairs,
      ProjectIssue issue)
      throws Exception {

    if (depth <= 0) {
      LOGGER.debug("Depth exhausted for {}:{}@{}", file, Optional.of(line), blameContextSha);
      return;
    }

    String visitKey = file + ":" + line + "@" + blameContextSha;
    if (visitedLineCommitPairs.contains(visitKey)) return;

    visitedLineCommitPairs.add(visitKey);

    String blamedSha = getBlamedCommit(git, repo, file, line, blameContextSha);
    if (blamedSha == null || blamedSha.equals(blameContextSha)) {
      LOGGER.warn("Blame returned null for {}:{}@{}", file, Optional.of(line), blameContextSha);
      return;
    }

    Optional<Commit> blamed = commitRepository.findById(new CommitID(blamedSha));
    if (blamed.isPresent()) {
      Commit blamedCommit = blamed.get();
      RevCommit revCommit = repo.parseCommit(ObjectId.fromString(blamedSha));

      issue.addBugIntroducing(blamedCommit);
      for (int i = 0; i < revCommit.getParentCount(); i++) {
        RevCommit parent = revCommit.getParent(i);

        LOGGER.info(
            "Tracing blamed commit {} -> parent {} for file '{}' line {} (depth remaining: {}) on issue id {}",
            blamedSha,
            parent.getName(),
            file,
            line,
            depth - 1,
            issue.getId());

        recursivelyTraceLine(
            git, repo, file, line, parent.getName(), depth - 1, visitedLineCommitPairs, issue);
      }
    } else {
      LOGGER.warn(
          "Blamed commit {} not found for line {} in file {}", blamedSha, Optional.of(line), file);
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
      throws IOException, GitAPIException {
    BlameResult blameResult = null;
    ObjectId commitId = repository.resolve(startingCommit);

    if (commitId == null) {
      System.out.println("Commit " + startingCommit + " not found. Fetching from origin...");
      try {
        git.fetch()
            .setRemote("origin")
            .setRefSpecs("+refs/*:refs/remotes/origin/*") // Fetch all refs to find the commit
            .call();

        // Re-resolve the commit after fetching
        commitId = repository.resolve(startingCommit);

        if (commitId == null) {
          System.out.println("Commit " + startingCommit + " still missing after fetch.");
          return null;
        }
      } catch (Exception fetchException) {
        System.out.println("Error fetching commit: " + fetchException.getMessage());
        return null;
      }
    }

    try {
      blameResult = findBlamedRef(git, file, commitId);
    } catch (Exception blameException) {
      checkoutOrphanedCommit(git, repository, startingCommit);
      blameResult = findBlamedRef(git, file, commitId);
    }

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

  private void checkoutOrphanedCommit(Git git, Repository repository, String commitSha)
      throws GitAPIException, IOException {

    try {
      git.fetch()
          .setRemote("origin")
          .setRefSpecs(
              "+" + commitSha + ":refs/remotes/origin/temp_commit_" + new Random().nextInt(10))
          .call();
      resolveLockIssue(git.getRepository().getDirectory().toPath().toString());
      git.clean().setCleanDirectories(true).setForce(true).call();
      git.stashCreate().setIncludeUntracked(true).call();
      git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commitSha).call();
      String tempBranch = "temp_commit_" + commitSha.substring(0, 7);
      git.branchCreate()
          .setName(tempBranch)
          .setStartPoint(commitSha)
          .setForce(true)
          .call();
      git.checkout().setName(tempBranch).call();
      LOGGER.info("Checked out orphaned commit {} via branch {}", commitSha, tempBranch);
    } catch (Exception e) {
      LOGGER.error("Failed to process commit in szz {}", commitSha, e);
    }
  }

  private void resolveLockIssue(String repoPath) throws IOException, InterruptedException {
    File lockFile = new File(repoPath + "/index.lock");

    if (lockFile.exists()) {
      System.out.println("Found stale lock file: " + lockFile.getPath());
      if (lockFile.delete()) {
        System.out.println("Successfully deleted the lock file.");
      } else {
        System.out.println("Failed to delete the lock file.");
      }
    }
    Thread.sleep(1000);
  }

  private static BlameResult findBlamedRef(Git git, String file, ObjectId commitId)
      throws GitAPIException {
    return git.blame().setFilePath(file).setStartCommit(commitId).setFollowFileRenames(true).call();
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
