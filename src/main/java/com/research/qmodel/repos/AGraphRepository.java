package com.research.qmodel.repos;

import com.research.qmodel.model.AGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AGraphRepository extends JpaRepository<AGraph, Long> {

    @Query(value = "SELECT * FROM agraph WHERE project_project_owner = :repoOwner AND project_project_name = :repoProjectName", nativeQuery = true)
    Optional<AGraph> findByRepoOwnerAndRepoProjectName(@Param("repoOwner") String repoOwner, @Param("repoProjectName") String repoProjectName);

}