package com.first.gateway.repository;

import com.first.gateway.domain.entity.AsyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AsyncTaskRepository extends JpaRepository<AsyncTask, Long> {

    List<AsyncTask> findByTaskTypeAndStatusOrderByCreatedAtAsc(String taskType, String status);

    List<AsyncTask> findByStatusAndStartedAtBefore(String status, Instant cutoff);

    @Modifying
    @Query("""
        UPDATE AsyncTask t SET t.status = 'RUNNING', t.startedAt = :now, t.version = t.version + 1
        WHERE t.id = :id AND t.status = 'PENDING' AND t.version = :version
        """)
    int claimTask(@Param("id") Long id, @Param("version") int version, @Param("now") Instant now);

    int deleteByStatusAndFinishedAtBefore(String status, Instant cutoff);
}
