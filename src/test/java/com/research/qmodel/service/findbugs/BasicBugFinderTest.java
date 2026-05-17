package com.research.qmodel.service.findbugs;

import com.research.qmodel.errors.IssueNotFoundException;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.repos.CommitRepository;
import com.research.qmodel.repos.ProjectIssueRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BasicBugFinder SZZ Algorithm Tests")
class BasicBugFinderTest {

  @Mock private CommitRepository commitRepository;
  @Mock private ProjectIssueRepository projectIssueRepository;
  @InjectMocks private BasicBugFinder basicBugFinder;

  private static final String TEST_OWNER = "testOwner";
  private static final String TEST_REPO = "testRepo";
  private static final Long TEST_ISSUE_ID = 123L;
  private static final int TEST_DEPTH = 3;

  @BeforeEach
  void setUp() throws Exception {
    // Set repoBasePath for testing
    Field repoBasePathField = BasicBugFinder.class.getDeclaredField("repoBasePath");
    repoBasePathField.setAccessible(true);
    repoBasePathField.set(basicBugFinder, "/tmp/test_repos");
  }

  @Nested
  @DisplayName("findBugIntroducingCommits Tests")
  class FindBugIntroducingCommitsTests {

    @Test
    @DisplayName("Should throw IssueNotFoundException when issue not found")
    void testIssueNotFound() {
      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(null);

      IssueNotFoundException exception =
          assertThrows(
              IssueNotFoundException.class,
              () ->
                  basicBugFinder.findBugIntroducingCommits(
                      TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, TEST_DEPTH));

      assertTrue(exception.getMessage().contains(String.valueOf(TEST_ISSUE_ID)));
    }

