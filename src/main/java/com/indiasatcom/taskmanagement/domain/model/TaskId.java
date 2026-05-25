package com.indiasatcom.taskmanagement.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object that wraps the unique identifier of a Task.
 * Using a dedicated type (rather than raw String) prevents
 * accidental mixing of identifiers across aggregates.
 */
public final class TaskId {

    private final String value;

    private TaskId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaskId value must not be blank");
        }
        this.value = value;
    }

    public static TaskId generate() {
        return new TaskId(UUID.randomUUID().toString());
    }

    public static TaskId of(String value) {
        return new TaskId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
