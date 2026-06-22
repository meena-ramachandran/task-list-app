package com.ortecfinance.tasklist.store;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ProjectStore {
    private final Map<String, Project> projects = new LinkedHashMap<>();
    private long lastId = 0;

    public Project save(Project project) {
        projects.put(project.getName(), project);
        return project;
    }

    public Optional<Project> findByName(String name) {
        return Optional.ofNullable(projects.get(name));
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
}
