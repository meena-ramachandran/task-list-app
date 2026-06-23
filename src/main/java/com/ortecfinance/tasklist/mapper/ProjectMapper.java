package com.ortecfinance.tasklist.mapper;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.dto.ProjectResponse;
import com.ortecfinance.tasklist.dto.TaskResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ProjectMapper {
    public static TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(task.getId(), task.getDescription(), task.isDone(), task.getDeadline());
    }

    public static List<TaskResponse> toTaskResponseList(List<Task> tasks) {
        return tasks.stream().map(ProjectMapper::toTaskResponse).toList();
    }

    public static ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(project.getName(), toTaskResponseList(project.getTasks()));
    }

    public static List<ProjectResponse> toProjectResponseList(Collection<Project> projects) {
        return projects.stream().map(ProjectMapper::toProjectResponse).toList();
    }

    public static Map<String, Map<String, List<TaskResponse>>> toDeadlineGroupsMap(
            Map<Optional<LocalDate>, Map<String, List<Task>>> grouped,
            DateTimeFormatter formatter) {

        Map<String, Map<String, List<TaskResponse>>> result = new LinkedHashMap<>();
        grouped.forEach((deadline, byProject) -> {
            String label = deadline.map(formatter::format).orElse("No deadline");

            Map<String, List<TaskResponse>> tasksByProject = new LinkedHashMap<>();
            byProject.forEach((projectName, tasks) -> tasksByProject.put(projectName, toTaskResponseList(tasks)));

            result.put(label, tasksByProject);
        });
        return result;
    }

    public static Map<String, List<TaskResponse>> toTasksByProject(Map<String, List<Task>> tasksByProject) {
        Map<String, List<TaskResponse>> result = new LinkedHashMap<>();
        tasksByProject.forEach((projectName, tasks) -> result.put(projectName, toTaskResponseList(tasks)));
        return result;
    }
}
