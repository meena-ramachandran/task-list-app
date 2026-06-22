package com.ortecfinance.tasklist.domain;

import java.time.LocalDate;

public final class Task {
    private final long id;
    private final String description;
    private boolean done;
    private LocalDate deadline;

    public Task(long id, String description, boolean done, LocalDate deadline) {
        this.id = id;
        this.description = description;
        this.done = done;
        this.deadline = deadline;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }


    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
