package com.ortecfinance.tasklist.service;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.exception.ProjectAlreadyExistsException;
import com.ortecfinance.tasklist.exception.ProjectNotFoundException;
import com.ortecfinance.tasklist.exception.TaskNotFoundException;
import com.ortecfinance.tasklist.store.ProjectStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ProjectService {
    private final ProjectStore store;

    public ProjectService(ProjectStore store) {
        this.store = store;
    }

    public Project addProject(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }
        if (store.existsByName(name)) {
            throw new ProjectAlreadyExistsException(name);
        }
        return store.save(new Project(name));
    }

    public Project getProject(String name){
        return store.findByName(name).orElseThrow(() -> new ProjectNotFoundException(name));
    }

    public void removeProject(String name) {
        if (!store.existsByName(name)) {
            throw new ProjectNotFoundException(name);
        }
        store.deleteByName(name);
    }

    public Collection<Project> getProjects() {
        return new ArrayList<>(store.findAll());
    }

    public Task addTask(String projectName, String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be empty.");
        }
        Project project = store.findByName(projectName).orElseThrow(() -> new ProjectNotFoundException(projectName) );
        return store.saveTask(project, description);
    }

    public Task setDeadline(long taskId, LocalDate deadline) {
        Task task = store.findTaskById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setDeadline(deadline);
        return task;
    }

    public Task setDone(long taskId, boolean done) {
        Task task = store.findTaskById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setDone(done);
        return task;
    }

    public Task setDone(String projectName, long taskId, boolean done) {
        Project project =store.findByName(projectName).orElseThrow(() -> new ProjectNotFoundException(projectName));
        Task task = project.findTask(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setDone(done);
        return task;
    }

    public void removeTask(long taskId) {
        Task task = store.findTaskById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        store.deleteTaskById(taskId);
    }

    public Task setDeadline(String projectName, long taskId, LocalDate deadline) {
        Project project =store.findByName(projectName).orElseThrow(() -> new ProjectNotFoundException(projectName));
        Task task = project.findTask(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setDeadline(deadline);
        return task;
    }

    public Map<String, List<Task>> getTasksForToday(LocalDate today) {
        Map<String, List<Task>> result = new LinkedHashMap<>();
        for (Project project : store.findAll()) {
            List<Task> task = new ArrayList<>();
            for (Task t : project.getTasks()) {
                if (t != null && today != null && today.equals(t.getDeadline())) {
                    task.add(t);
                }
            }
            if (!task.isEmpty()) {
                result.put(project.getName(), task);
            }
        }
        return result;
    }

    public Map<Optional<LocalDate>, Map<String, List<Task>>> getTasksGroupedByDeadline() {
        Map<Optional<LocalDate>, Map<String, List<Task>>> grouped = new TreeMap<>(
                Comparator.comparing(d -> d.orElse(LocalDate.MAX))
        );

        for (Project project : store.findAll()) {
            for (Task task : project.getTasks()) {
                grouped.computeIfAbsent(Optional.ofNullable(task.getDeadline()), d -> new LinkedHashMap<>())
                        .computeIfAbsent(project.getName(), p -> new ArrayList<>())
                        .add(task);
            }
        }
        return grouped;
    }
}
