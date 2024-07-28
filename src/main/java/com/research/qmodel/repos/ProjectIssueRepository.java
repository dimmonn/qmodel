package com.research.qmodel.repos;

import com.research.qmodel.model.IssueID;
import com.research.qmodel.model.ProjectIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectIssueRepository extends JpaRepository<ProjectIssue, IssueID> {
    @Query("SELECT DISTINCT pi " +
            "FROM ProjectIssue pi " +
            "JOIN pi.timeLine t " +
            "JOIN t.pullIds p " +
            "WHERE p = :pullId")
    List<ProjectIssue> findRelatedIssuesByPullId(@Param("pullId") Long pullId);

}