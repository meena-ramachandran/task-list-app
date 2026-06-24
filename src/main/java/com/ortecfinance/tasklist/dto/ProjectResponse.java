package com.ortecfinance.tasklist.dto;


import java.util.List;

public record ProjectResponse(Long id, String name, List<TaskResponse> tasks) {
}
