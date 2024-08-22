package com.research.qmodel.repos;

import com.research.qmodel.model.Commit;
import com.research.qmodel.model.IssueID;
import com.research.qmodel.model.ProjectIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectIssueRepository extends JpaRepository<ProjectIssue, IssueID> {
  @Query(
      "SELECT DISTINCT pi "
          + "FROM ProjectIssue pi "
          + "JOIN pi.timeLine t "
          + "JOIN t.pullIds p "
          + "WHERE p = :pullId")
  List<ProjectIssue> findRelatedIssuesByPullId(@Param("pullId") Long pullId);

  @Query(
      "SELECT pi "
          + "FROM ProjectIssue pi "
          + "WHERE pi.projectName = :repoName AND pi.projectOwner = :repoOwner AND pi.fixPr > 0")
  List<ProjectIssue> finAllFixedIssues(
      @Param("repoName") String repoName, @Param("repoOwner") String repoOwner);

  @Query(
      "SELECT pi "
          + "FROM ProjectIssue pi "
          + "WHERE pi.projectName = :repoName "
          + "AND pi.projectOwner = :repoOwner "
          + "AND pi.id = :id "
          + "AND pi.fixPr > 0")
  ProjectIssue findIssueById(
      @Param("repoName") String repoName,
      @Param("repoOwner") String repoOwner,
      @Param("id") Long id);

  @Query("SELECT pi FROM ProjectIssue pi WHERE pi.projectOwner = :owner AND pi.projectName = :repo")
  List<ProjectIssue> findByProject(@Param("owner") String owner, @Param("repo") String repo);
}
