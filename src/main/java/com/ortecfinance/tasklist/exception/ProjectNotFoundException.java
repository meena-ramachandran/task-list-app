package com.ortecfinance.tasklist.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String projectName) {
        super("Could not find a project with the name %s.".formatted(projectName));
    }

    public ProjectNotFoundException(Long projectId) {
        super("Could not find a project with an ID of %d.".formatted(projectId));
    }
}