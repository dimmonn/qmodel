package com.research.qmodel.service.findbugs;

import com.research.qmodel.model.Commit;
import com.research.qmodel.model.FileChange;
import com.research.qmodel.model.ProjectIssue;
import com.research.qmodel.repos.CommitRepository;
import com.research.qmodel.repos.ProjectIssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the SZZ Algorithm implementation
 * Tests realistic scenarios with mocked git operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SZZ Algorithm Integration Tests")
class BasicBugFinderIntegrationTest {

  @Mock private CommitRepository commitRepository;
  @Mock private ProjectIssueRepository projectIssueRepository;
  @InjectMocks private BasicBugFinder basicBugFinder;

  private static final String REPO_OWNER = "junit";
  private static final String REPO_NAME = "junit5";
  private static final Long TEST_ISSUE_ID = 456L;

  @BeforeEach
  void setUp() throws Exception {
    java.lang.reflect.Field repoBasePathField = BasicBugFinder.class.getDeclaredField("repoBasePath");
    repoBasePathField.setAccessible(true);
    repoBasePathField.set(basicBugFinder, "/tmp/git_repos");
  }

  @Test
  @DisplayName("Scenario: Fix a single-line bug fix")
  void testSingleLineBugFix() {
    // Setup: A fix commit that changed one line in a file
    Commit fixCommit = new Commit();
    fixCommit.setSha("fix001");

    FileChange fileChange = new FileChange();
    fileChange.setFileName("src/main/java/Example.java");
    fileChange.setPatch(
        "@@ -10,5 +10,5 @@\n"
            + " int x = 5;\n"
            + "-int buggy = x + y;  // Bug was here\n"
            + "+int fixed = x + y;  // Fixed\n"
            + " return fixed;\n");
    fileChange.setChangedLines(new HashSet<>(Arrays.asList(12)));

    fixCommit.setFileChanges(Arrays.asList(fileChange));

    ProjectIssue issue = new ProjectIssue();
    issue.setId(TEST_ISSUE_ID);
    issue.setFixingCommits(Arrays.asList(fixCommit));

    when(projectIssueRepository.findIssueById(REPO_NAME, REPO_OWNER, TEST_ISSUE_ID))
        .thenReturn(issue);

    // Execute
    assertDoesNotThrow(
        () -> basicBugFinder.findBugIntroducingCommits(REPO_OWNER, REPO_NAME, TEST_ISSUE_ID, 5));
  }

  @Test
  @DisplayName("Scenario: Multi-file bugfix with multiple lines changed")
  void testMultiFileBugFix() {
    // Setup: A fix that touched multiple files
    Commit fixCommit = new Commit();
    fixCommit.setSha("fix002");

    FileChange file1 = new FileChange();
    file1.setFileName("src/main/java/Service.java");
    file1.setPatch(
        "@@ -50,7 +50,8 @@\n"
            + " public void process() {\n"
            + "-   bug1();\n"
            + "+   fixed1();\n"
            + "+   fixed2();\n"
            + " }\n");
    file1.setChangedLines(new HashSet<>(Arrays.asList(52, 53)));

    FileChange file2 = new FileChange();
    file2.setFileName("src/main/java/Utils.java");
    file2.setPatch(
        "@@ -100,5 +100,6 @@\n"
            + " public String getData() {\n"
            + "-   return null;  // BUG\n"
            + "+   return cache.get();  // FIXED\n"
            + " }\n");
    file2.setChangedLines(new HashSet<>(Arrays.asList(102)));

    fixCommit.setFileChanges(Arrays.asList(file1, file2));

    ProjectIssue issue = new ProjectIssue();
    issue.setId(TEST_ISSUE_ID);
    issue.setFixingCommits(Arrays.asList(fixCommit));

    when(projectIssueRepository.findIssueById(REPO_NAME, REPO_OWNER, TEST_ISSUE_ID))
        .thenReturn(issue);

    // Execute
    assertDoesNotThrow(
        () -> basicBugFinder.findBugIntroducingCommits(REPO_OWNER, REPO_NAME, TEST_ISSUE_ID, 5));
  }

  @Test
  @DisplayName("Scenario: Large patch with context lines")
  void testLargePatchWithContext() {
    // Real-world patch with lots of context
    String largePatch =
        "@@ -1000,20 +1000,22 @@\n"
            + " public class LargeClass {\n"
            + "   private static final Logger LOG = getLogger();\n"
            + "   private String name;\n"
            + "   \n"
            + "-  public void oldBuggyMethod() {\n"
            + "+  public void fixedMethod() {\n"
            + "     try {\n"
            + "-    String result = null;  // BUG: NPE risk\n"
            + "+    String result = getValidResult();  // FIXED\n"
            + "       if (result.isEmpty()) {  // Was crashing here\n"
            + "         LOG.debug(\"Result is empty\");\n"
            + "       }\n"
            + "+    validateResult(result);\n"
            + "     } catch (Exception e) {\n"
            + "       LOG.error(\"Error\", e);\n"
            + "     }\n"
            + "   }\n"
            + " }\n";

    Commit fixCommit = new Commit();
    fixCommit.setSha("fix003");

    FileChange fileChange = new FileChange();
    fileChange.setFileName("src/main/java/LargeClass.java");
    fileChange.setPatch(largePatch);

    fixCommit.setFileChanges(Arrays.asList(fileChange));

    ProjectIssue issue = new ProjectIssue();
    issue.setId(TEST_ISSUE_ID);
    issue.setFixingCommits(Arrays.asList(fixCommit));

    when(projectIssueRepository.findIssueById(REPO_NAME, REPO_OWNER, TEST_ISSUE_ID))
        .thenReturn(issue);

    // Execute & Verify - should not throw and process correctly
    assertDoesNotThrow(
        () -> basicBugFinder.findBugIntroducingCommits(REPO_OWNER, REPO_NAME, TEST_ISSUE_ID, 10));
  }

