package com.ortecfinance.tasklist.service;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.dto.UpdateTaskRequest;
import com.ortecfinance.tasklist.exception.ProjectAlreadyExistsException;
import com.ortecfinance.tasklist.exception.ProjectNotFoundException;
import com.ortecfinance.tasklist.exception.TaskNotFoundException;
import com.ortecfinance.tasklist.store.ProjectStore;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectStore store;

    public ProjectService(ProjectStore store) {
        this.store = store;
    }

    @Transactional
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

    @Transactional
    public void removeProject(String name) {
        if (!store.existsByName(name)) {
            throw new ProjectNotFoundException(name);
        }
        store.deleteByName(name);
    }

    public Collection<Project> getProjects() {
        return new ArrayList<>(store.findAll());
    }

    @Transactional
    public Task addTask(String projectName, Task task) {
        if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be empty.");
        }
        Project project = store.findByName(projectName).orElseThrow(() -> new ProjectNotFoundException(projectName) );
        return store.saveTask(project, task);
    }

    @Transactional
    public Task setDeadline(long taskId, LocalDate deadline) {
        Task task = store.findTaskById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        task.setDeadline(deadline);
        return task;
    }

    private Task markTaskDone(Task task, boolean done) {
        task.setDone(done);
        return task;
    }

    private Task setTaskDeadline(Task task, LocalDate deadline) {
        task.setDeadline(deadline);
        return task;
    }

    @Transactional
    public Task setDone(long taskId, boolean done) {
        Task task = store.findTaskById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        return this.markTaskDone(task, done);
    }

    @Transactional
    public Task setDone(String projectName, long taskId, boolean done) {
        Project project =store.findByName(projectName).orElseThrow(() -> new ProjectNotFoundException(projectName));
        Task task = project.findTask(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        return this.markTaskDone(task, done);
    }

    @Transactional
    public void removeTask(long taskId) {
        store.findTaskById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        store.deleteTaskById(taskId);
    }

    @Transactional
    public Task updateTask(String projectName, Long taskId, UpdateTaskRequest request) {
        Project project = store.findByName(projectName)
                .orElseThrow(() -> new ProjectNotFoundException(projectName));

        Task task = project.findTask(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (request.description() != null) {
            if (request.description().trim().isEmpty()) {
                throw new IllegalArgumentException("Task description cannot be empty.");
            }
            task.setDescription(request.description());
        }

        if (request.done() != null) {
            task.setDone(request.done());
        }

        if (request.deadline() != null) { // Field was sent in request
            String val = request.deadline().trim();
            if (val.isEmpty() || val.equalsIgnoreCase("null")) {
                this.setTaskDeadline(task, null);
            } else {
                this.setTaskDeadline(task, parseDate(val));
            }
        }

        return task;
    }

    @Transactional
    public void removeTask(String projectName, Long taskId) {
        Project project = store.findByName(projectName).orElseThrow(() -> new ProjectNotFoundException(projectName));
        project.findTask(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        project.removeTask(taskId);
        store.deleteTaskById(taskId);
    }



    @Transactional
    public Task setDeadline(String projectName, long taskId, LocalDate deadline) {
        Project project =store.findByName(projectName).orElseThrow(() -> new ProjectNotFoundException(projectName));
        Task task = project.findTask(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        return this.setTaskDeadline(task, deadline);
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

    @Transactional
    public Project renameProject(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }

        Project project = store.findByName(oldName)
                .orElseThrow(() -> new ProjectNotFoundException(oldName));

        // If renaming to a different name, verify the new name is unique
        if (!oldName.equalsIgnoreCase(newName) && store.existsByName(newName)) {
            throw new ProjectAlreadyExistsException(newName);
        }

        store.renameProject(project, newName);
        return project;
    }

    public Project getProject(Long id) {
        return store.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    @Transactional
    public void removeProject(Long id) {
        Project project = store.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
        store.deleteByName(project.getName());
    }

    @Transactional
    public Task addTask(Long projectId, Task task) {
        if (task.getDescription() == null || task.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be empty.");
        }
        Project project = store.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        return store.saveTask(project, task);
    }

    @Transactional
    public Task updateTask(Long projectId, Long taskId, UpdateTaskRequest request) {
        Project project = store.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Task task = project.findTask(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (request.description() != null) {
            if (request.description().trim().isEmpty()) {
                throw new IllegalArgumentException("Task description cannot be empty.");
            }
            task.setDescription(request.description());
        }

        if (request.done() != null) {
            this.markTaskDone(task, request.done());
        }

        if (request.deadline() != null) {
            String val = request.deadline().trim();
            if (val.isEmpty() || val.equalsIgnoreCase("null")) {
                this.setTaskDeadline(task, null);
            } else {
                this.setTaskDeadline(task, parseDate(val));
            }
        }

        return task;
    }

    @Transactional
    public void removeTask(Long projectId, Long taskId) {
        Project project = store.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        project.findTask(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        project.removeTask(taskId);
        store.deleteTaskById(taskId);
    }

    @Transactional
    public Project renameProject(Long id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }

        Project project = store.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        if (!project.getName().equalsIgnoreCase(newName) && store.existsByName(newName)) {
            throw new ProjectAlreadyExistsException(newName);
        }

        store.renameProject(project, newName);
        return project;
    }

    private LocalDate parseDate(String val) {
        try {
            return LocalDate.parse(val, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(val);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid date format. Expected dd-MM-yyyy or yyyy-MM-dd");
            }
        }
    }

}
