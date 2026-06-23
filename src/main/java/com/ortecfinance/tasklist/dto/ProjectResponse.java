package com.ortecfinance.tasklist.dto;


import java.util.List;

public record ProjectResponse(String name, List<TaskResponse> tasks) {
}
