package com.ortecfinance.tasklist.store;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.repository.ProjectRepository;
import com.ortecfinance.tasklist.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

@Component
@Primary
public class JpaProjectStore implements ProjectStore {
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final EntityManager entityManager;

    public JpaProjectStore(ProjectRepository projectRepository, TaskRepository taskRepository, EntityManager entityManager) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public Collection<Project> findAll() {
        return projectRepository.findAllWithTasks();
    }

    @Override
    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }


    @Override
    public Optional<Project> findByName(String name) {
        return projectRepository.findByName(name);
    }

    @Override
    public boolean existsByName(String name) {
        return projectRepository.existsByName(name);
    }

    @Override
    public void deleteByName(String name) {
        projectRepository.deleteByName(name);
    }

    @Override
    public Task saveTask(Project project, Task task) {
        project.addTask(task);
        return taskRepository.save(task);
    }

    @Override
    public Optional<Task> findTaskById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    @Transactional
    public boolean deleteTaskById(Long id) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            Project project = task.getProject();
            if (project != null) {
                project.removeTask(id);
            }
            taskRepository.delete(task);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void clear() {
        taskRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        entityManager.createNativeQuery("ALTER TABLE tasks ALTER COLUMN id RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE projects ALTER COLUMN id RESTART WITH 1").executeUpdate();
    }

    @Override
    public Collection<Task> findTasksByDeadline(LocalDate deadline) {
        return taskRepository.findByDeadline(deadline);
    }

    @Override
    public void renameProject(Project project, String newName) {
        project.setName(newName);
    }

}
