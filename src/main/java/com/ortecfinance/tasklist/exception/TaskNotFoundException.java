package com.ortecfinance.tasklist.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(long taskId) {
        super("Could not find a task with an ID of %d.".formatted(taskId));
    }
}
