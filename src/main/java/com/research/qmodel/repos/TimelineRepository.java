package com.research.qmodel.repos;

import com.research.qmodel.model.Timeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimelineRepository extends JpaRepository<Timeline, Long>  {
    Optional<Timeline> findByRawData(String rawData);
}
