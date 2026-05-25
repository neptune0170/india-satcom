package com.indiasatcom.taskmanagement.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.indiasatcom.taskmanagement.domain.model.Task;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;

import java.time.LocalDate;

public record TaskResponse(
        String id,
        String title,
        String description,
        TaskStatus status,
        @JsonProperty("due_date") LocalDate dueDate
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId().value(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate()
        );
    }
}
