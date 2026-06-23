package com.ortecfinance.tasklist.dto;

import java.time.LocalDate;

public record TaskResponse(long id, String description, boolean done, LocalDate deadline) {
}
