package com.ortecfinance.tasklist.mapper;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.dto.ProjectResponse;
import com.ortecfinance.tasklist.dto.TaskResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;


class ProjectMapperTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Test
    void toTaskResponseCopiesAllFields() {
        Task task = new Task("Eat more donuts.", true, LocalDate.of(2024, 11, 25), null);

        TaskResponse response = ProjectMapper.toTaskResponse(task);

        assertThat(response, is(new TaskResponse(task.getId(),"Eat more donuts.", true, LocalDate.of(2024, 11, 25))));
    }

    @Test
    void toProjectResponseIncludesAllTasks() throws Exception {
        Project project = new Project("secrets");
        java.lang.reflect.Field idField = Project.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(project, 42L);

        project.addTask(new Task(1L, "Eat more donuts.", false, null));
        project.addTask(new Task(2L, "Destroy all humans.", false, null));
        ProjectResponse response = ProjectMapper.toProjectResponse(project);
        assertThat(response.id(), is(42L));
        assertThat(response.name(), is("secrets"));
        assertThat(response.tasks(), contains(
                new TaskResponse(1L, "Eat more donuts.", false, null),
                new TaskResponse(2L, "Destroy all humans.", false, null)));
    }
    @Test
    void toDeadlineGroupsMapFormatsDeadlineAndLabelsNoDeadline() {
        Task withDeadline = new Task(1L, "Eat more donuts.", false, LocalDate.of(2021, 11, 11));
        Task withoutDeadline = new Task(2L, "Destroy all humans.", false, null);
        Map<Optional<LocalDate>, Map<String, List<Task>>> grouped = new TreeMap<>(
                java.util.Comparator.comparing(d -> d.orElse(LocalDate.MAX)));
        Map<String, List<Task>> secretsWithDeadline = new LinkedHashMap<>();
        secretsWithDeadline.put("secrets", List.of(withDeadline));
        grouped.put(Optional.of(LocalDate.of(2021, 11, 11)), secretsWithDeadline);
        Map<String, List<Task>> secretsNoDeadline = new LinkedHashMap<>();
        secretsNoDeadline.put("secrets", List.of(withoutDeadline));
        grouped.put(Optional.empty(), secretsNoDeadline);
        Map<String, Map<String, List<TaskResponse>>> result = ProjectMapper.toDeadlineGroupsMap(grouped, FORMATTER);
        assertThat(result.containsKey("11-11-2021"), is(true));
        assertThat(result.containsKey("No deadline"), is(true));
        assertThat(result.get("11-11-2021").get("secrets").get(0).id(), is(1L));
        assertThat(result.get("No deadline").get("secrets").get(0).id(), is(2L));
    }
}

