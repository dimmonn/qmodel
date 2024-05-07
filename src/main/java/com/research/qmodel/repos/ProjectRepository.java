package com.research.qmodel.repos;

import com.research.qmodel.model.Project;
import com.research.qmodel.model.ProjectID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends CrudRepository<Project, ProjectID> {
    Project findByOwnerAndProjectName(String owner, String projectName);
}