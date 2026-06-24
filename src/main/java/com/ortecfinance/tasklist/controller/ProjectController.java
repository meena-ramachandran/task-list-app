package com.ortecfinance.tasklist.controller;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.dto.*;
import com.ortecfinance.tasklist.mapper.ProjectMapper;
import com.ortecfinance.tasklist.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@Tag(name = "Projects", description = "Endpoints for project and task management")
public class ProjectController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "Get all Projects", description = "Retrieves all projects")
    public List<ProjectResponse> getProjects() {
        return ProjectMapper.toProjectResponseList(projectService.getProjects());
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get a Project by ID", description = "Retrieves a project by its unique ID")
    public ProjectResponse getProject(@PathVariable Long projectId) {
        return ProjectMapper.toProjectResponse(projectService.getProject(projectId));
    }

    @PostMapping
    @Operation(summary = "Create a new project", description = "Creates a project with a unique name")
    public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request) {
        Project project = projectService.addProject(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.toProjectResponse(project));
    }

    @PatchMapping("/{projectId}")
    @Operation(summary = "Rename a project", description = "Renames an existing project")
    public ResponseEntity<ProjectResponse> renameProject(
            @PathVariable Long projectId,
            @RequestBody RenameProjectRequest request) {

        Project project = projectService.renameProject(projectId, request.name());
        return ResponseEntity.ok(ProjectMapper.toProjectResponse(project));
    }


    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete a project", description = "Deletes a project and all its tasks")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.removeProject(projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/tasks")
    @Operation(summary = "Create a new task", description = "Creates a task within a project")
    public ResponseEntity<TaskResponse> createTask(@PathVariable Long projectId,
            @RequestBody CreateTaskRequest request) {
        Task task = projectService.addTask(projectId, new Task(request.description(), false, null, null));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.toTaskResponse(task));
    }

    @GetMapping("/view_by_deadline")
    @Operation(summary = "View tasks grouped by deadline", description = "Retrieves tasks grouped by deadline")
    public Map<String, Map<String, List<TaskResponse>>> viewByDeadline() {
        return ProjectMapper.toDeadlineGroupsMap(projectService.getTasksGroupedByDeadline(), FORMATTER);
    }

    @GetMapping("/today")
    @Operation(summary = "View tasks for today", description = "Retrieves tasks for today")
    public Map<String, List<TaskResponse>> getTasksForToday() {
        return ProjectMapper.toTasksByProject(projectService.getTasksForToday(LocalDate.now()));
    }

    @PatchMapping("/{projectId}/tasks/{taskId}")
    @Operation(summary = "Update a task", description = "Updates a task in a project")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request) {
        Task task = projectService.updateTask(projectId, taskId, request);
        return ResponseEntity.ok(ProjectMapper.toTaskResponse(task));
    }

    @DeleteMapping("/{projectId}/tasks/{taskId}")
    @Operation(summary = "Delete a task", description = "Deletes a task from a project")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        projectService.removeTask(projectId, taskId);
        return ResponseEntity.noContent().build();
    }
}
