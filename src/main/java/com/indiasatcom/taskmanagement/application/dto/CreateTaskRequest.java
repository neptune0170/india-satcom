package com.indiasatcom.taskmanagement.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank(message = "title is required")
        String title,

        String description,

        TaskStatus status,

        @NotNull(message = "due_date is required")
        @JsonProperty("due_date")
        LocalDate dueDate
) {
}
