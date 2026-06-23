package com.ortecfinance.tasklist.exception;

public class ProjectAlreadyExistsException extends RuntimeException {
    public ProjectAlreadyExistsException(String projectName) {
        super("A project with the name " + projectName + " already exists.");
    }
}

