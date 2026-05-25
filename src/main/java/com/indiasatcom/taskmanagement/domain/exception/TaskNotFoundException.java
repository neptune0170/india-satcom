package com.indiasatcom.taskmanagement.domain.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String id) {
        super("Task with id '" + id + "' was not found");
    }
}
