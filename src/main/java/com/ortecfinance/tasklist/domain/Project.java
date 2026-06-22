package com.ortecfinance.tasklist.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class Project {
    private final String name;
    private final List<Task> tasks = new ArrayList<>();

    public Project(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public Optional<Task> findTask(long id) {
        return tasks.stream().filter(t -> t.getId() == id).findFirst();
    }

    public boolean removeTask(long id) {
        return tasks.removeIf(t -> t.getId() == id);
    }
}

