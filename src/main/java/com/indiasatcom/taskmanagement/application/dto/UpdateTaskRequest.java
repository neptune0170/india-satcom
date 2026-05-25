package com.indiasatcom.taskmanagement.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;

import java.time.LocalDate;

/**
 * All fields are optional; only non-null values are applied to the target task.
 */
public record UpdateTaskRequest(
        String title,
        String description,
        TaskStatus status,
        @JsonProperty("due_date") LocalDate dueDate
) {
}
