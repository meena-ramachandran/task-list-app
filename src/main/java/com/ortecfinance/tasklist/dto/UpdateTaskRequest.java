package com.ortecfinance.tasklist.dto;

public record UpdateTaskRequest(
        String description,
        Boolean done,
        String deadline
) {}