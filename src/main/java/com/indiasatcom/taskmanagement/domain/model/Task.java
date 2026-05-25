package com.indiasatcom.taskmanagement.domain.model;

import com.indiasatcom.taskmanagement.domain.exception.InvalidTaskException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Aggregate root for the Task bounded context.
 *
 * Invariants enforced here:
 *   - id is always present
 *   - title is non-blank
 *   - dueDate is non-null and not in the past
 *   - status defaults to PENDING when not supplied
 */
public class Task {

    private final TaskId id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDate dueDate;

    private Task(TaskId id, String title, String description, TaskStatus status, LocalDate dueDate) {
        this.id = Objects.requireNonNull(id, "id is required");
        setTitle(title);
        setDueDate(dueDate);
        this.description = description;
        this.status = status != null ? status : TaskStatus.PENDING;
    }

    public static Task create(String title, String description, TaskStatus status, LocalDate dueDate) {
        return new Task(TaskId.generate(), title, description, status, dueDate);
    }

    public static Task rehydrate(TaskId id, String title, String description, TaskStatus status, LocalDate dueDate) {
        return new Task(id, title, description, status, dueDate);
    }

    public void updateTitle(String newTitle) {
        setTitle(newTitle);
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }

    public void updateStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidTaskException("status must not be null");
        }
        this.status = newStatus;
    }

    public void updateDueDate(LocalDate newDueDate) {
        setDueDate(newDueDate);
    }

    private void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("title must not be blank");
        }
        this.title = title.trim();
    }

    private void setDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new InvalidTaskException("due_date must not be null");
        }
        if (dueDate.isBefore(LocalDate.now())) {
            throw new InvalidTaskException("due_date must not be in the past");
        }
        this.dueDate = dueDate;
    }

    public TaskId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