  @Test
  @DisplayName("Scenario: Commit with deleted lines only (refactoring)")
  void testCommitWithDeletionsOnly() {
    String deletionOnlyPatch =
        "@@ -20,10 +20,7 @@\n"
            + " public void method() {\n"
            + "-   deadCode1();\n"
            + "-   deadCode2();\n"
            + "-   deadCode3();\n"
            + "   actualLogic();\n"
            + " }\n";

    Commit fixCommit = new Commit();
    fixCommit.setSha("fix004");

    FileChange fileChange = new FileChange();
    fileChange.setFileName("src/main/java/Cleanup.java");
    fileChange.setPatch(deletionOnlyPatch);
    fileChange.setChangedLines(new HashSet<>()); // No additions

    fixCommit.setFileChanges(Arrays.asList(fileChange));

    ProjectIssue issue = new ProjectIssue();
    issue.setId(TEST_ISSUE_ID);
    issue.setFixingCommits(Arrays.asList(fixCommit));

    when(projectIssueRepository.findIssueById(REPO_NAME, REPO_OWNER, TEST_ISSUE_ID))
        .thenReturn(issue);

    // Deletions shouldn't match added lines
    assertDoesNotThrow(
        () -> basicBugFinder.findBugIntroducingCommits(REPO_OWNER, REPO_NAME, TEST_ISSUE_ID, 3));
  }

  @Test
  @DisplayName("Scenario: Repository path construction with different owners/repos")
  void testRepositoryPathConstruction() throws Exception {
    java.lang.reflect.Method getRepoPathMethod =
        BasicBugFinder.class.getDeclaredMethod("getRepositoryPath", String.class, String.class);
    getRepoPathMethod.setAccessible(true);

    String result = (String) getRepoPathMethod.invoke(basicBugFinder, "apache", "commons-lang");

    assertTrue(result.contains("apache"));
    assertTrue(result.contains("commons-lang"));
    assertTrue(result.endsWith("apache_commons-lang"));
  }

  @Test
  @DisplayName("Scenario: Parse complex multi-hunk patch")
  void testMultiHunkPatch() throws Exception {
    String multiHunkPatch =
        "--- a/file.java\n"
            + "+++ b/file.java\n"
            + "@@ -10,7 +10,8 @@\n"
            + " int a = 1;\n"
            + "-int b = bug1;  // REMOVED\n"
            + "+int b = fixed1;  // ADDED\n"
            + "+int c = fixed2;  // ADDED\n"
            + " return a + b;\n"
            + "@@ -50,5 +51,5 @@\n"
            + " String result = compute();\n"
            + "-if (result == null) throw error;  // REMOVED\n"
            + "+if (result == null) return empty;  // ADDED\n"
            + " return result;\n";

    java.lang.reflect.Method getChangedLinesMethod =
        BasicBugFinder.class.getDeclaredMethod("getChangedLineNumbers", String.class);
    getChangedLinesMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Set<Integer> result = (Set<Integer>) getChangedLinesMethod.invoke(basicBugFinder, multiHunkPatch);

    assertNotNull(result);
    assertTrue(result.size() >= 3, "Should have at least 3 changed lines");
  }

  @Test
  @DisplayName("Scenario: Handle edge case - empty patch")
  void testEmptyPatchHandling() throws Exception {
    Commit fixCommit = new Commit();
    fixCommit.setSha("fix005");

    FileChange fileChange = new FileChange();
    fileChange.setFileName("empty.txt");
    fileChange.setPatch(""); // Empty patch

    fixCommit.setFileChanges(Arrays.asList(fileChange));

    ProjectIssue issue = new ProjectIssue();
    issue.setId(TEST_ISSUE_ID);
    issue.setFixingCommits(Arrays.asList(fixCommit));

    when(projectIssueRepository.findIssueById(REPO_NAME, REPO_OWNER, TEST_ISSUE_ID))
        .thenReturn(issue);

    // Should handle gracefully
    assertDoesNotThrow(
        () -> basicBugFinder.findBugIntroducingCommits(REPO_OWNER, REPO_NAME, TEST_ISSUE_ID, 2));
  }

  @Test
  @DisplayName("Scenario: Handle malformed patch gracefully")
  void testMalformedPatchHandling() throws Exception {
    String malformedPatch = "This is not a valid diff@@@@\n@@ @@\n+some added line\n";

    Commit fixCommit = new Commit();
    fixCommit.setSha("fix006");

    FileChange fileChange = new FileChange();
    fileChange.setFileName("broken.java");
    fileChange.setPatch(malformedPatch);

    fixCommit.setFileChanges(Arrays.asList(fileChange));

    ProjectIssue issue = new ProjectIssue();
    issue.setId(TEST_ISSUE_ID);
    issue.setFixingCommits(Arrays.asList(fixCommit));

    when(projectIssueRepository.findIssueById(REPO_NAME, REPO_OWNER, TEST_ISSUE_ID))
        .thenReturn(issue);

    // Should not crash on malformed input
    assertDoesNotThrow(
        () -> basicBugFinder.findBugIntroducingCommits(REPO_OWNER, REPO_NAME, TEST_ISSUE_ID, 2));
  }
}

