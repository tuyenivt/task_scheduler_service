package com.example.taskscheduler.mapper;

import com.example.taskscheduler.domain.entity.ScheduledTask;
import com.example.taskscheduler.domain.entity.TaskExecutionLog;
import com.example.taskscheduler.dto.TaskExecutionLogResponse;
import com.example.taskscheduler.dto.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for converting between entities and DTOs
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    /**
     * Convert ScheduledTask entity to TaskResponse DTO
     */
    TaskResponse toResponse(ScheduledTask task);

    /**
     * Convert list of ScheduledTask entities to TaskResponse DTOs
     */
    List<TaskResponse> toResponseList(List<ScheduledTask> tasks);

    /**
     * Convert TaskExecutionLog entity to TaskExecutionLogResponse DTO
     */
    TaskExecutionLogResponse toLogResponse(TaskExecutionLog log);

    /**
     * Convert list of TaskExecutionLog entities to DTOs
     */
    List<TaskExecutionLogResponse> toLogResponses(List<TaskExecutionLog> logs);
}
