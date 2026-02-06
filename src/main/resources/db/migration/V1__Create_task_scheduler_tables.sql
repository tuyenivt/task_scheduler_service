-- V1__Create_task_scheduler_tables.sql
-- Initial schema for Task Scheduler Service

-- Enable UUID extension if not already enabled
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- Main scheduled tasks table
CREATE TABLE scheduled_tasks
(
    id                     UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),

    -- Task identification
    task_type              VARCHAR(50)              NOT NULL,
    reference_id           VARCHAR(100)             NOT NULL,
    secondary_reference_id VARCHAR(100),
    description            VARCHAR(500),

    -- Status and priority
    status                 VARCHAR(30)              NOT NULL DEFAULT 'PENDING',
    priority               VARCHAR(20)              NOT NULL DEFAULT 'NORMAL',

    -- Task data (JSONB for flexible schema and query capability)
    payload                JSONB                             DEFAULT '{}',
    metadata               JSONB                             DEFAULT '{}',

    -- Scheduling
    scheduled_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at             TIMESTAMP WITH TIME ZONE,
    cron_expression        VARCHAR(100),

    -- Retry configuration
    retry_count            INTEGER                  NOT NULL DEFAULT 0,
    max_retries            INTEGER,
    retry_delay_hours      INTEGER,

    -- Execution results
    last_error             TEXT,
    last_error_stack_trace TEXT,
    execution_result       JSONB,

    -- Distributed locking
    locked_by              VARCHAR(100),
    locked_until           TIMESTAMP WITH TIME ZONE,
    version                BIGINT                   NOT NULL DEFAULT 0,

    -- Audit fields
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by             VARCHAR(100),
    started_at             TIMESTAMP WITH TIME ZONE,
    completed_at           TIMESTAMP WITH TIME ZONE,
    execution_duration_ms  BIGINT,

    -- Constraints
    CONSTRAINT chk_status CHECK (status IN (
                                            'PENDING', 'SCHEDULED', 'PROCESSING', 'COMPLETED',
                                            'FAILED', 'RETRY_PENDING', 'MAX_RETRIES_EXCEEDED',
                                            'CANCELLED', 'PAUSED', 'EXPIRED', 'DEAD_LETTER'
        )),
    CONSTRAINT chk_task_type CHECK (task_type IN (
                                                  'ORDER_CANCEL', 'PAYMENT_REFUND', 'PAYMENT_PARTIAL_REFUND',
                                                  'PAYMENT_VOID', 'WEBHOOK_NOTIFICATION', 'CUSTOM'
        )),
    CONSTRAINT chk_priority CHECK (priority IN (
                                                'LOW', 'NORMAL', 'HIGH', 'CRITICAL'
        ))
);

-- Indexes for efficient task retrieval
-- Main index for polling: status + scheduled time
CREATE INDEX idx_task_status_scheduled_time ON scheduled_tasks (status, scheduled_time) WHERE status IN ('PENDING', 'SCHEDULED', 'FAILED', 'RETRY_PENDING');

-- Index for task type filtering
CREATE INDEX idx_task_type_status ON scheduled_tasks (task_type, status);

-- Index for reference ID lookups (order/payment ID)
CREATE INDEX idx_task_reference_id ON scheduled_tasks (reference_id);

-- Index for lock management
CREATE INDEX idx_task_locked ON scheduled_tasks (locked_by, locked_until) WHERE locked_by IS NOT NULL;

-- Index for priority-based scheduling
CREATE INDEX idx_task_priority_scheduled ON scheduled_tasks (priority DESC, scheduled_time ASC) WHERE status IN ('PENDING', 'SCHEDULED', 'RETRY_PENDING');

-- Partial index for active tasks only
CREATE INDEX idx_task_active ON scheduled_tasks (status, task_type, reference_id) WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'EXPIRED');

-- GIN index for JSONB payload queries
CREATE INDEX idx_task_payload ON scheduled_tasks USING GIN (payload jsonb_path_ops);

-- GIN index for JSONB metadata queries
CREATE INDEX idx_task_metadata ON scheduled_tasks USING GIN (metadata jsonb_path_ops);


-- Task execution logs table
CREATE TABLE task_execution_logs
(
    id                UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),

    -- Reference to task
    task_id           UUID                     NOT NULL,
    attempt_number    INTEGER                  NOT NULL,

    -- Execution details
    status            VARCHAR(30)              NOT NULL,
    executor_instance VARCHAR(100),

    -- Timing
    started_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at      TIMESTAMP WITH TIME ZONE,
    duration_ms       BIGINT,

    -- Result
    success           BOOLEAN                  NOT NULL DEFAULT FALSE,
    error_message     TEXT,
    error_stack_trace TEXT,
    error_type        VARCHAR(200),
    http_status_code  INTEGER,

    -- Request/Response data
    request_payload   JSONB,
    response_payload  JSONB,

    -- Notes
    notes             TEXT,

    -- Audit
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key (not enforced for performance, cleaned up via batch jobs)
    -- FOREIGN KEY (task_id) REFERENCES scheduled_tasks(id) ON DELETE CASCADE

    CONSTRAINT chk_log_status CHECK (status IN (
                                                'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED',
                                                'RETRY_PENDING', 'MAX_RETRIES_EXCEEDED', 'CANCELLED'
        ))
);

-- Indexes for execution logs
CREATE INDEX idx_exec_log_task_id ON task_execution_logs (task_id);
CREATE INDEX idx_exec_log_executed_at ON task_execution_logs (started_at);
CREATE INDEX idx_exec_log_status ON task_execution_logs (status);
CREATE INDEX idx_exec_log_task_attempt ON task_execution_logs (task_id, attempt_number);


-- ShedLock table for distributed scheduler locking
CREATE TABLE shedlock
(
    name       VARCHAR(64)              NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by  VARCHAR(255)             NOT NULL
);


-- Function to update updated_at timestamp
CREATE
OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at
= NOW();
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

-- Trigger to auto-update updated_at
CREATE TRIGGER trigger_update_scheduled_tasks_updated_at
    BEFORE UPDATE
    ON scheduled_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


-- Comments for documentation
COMMENT
ON TABLE scheduled_tasks IS 'Main table for scheduled tasks in the task scheduler service';
COMMENT
ON TABLE task_execution_logs IS 'Execution history for each task attempt';
COMMENT
ON TABLE shedlock IS 'ShedLock table for distributed scheduler coordination';

COMMENT
ON COLUMN scheduled_tasks.payload IS 'Task-specific data used by handlers (JSONB)';
COMMENT
ON COLUMN scheduled_tasks.metadata IS 'Control metadata for task execution (JSONB)';
COMMENT
ON COLUMN scheduled_tasks.locked_by IS 'Instance ID that holds the lock';
COMMENT
ON COLUMN scheduled_tasks.locked_until IS 'Lock expiration time for distributed processing';
COMMENT
ON COLUMN scheduled_tasks.version IS 'Optimistic locking version number';
