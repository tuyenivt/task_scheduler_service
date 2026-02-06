package com.example.taskscheduler.domain.entity;

import com.example.taskscheduler.domain.enums.TaskPriority;
import com.example.taskscheduler.domain.enums.TaskStatus;
import com.example.taskscheduler.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Main entity representing a scheduled task.
 * <p>
 * Supports:
 * - Configurable task types and priorities
 * - Custom metadata/payload for task-specific data
 * - Retry configuration per task
 * - Full execution history tracking
 * - Distributed locking support
 */
@Entity
@Table(name = "scheduled_tasks", indexes = {
        @Index(name = "idx_task_status_scheduled_time", columnList = "status, scheduled_time"),
        @Index(name = "idx_task_type_status", columnList = "task_type, status"),
        @Index(name = "idx_task_reference_id", columnList = "reference_id"),
        @Index(name = "idx_task_locked_by_until", columnList = "locked_by, locked_until"),
        @Index(name = "idx_task_priority_scheduled", columnList = "priority DESC, scheduled_time ASC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Type of task to execute - determines which handler processes it
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 50)
    private TaskType taskType;

    /**
     * Current status in the task lifecycle
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    /**
     * Task priority for execution ordering
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.NORMAL;

    /**
     * External reference ID (e.g., order ID, payment ID)
     * Used to link task to the business entity it operates on
     */
    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;

    /**
     * Secondary reference if needed (e.g., transaction ID for refund)
     */
    @Column(name = "secondary_reference_id", length = 100)
    private String secondaryReferenceId;

    /**
     * Human-readable description of the task
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * JSON payload containing task-specific data
     * Stored as JSONB in PostgreSQL for query capability
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    /**
     * Custom metadata for task control and configuration
     * Can include things like specific API endpoints, headers, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * When the task should be executed
     */
    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    /**
     * Task expiration time - won't execute after this
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Number of execution attempts so far
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum retry attempts allowed (overrides default)
     */
    @Column(name = "max_retries")
    private Integer maxRetries;

    /**
     * Hours to wait before next retry (overrides default)
     */
    @Column(name = "retry_delay_hours")
    private Integer retryDelayHours;

    /**
     * Custom cron expression for recurring tasks
     * If set, task will be rescheduled after completion
     */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    /**
     * Last error message if task failed
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * Stack trace of last error (truncated)
     */
    @Column(name = "last_error_stack_trace", columnDefinition = "TEXT")
    private String lastErrorStackTrace;

    /**
     * Result of last execution (success response, etc.)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_result", columnDefinition = "jsonb")
    private Map<String, Object> executionResult;

    // === Distributed Locking Fields ===

    /**
     * Instance ID that currently holds the lock
     */
    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    /**
     * When the lock expires
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * Version for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    // === Audit Fields ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Total execution duration in milliseconds
     */
    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;

    // === Lifecycle Callbacks ===

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = TaskStatus.PENDING;
        }
        if (this.priority == null) {
            this.priority = TaskPriority.NORMAL;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        if (this.payload == null) {
            this.payload = new HashMap<>();
        }
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // === Helper Methods ===

    /**
     * Get effective max retries (task-specific or default)
     */
    public int getEffectiveMaxRetries(int defaultMaxRetries) {
        return maxRetries != null ? maxRetries : defaultMaxRetries;
    }

    /**
     * Get effective retry delay hours (task-specific or default)
     */
    public int getEffectiveRetryDelayHours(int defaultRetryDelayHours) {
        return retryDelayHours != null ? retryDelayHours : defaultRetryDelayHours;
    }

    /**
     * Check if task can be retried
     */
    public boolean canRetry(int defaultMaxRetries) {
        return retryCount < getEffectiveMaxRetries(defaultMaxRetries);
    }

    /**
     * Check if task is currently locked
     */
    public boolean isLocked() {
        return lockedBy != null && lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Check if task has expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Add or update a metadata entry
     */
    public void putMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Get a metadata value
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key, Class<T> type) {
        if (this.metadata == null) {
            return null;
        }
        var value = this.metadata.get(key);
        return type.isInstance(value) ? (T) value : null;
    }

    /**
     * Add or update a payload entry
     */
    public void putPayload(String key, Object value) {
        if (this.payload == null) {
            this.payload = new HashMap<>();
        }
        this.payload.put(key, value);
    }

    /**
     * Get a payload value
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key, Class<T> type) {
        if (this.payload == null) {
            return null;
        }
        var value = this.payload.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
}
