package com.ortecfinance.tasklist.store;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface ProjectStore {
    Project save(Project project);
    Collection<Project> findAll();
    Optional<Project> findByName(String name);
    Optional<Project> findById(Long id);
    boolean existsByName(String name);
    void deleteByName(String name);
    void renameProject(Project project, String newName);


    Task saveTask(Project project, Task task);
    Optional<Task> findTaskById(Long id);
    boolean deleteTaskById(Long id);
    void clear();
    Collection<Task> findTasksByDeadline(LocalDate deadline);


}
