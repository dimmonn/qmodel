package com.research.qmodel.repos;

import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import com.research.qmodel.model.CommitID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface CommitRepository extends JpaRepository<Commit, CommitID> {
}