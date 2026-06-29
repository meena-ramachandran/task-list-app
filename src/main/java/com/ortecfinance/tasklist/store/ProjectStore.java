package com.ortecfinance.tasklist.store;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import java.util.*;
import org.springframework.stereotype.Component;


@Component
public class ProjectStore {
    private final Map<String, Project> projects = new LinkedHashMap<>();
    private long lastId = 0;

    public Project save(Project project) {
        projects.put(project.getName(), project);
        return project;
    }

    public List<Project> findAll() {
        return new ArrayList<>(projects.values());
    }

    public Optional<Project> findByName(String name) {
        return Optional.ofNullable(projects.get(name));
    }

    public boolean existsByName(String name) {
        return projects.containsKey(name);
    }

    public void deleteByName(String name) {
        projects.remove(name);
    }

    public Task saveTask(Project project, String description) {
        Task task = new Task(nextId(), description, false, null);
        project.addTask(task);
        return task;
    }

    public Optional<Task> findTaskById(long id) {
        for (Project project : projects.values()) {
            Optional<Task> found = project.findTask(id);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public boolean deleteTaskById(long id) {
        for (Project project : projects.values()) {
            if (project.removeTask(id)) {
                return true;
            }
        }
        return false;
    }

    private long nextId() {
        return ++lastId;
    }

    public void clear() {
        projects.clear();
        lastId = 0;
    }
}
