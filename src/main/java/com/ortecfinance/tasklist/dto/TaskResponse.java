package com.ortecfinance.tasklist.dto;

import java.time.LocalDate;

public record TaskResponse(Long id, String description, boolean done, LocalDate deadline) {
}