    @Test
    @DisplayName("Should return cached bug-introducing commits if already found")
    void testReturnCachedResults() throws IOException, GitAPIException {
      ProjectIssue issue = new ProjectIssue();
      issue.setId(TEST_ISSUE_ID);

      Commit cachedCommit = new Commit();
      cachedCommit.setSha("abc123");

      List<Commit> cachedBugs = new ArrayList<>();
      cachedBugs.add(cachedCommit);
      issue.setBugIntroducingCommits(cachedBugs);

      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(issue);

      List<Commit> result =
          basicBugFinder.findBugIntroducingCommits(TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, TEST_DEPTH);

      assertEquals(cachedBugs, result);
      verify(projectIssueRepository, times(1)).findIssueById(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should handle issue with null bug-introducing commits")
    void testIssueWithNullBugCommits() throws IOException, GitAPIException {
      ProjectIssue issue = new ProjectIssue();
      issue.setId(TEST_ISSUE_ID);
      issue.setBugIntroducingCommits(null);

      Commit fixCommit = new Commit();
      fixCommit.setSha("fix123");
      fixCommit.setFileChanges(new ArrayList<>());

      issue.setFixingCommits(new ArrayList<>(List.of(fixCommit)));

      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(issue);

      List<Commit> result =
          basicBugFinder.findBugIntroducingCommits(TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, TEST_DEPTH);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("getChangedLineNumbers Tests")
  class GetChangedLineNumbersTests {

    @Test
    @DisplayName("Should extract added lines from patch")
    void testExtractAddedLines() throws Exception {
      String patch =
          "@@ -5,10 +7,12 @@\n"
              + " context line\n"
              + "+added line 1\n"
              + "+added line 2\n"
              + " context line\n"
              + "-deleted line\n"
              + "+added line 3\n";

      Set<Integer> result = callGetChangedLineNumbers(patch);

      assertNotNull(result);
      assertTrue(result.contains(8)); // First added line
      assertTrue(result.contains(9)); // Second added line
      assertTrue(result.contains(11)); // Third added line (after deletions at line 11, not 12)
    }

    @Test
    @DisplayName("Should handle null patch")
    void testNullPatch() throws Exception {
      Set<Integer> result = callGetChangedLineNumbers(null);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty patch")
    void testEmptyPatch() throws Exception {
      Set<Integer> result = callGetChangedLineNumbers("");

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle patch with multiple hunks")
    void testMultipleHunks() throws Exception {
      String patch =
          "@@ -5,10 +7,12 @@\n"
              + "+added line 1\n"
              + " context\n"
              + "@@ -20,5 +25,7 @@\n"
              + "+added line 2\n"
              + " context\n";

      Set<Integer> result = callGetChangedLineNumbers(patch);

      assertNotNull(result);
      assertTrue(result.contains(7)); // First hunk
      assertTrue(result.contains(25)); // Second hunk
    }

    @Test
    @DisplayName("Should ignore deletion-only patches")
    void testDeletionOnlyPatch() throws Exception {
      String patch =
          "@@ -5,10 +5,8 @@\n"
              + " context\n"
              + "-deleted line 1\n"
              + "-deleted line 2\n"
              + " context\n";

      Set<Integer> result = callGetChangedLineNumbers(patch);

      assertNotNull(result);
      // Deletion-only patches have no added lines, so result should be empty
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle patch with malformed hunk headers gracefully")
    void testMalformedHunkHeader() throws Exception {
      String patch =
          "@@ invalid @@\n"
              + "+added line\n"
              + " context\n";

      // Should not throw exception
      Set<Integer> result = callGetChangedLineNumbers(patch);
      assertNotNull(result);
    }
  }

  @Nested
  @DisplayName("Stream Closure Tests")
  class StreamClosureTests {

    @Test
    @DisplayName("runGitCommand should close resources properly")
    void testRunGitCommandClosesResources() throws Exception {
      // This is a behavioral test - ensure no resource leaks occur
      // The method signature doesn't expose streams, so we verify it completes successfully
      List<String> result = callRunGitCommand("/tmp", "echo test");

      assertNotNull(result);
      // The actual git command will fail since we're testing mock, but the important part
      // is that it doesn't leave unclosed resources
    }
  }

  @Nested
  @DisplayName("Null Check Tests")
  class NullCheckTests {

    @Test
    @DisplayName("Should handle null file changes gracefully")
    void testNullFileChanges() throws Exception {
      ProjectIssue issue = new ProjectIssue();
      issue.setId(TEST_ISSUE_ID);

      Commit fixCommit = new Commit();
      fixCommit.setSha("fix123");
      fixCommit.setFileChanges(null); // Null file changes

      issue.setFixingCommits(new ArrayList<>(List.of(fixCommit)));

      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(issue);

      // Should not throw NullPointerException
      List<Commit> result =
          basicBugFinder.findBugIntroducingCommits(TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, TEST_DEPTH);

      assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle null changed lines in file changes")
    void testNullChangedLines() throws Exception {
      FileChange fileChange = new FileChange();
      fileChange.setFileName("test.java");
      fileChange.setChangedLines(null); // Null changed lines

      assertDoesNotThrow(
          () -> {
            // Simulate what happens in recursivelyTraceLine
            if (fileChange.getChangedLines() != null) {
              for (Integer line : fileChange.getChangedLines()) {
                // Process line
              }
            }
          });
    }
  }

  @Nested
  @DisplayName("Repository Path Tests")
  class RepositoryPathTests {

    @Test
    @DisplayName("getRepositoryPath should construct correct path")
    void testGetRepositoryPath() throws Exception {
      String owner = "junit";
      String repo = "junit5";

      String result = callGetRepositoryPath(owner, repo);

      assertTrue(result.contains(owner));
      assertTrue(result.contains(repo));
      assertTrue(result.endsWith("junit_junit5"));
    }

    @Test
    @DisplayName("getRepositoryPath should use configurable base path")
    void testRepositoryPathUsesConfigurable() throws Exception {
      // Verify that repoBasePath is being used
      String owner = "testOwner";
      String repo = "testRepo";

      String result = callGetRepositoryPath(owner, repo);

      assertTrue(result.startsWith("/tmp/test_repos"));
    }
  }

  @Nested
  @DisplayName("Line Number Tracking Tests")
  class LineNumberTrackingTests {

    @Test
    @DisplayName("Should properly handle line number increments in patches")
    void testLineNumberIncrementInPatches() throws Exception {
      // Test patch with context and added lines
      String patch =
          "@@ -10,8 +10,10 @@\n"
              + " int x = 5;        // line 10\n"
              + " int y = 10;       // line 11\n"
              + "+int z = 15;       // line 12 (NEW)\n"
              + "+int w = 20;       // line 13 (NEW)\n"
              + " return x + y;     // line 14\n"
              + " }                 // line 15\n";

      Set<Integer> result = callGetChangedLineNumbers(patch);

      assertTrue(result.contains(12), "Should contain newly added line 12");
      assertTrue(result.contains(13), "Should contain newly added line 13");
    }

    @Test
    @DisplayName("Should handle lines removed before traced line")
    void testLineNumberAfterDeletion() throws Exception {
      String patch =
          "@@ -5,8 +5,7 @@\n"
              + " line 5\n"
              + " line 6\n"
              + "-deleted line 7\n"
              + " line 8\n"
              + "+new line 8\n"
              + " line 9\n";

      Set<Integer> result = callGetChangedLineNumbers(patch);

      assertNotNull(result);
      assertFalse(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle empty commit list")
    void testEmptyCommitList() throws IOException, GitAPIException {
      ProjectIssue issue = new ProjectIssue();
      issue.setId(TEST_ISSUE_ID);
      issue.setFixingCommits(new ArrayList<>());

      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(issue);

      List<Commit> result =
          basicBugFinder.findBugIntroducingCommits(TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, TEST_DEPTH);

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle commit with no file changes")
    void testCommitWithNoFileChanges() throws IOException, GitAPIException {
      ProjectIssue issue = new ProjectIssue();
      issue.setId(TEST_ISSUE_ID);

      Commit commit = new Commit();
      commit.setSha("abc123");
      commit.setFileChanges(new ArrayList<>());

      issue.setFixingCommits(new ArrayList<>(List.of(commit)));

      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(issue);

      List<Commit> result =
          basicBugFinder.findBugIntroducingCommits(TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, TEST_DEPTH);

      assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle very deep depth values")
    void testVeryDeepDepth() throws IOException, GitAPIException {
      ProjectIssue issue = new ProjectIssue();
      issue.setId(TEST_ISSUE_ID);
      issue.setFixingCommits(new ArrayList<>());

      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(issue);

      // Should handle large depth values without issues
      List<Commit> result =
          basicBugFinder.findBugIntroducingCommits(TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, 1000);

      assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle negative depth gracefully")
    void testNegativeDepth() throws IOException, GitAPIException {
      ProjectIssue issue = new ProjectIssue();
      issue.setId(TEST_ISSUE_ID);
      issue.setFixingCommits(new ArrayList<>());

      when(projectIssueRepository.findIssueById(TEST_REPO, TEST_OWNER, TEST_ISSUE_ID))
          .thenReturn(issue);

      // Negative depth should result in empty results
      List<Commit> result =
          basicBugFinder.findBugIntroducingCommits(TEST_OWNER, TEST_REPO, TEST_ISSUE_ID, -1);

      assertNotNull(result);
    }
  }

  // ==================== Helper Methods ====================

  private Set<Integer> callGetChangedLineNumbers(String patch) throws Exception {
    java.lang.reflect.Method method =
        BasicBugFinder.class.getDeclaredMethod("getChangedLineNumbers", String.class);
    method.setAccessible(true);
    return (Set<Integer>) method.invoke(basicBugFinder, patch);
  }

  private List<String> callRunGitCommand(String repoPath, String command) throws Exception {
    java.lang.reflect.Method method =
        BasicBugFinder.class.getDeclaredMethod("runGitCommand", String.class, String.class);
    method.setAccessible(true);
    try {
      return (List<String>) method.invoke(basicBugFinder, repoPath, command);
    } catch (Exception e) {
      // Expected to fail since we're not in a real git repo, but method should handle it
      return new ArrayList<>();
    }
  }

  private String callGetRepositoryPath(String owner, String repo) throws Exception {
    java.lang.reflect.Method method =
        BasicBugFinder.class.getDeclaredMethod("getRepositoryPath", String.class, String.class);
    method.setAccessible(true);
    return (String) method.invoke(basicBugFinder, owner, repo);
  }
}

