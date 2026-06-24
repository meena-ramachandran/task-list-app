package com.ortecfinance.tasklist.store;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class InMemoryProjectStore implements ProjectStore {
    private final Map<String, Project> projects = new LinkedHashMap<>();
    private long lastProjectId = 0;
    private long lastTaskId = 0;

    @Override
    public Project save(Project project) {
        if (project.getId() == null) {
            setProjectId(project, ++lastProjectId);
        }
        projects.put(project.getName(), project);
        return project;
    }

    @Override
    public Collection<Project> findAll() {
        return new ArrayList<>(projects.values());
    }

    @Override
    public Optional<Project> findByName(String name) {
        return Optional.ofNullable(projects.get(name));
    }

    @Override
    public boolean existsByName(String name) {
        return projects.containsKey(name);
    }

    @Override
    public void deleteByName(String name) {
        projects.remove(name);
    }

    @Override
    public Task saveTask(Project project, Task task) {
        if (task.getId() == null) {
            setTaskId(task, ++lastTaskId);
        }
        project.addTask(task);
        return task;
    }

    @Override
    public Optional<Task> findTaskById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Project project : projects.values()) {
            Optional<Task> found = project.findTask(id);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean deleteTaskById(Long id) {
        if (id == null) {
            return false;
        }
        for (Project project : projects.values()) {
            if (project.removeTask(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Task> findTasksByDeadline(LocalDate deadline) {
        List<Task> result = new ArrayList<>();
        for (Project project : projects.values()) {
            for (Task task : project.getTasks()) {
                if (deadline != null && deadline.equals(task.getDeadline())) {
                    result.add(task);
                }
            }
        }
        return result;
    }

    @Override
    public void clear() {
        projects.clear();
        lastProjectId = 0;
        lastTaskId = 0;
    }

    @Override
    public Optional<Project> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return projects.values().stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst();
    }

    private void setProjectId(Project project, Long id) {
        try {
            java.lang.reflect.Field idField = Project.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(project, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set project ID via reflection", e);
        }
    }

    private void setTaskId(Task task, Long id) {
        try {
            java.lang.reflect.Field idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set task ID via reflection", e);
        }
    }

    @Override
    public void renameProject(Project project, String newName) {
        projects.remove(project.getName());
        project.setName(newName);
        projects.put(newName, project);
    }

}
