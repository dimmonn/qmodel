package com.research.qmodel.repos;

import com.research.qmodel.model.Action;
import com.research.qmodel.model.ProjectID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ActionsRepository extends JpaRepository<Action, ProjectID> {
}