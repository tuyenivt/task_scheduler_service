package com.example.taskscheduler.domain.repository;

import com.example.taskscheduler.domain.entity.TaskExecutionLog;
import com.example.taskscheduler.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TaskExecutionLog entity
 */
@Repository
public interface TaskExecutionLogRepository extends JpaRepository<TaskExecutionLog, UUID> {

    /**
     * Find all execution logs for a task, ordered by attempt number
     */
    List<TaskExecutionLog> findByTaskIdOrderByAttemptNumberDesc(UUID taskId);

    /**
     * Find execution logs within a time range
     */
    Page<TaskExecutionLog> findByStartedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * Find failed execution logs
     */
    Page<TaskExecutionLog> findBySuccessFalse(Pageable pageable);

    /**
     * Find execution logs by status
     */
    Page<TaskExecutionLog> findByStatus(TaskStatus status, Pageable pageable);

    /**
     * Count executions by instance
     */
    @Query("""
            SELECT el.executorInstance, COUNT(el)
            FROM TaskExecutionLog el
            WHERE el.startedAt >= :since
            GROUP BY el.executorInstance
            """)
    List<Object[]> countExecutionsByInstance(@Param("since") Instant since);

    /**
     * Get average execution duration by task type
     */
    @Query(value = """
            SELECT t.task_type, AVG(el.duration_ms) as avg_duration
            FROM task_execution_logs el
            JOIN scheduled_tasks t ON el.task_id = t.id
            WHERE el.started_at >= :since AND el.duration_ms IS NOT NULL
            GROUP BY t.task_type
            """, nativeQuery = true)
    List<Object[]> getAverageExecutionDurationByType(@Param("since") Instant since);

    /**
     * Delete old execution logs (for cleanup)
     */
    @Modifying
    @Query("""
            DELETE FROM TaskExecutionLog el
            WHERE el.createdAt < :cutoff
            """)
    int deleteOldLogs(@Param("cutoff") Instant cutoff);

    /**
     * Find latest execution log for a task
     */
    @Query("""
            SELECT el FROM TaskExecutionLog el
            WHERE el.taskId = :taskId
            ORDER BY el.attemptNumber DESC
            LIMIT 1
            """)
    TaskExecutionLog findLatestByTaskId(@Param("taskId") UUID taskId);

    /**
     * Get error distribution for analysis
     */
    @Query("""
            SELECT el.errorType, COUNT(el)
            FROM TaskExecutionLog el
            WHERE el.success = false
              AND el.startedAt >= :since
              AND el.errorType IS NOT NULL
            GROUP BY el.errorType
            ORDER BY COUNT(el) DESC
            """)
    List<Object[]> getErrorDistribution(@Param("since") Instant since);
}
