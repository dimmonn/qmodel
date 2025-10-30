package com.research.qmodel.repos;

import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CommitRepository extends JpaRepository<Commit, CommitID> {
  @Query("SELECT c FROM Commit c " +
      "JOIN c.aGraph g " +
      "JOIN g.project p " +
      "WHERE p.projectName = :projectName AND p.projectOwner = :ownerName")
  List<Commit> findAllCommitByProjectNameAndOwner(@Param("projectName") String projectName,
      @Param("ownerName") String ownerName);

    @Modifying
    @Query("""
       update Commit c set
         c.inDegree = :inDeg,
         c.outDegree = :outDeg,
         c.mergeCount = :mergeCount,
         c.minDepthOfCommitHistory = :minDepth,
         c.maxDepthOfCommitHistory = :maxDepth,
         c.numberOfBranches = :numBranches,
         c.averageDegree = :avgDeg
       where c.sha = :sha
       """)
    void updateGraphMetrics(@Param("sha") String sha,
                            @Param("inDeg") int inDeg,
                            @Param("outDeg") int outDeg,
                            @Param("mergeCount") int mergeCount,
                            @Param("minDepth") int minDepth,
                            @Param("maxDepth") int maxDepth,
                            @Param("numBranches") int numBranches,
                            @Param("avgDeg") double avgDeg);

}
