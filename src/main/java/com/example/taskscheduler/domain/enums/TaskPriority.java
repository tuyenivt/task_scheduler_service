package com.example.taskscheduler.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Task priority levels for execution ordering.
 * Higher priority tasks are executed first.
 */
@Getter
@RequiredArgsConstructor
public enum TaskPriority {

    /**
     * Lowest priority - executed when no higher priority tasks exist
     */
    LOW(1, "Low"),

    /**
     * Standard priority for normal operations
     */
    NORMAL(5, "Normal"),

    /**
     * Elevated priority for important tasks
     */
    HIGH(8, "High"),

    /**
     * Highest priority - execute immediately before all others
     */
    CRITICAL(10, "Critical");

    private final int value;
    private final String displayName;

    /**
     * Find TaskPriority by its numeric value
     */
    public static TaskPriority fromValue(int value) {
        for (var priority : values()) {
            if (priority.getValue() == value) {
                return priority;
            }
        }
        // Default to NORMAL if unknown value
        return NORMAL;
    }
}
