package com.ortecfinance.tasklist.controller;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.dto.CreateProjectRequest;
import com.ortecfinance.tasklist.dto.CreateTaskRequest;
import com.ortecfinance.tasklist.dto.ProjectResponse;
import com.ortecfinance.tasklist.dto.TaskResponse;
import com.ortecfinance.tasklist.mapper.ProjectMapper;
import com.ortecfinance.tasklist.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request) {
        Project project = projectService.addProject(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.toProjectResponse(project));
    }

    @GetMapping
    public List<ProjectResponse> getProjects() {
        return ProjectMapper.toProjectResponseList(projectService.getProjects());
    }

    @PostMapping("/{projectName}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable String projectName,
            @RequestBody CreateTaskRequest request) {

        Task task = projectService.addTask(projectName, request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.toTaskResponse(task));
    }

    @PutMapping(value = "/{projectName}/tasks/{taskId}", params = "deadline")
    public ResponseEntity<TaskResponse> updateDeadline(
            @PathVariable String projectName,
            @PathVariable long taskId,
            @RequestParam(value = "deadline", required = false) String deadline) {
        LocalDate parsedDeadline = null;
        if (deadline != null && !deadline.trim().isEmpty() && !deadline.equalsIgnoreCase("null")) {
            try {
                parsedDeadline = LocalDate.parse(deadline, FORMATTER);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format. Expected dd-MM-yyyy");
            }
        }
        Task task = projectService.setDeadline(taskId, parsedDeadline);
        return ResponseEntity.ok(ProjectMapper.toTaskResponse(task));
    }

    @PutMapping(value = "/{projectName}/tasks/{taskId}", params = "done")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable String projectName,
            @PathVariable long taskId,
            @RequestParam("done") boolean done) {
        Task task = projectService.setDone(taskId, done);
        return ResponseEntity.ok(ProjectMapper.toTaskResponse(task));
    }

    @GetMapping("/view_by_deadline")
    public Map<String, Map<String, List<TaskResponse>>> viewByDeadline() {
        return ProjectMapper.toDeadlineGroupsMap(projectService.getTasksGroupedByDeadline(), FORMATTER);
    }

    // Defined before /{projectName} to prevent route collision where "/today"
    // matches "{projectName}"
    @GetMapping("/today")
    public Map<String, List<TaskResponse>> getTasksForToday() {
        return ProjectMapper.toTasksByProject(projectService.getTasksForToday(LocalDate.now()));
    }

    @DeleteMapping("/{projectName}")
    public ResponseEntity<Void> deleteProject(@PathVariable String projectName) {
        projectService.removeProject(projectName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectName}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable String projectName,
            @PathVariable long taskId) {
        projectService.removeTask(taskId);
        return ResponseEntity.noContent().build();
    }
}

