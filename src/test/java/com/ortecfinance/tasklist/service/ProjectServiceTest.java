package com.ortecfinance.tasklist.service;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.dto.UpdateTaskRequest;
import com.ortecfinance.tasklist.exception.ProjectAlreadyExistsException;
import com.ortecfinance.tasklist.exception.ProjectNotFoundException;
import com.ortecfinance.tasklist.exception.TaskNotFoundException;
import com.ortecfinance.tasklist.store.InMemoryProjectStore;
import com.ortecfinance.tasklist.store.ProjectStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProjectServiceTest {
    private final ProjectStore store = new InMemoryProjectStore();
    private final ProjectService service = new ProjectService(store);

    @Test
    void addProjectMakesItExistInStore() {
        service.addProject("secrets");

        assertThat(store.existsByName("secrets"), is(true));
    }

    @Test
    void addProjectThrowsIllegalArgumentExceptionIfNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addProject(null)
        );
    }

    @Test
    void addProjectThrowsIllegalArgumentExceptionIfEmpty() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addProject("   ")
        );
    }

    @Test
    void addProjectThrowsProjectAlreadyExistsException() {
        service.addProject("secrets");
        assertThrows(
                ProjectAlreadyExistsException.class,
                () -> service.addProject("secrets")
        );
    }

    @Test
    void getProjectReturnsExistingProject() {
        Project project = service.addProject("secrets");
        Project retrieved = service.getProject("secrets");
        assertThat(retrieved, is(project));
    }

    @Test
    void getProjectThrowsProjectNotFoundException() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.getProject("missing")
        );
    }

    @Test
    void removeProjectDeletesItIfPresent() {
        service.addProject("secrets");
        assertThat(store.existsByName("secrets"), is(true));

        service.removeProject("secrets");

        assertThat(store.existsByName("secrets"), is(false));
    }

    @Test
    void removeProjectThrowsIfMissing() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.removeProject("missing")
        );
    }

    @Test
    void projectExistsReflectsAddedProjects() {
        service.addProject("secrets");

        assertThat(store.existsByName("secrets"), is(true));
        assertThat(store.existsByName("missing"), is(false));
    }

    @Test
    void getProjectsReturnsAllAddedProjects() {
        service.addProject("secrets");
        service.addProject("training");

        assertThat(service.getProjects(), hasSize(2));
    }

    @Test
    void getProjectsEmptyInitially() {
        assertThat(service.getProjects(), empty());
    }

    @Test
    void addTaskByNameSuccessful() {
        service.addProject("secrets");
        Task task = new Task("Eat more donuts", false, null, null);
        Task returnedTask = service.addTask("secrets", task);

        assertThat(returnedTask.getDescription(), is("Eat more donuts"));
        assertThat(returnedTask.isDone(), is(false));
        assertThat(returnedTask.getId(), is(notNullValue()));
    }

    @Test
    void addTaskByNameThrowsIllegalArgumentExceptionIfEmpty() {
        service.addProject("secrets");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addTask("secrets", new Task("", false, null, null))
        );
    }

    @Test
    void addTaskByNameThrowsProjectNotFoundException() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.addTask("missing", new Task("task", false, null, null))
        );
    }

    @Test
    void setDeadlineByTaskIdSuccessful() {
        Project project = service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));
        LocalDate deadline = LocalDate.of(2024, 11, 25);

        Task returned = service.setDeadline(task.getId(), deadline);
        assertThat(returned.getDeadline(), is(deadline));
    }

    @Test
    void setDeadlineByTaskIdThrowsTaskNotFound() {
        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDeadline(999L, LocalDate.now())
        );
    }

    @Test
    void setDoneByTaskIdSuccessful() {
        Project project = service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));

        Task returned = service.setDone(task.getId(), true);
        assertThat(returned.isDone(), is(true));
    }

    @Test
    void setDoneByTaskIdThrowsTaskNotFound() {
        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDone(999L, true)
        );
    }

    @Test
    void setDoneByProjectNameAndTaskIdSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));

        Task returned = service.setDone("secrets", task.getId(), true);
        assertThat(returned.isDone(), is(true));
    }

    @Test
    void setDoneByProjectNameAndTaskIdThrowsProjectNotFound() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.setDone("missing", 999L, true)
        );
    }

    @Test
    void setDoneByProjectNameAndTaskIdThrowsTaskNotFound() {
        service.addProject("secrets");
        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDone("secrets", 999L, true)
        );
    }

    @Test
    void removeTaskByTaskIdSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));

        service.removeTask(task.getId());
        assertThat(store.findTaskById(task.getId()).isEmpty(), is(true));
    }

    @Test
    void removeTaskByTaskIdThrowsTaskNotFound() {
        assertThrows(
                TaskNotFoundException.class,
                () -> service.removeTask(999L)
        );
    }

    @Test
    void updateTaskByProjectNameSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));

        // Test updating description, done, and deadline (dd-MM-yyyy format)
        UpdateTaskRequest req1 = new UpdateTaskRequest("new description", true, "25-11-2024");
        Task updated = service.updateTask("secrets", task.getId(), req1);
        assertThat(updated.getDescription(), is("new description"));
        assertThat(updated.isDone(), is(true));
        assertThat(updated.getDeadline(), is(LocalDate.of(2024, 11, 25)));

        // Test updating deadline (yyyy-MM-dd format)
        UpdateTaskRequest req2 = new UpdateTaskRequest(null, null, "2024-11-30");
        updated = service.updateTask("secrets", task.getId(), req2);
        assertThat(updated.getDeadline(), is(LocalDate.of(2024, 11, 30)));

        // Test explicit null deadline removal
        UpdateTaskRequest req3 = new UpdateTaskRequest(null, null, "null");
        updated = service.updateTask("secrets", task.getId(), req3);
        assertThat(updated.getDeadline(), is(nullValue()));
    }

    @Test
    void updateTaskByProjectNameThrowsIllegalArgumentExceptionIfEmptyDescription() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));
        UpdateTaskRequest req = new UpdateTaskRequest("   ", null, null);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateTask("secrets", task.getId(), req)
        );
    }

    @Test
    void updateTaskByProjectNameThrowsIllegalArgumentExceptionIfInvalidDeadline() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));
        UpdateTaskRequest req = new UpdateTaskRequest(null, null, "invalid-date");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateTask("secrets", task.getId(), req)
        );
    }

    @Test
    void updateTaskByProjectNameThrowsProjectNotFound() {
        UpdateTaskRequest req = new UpdateTaskRequest("task", true, null);
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.updateTask("missing", 999L, req)
        );
    }

    @Test
    void updateTaskByProjectNameThrowsTaskNotFound() {
        service.addProject("secrets");
        UpdateTaskRequest req = new UpdateTaskRequest("task", true, null);
        assertThrows(
                TaskNotFoundException.class,
                () -> service.updateTask("secrets", 999L, req)
        );
    }

    @Test
    void removeTaskByProjectNameSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));

        service.removeTask("secrets", task.getId());
        assertThat(store.findTaskById(task.getId()).isEmpty(), is(true));
    }

    @Test
    void removeTaskByProjectNameThrowsProjectNotFound() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.removeTask("missing", 999L)
        );
    }

    @Test
    void removeTaskByProjectNameThrowsTaskNotFound() {
        service.addProject("secrets");
        assertThrows(
                TaskNotFoundException.class,
                () -> service.removeTask("secrets", 999L)
        );
    }

    @Test
    void setDeadlineByProjectNameAndTaskIdSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", new Task("task", false, null, null));
        LocalDate deadline = LocalDate.of(2024, 11, 25);

        Task returned = service.setDeadline("secrets", task.getId(), deadline);
        assertThat(returned.getDeadline(), is(deadline));
    }

    @Test
    void getTasksForTodayReturnsCorrectTasks() {
        service.addProject("secrets");
        service.addProject("training");

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Task t1 = service.addTask("secrets", new Task("t1", false, today, null));
        Task t2 = service.addTask("secrets", new Task("t2", false, tomorrow, null));
        Task t3 = service.addTask("training", new Task("t3", false, today, null));

        Map<String, List<Task>> tasksForToday = service.getTasksForToday(today);
        assertThat(tasksForToday.keySet(), containsInAnyOrder("secrets", "training"));
        assertThat(tasksForToday.get("secrets"), contains(t1));
        assertThat(tasksForToday.get("training"), contains(t3));
    }

    @Test
    void getTasksGroupedByDeadlineSortedCorrectly() {
        service.addProject("secrets");
        service.addProject("training");

        LocalDate d1 = LocalDate.of(2024, 11, 25);
        LocalDate d2 = LocalDate.of(2024, 11, 26);

        Task t1 = service.addTask("secrets", new Task("t1", false, d2, null));
        Task t2 = service.addTask("secrets", new Task("t2", false, null, null));
        Task t3 = service.addTask("training", new Task("t3", false, d1, null));

        Map<Optional<LocalDate>, Map<String, List<Task>>> grouped = service.getTasksGroupedByDeadline();
        Iterator<Map.Entry<Optional<LocalDate>, Map<String, List<Task>>>> iterator = grouped.entrySet().iterator();

        Map.Entry<Optional<LocalDate>, Map<String, List<Task>>> first = iterator.next();
        assertThat(first.getKey(), is(Optional.of(d1)));
        assertThat(first.getValue().get("training"), contains(t3));

        Map.Entry<Optional<LocalDate>, Map<String, List<Task>>> second = iterator.next();
        assertThat(second.getKey(), is(Optional.of(d2)));
        assertThat(second.getValue().get("secrets"), contains(t1));

        Map.Entry<Optional<LocalDate>, Map<String, List<Task>>> third = iterator.next();
        assertThat(third.getKey(), is(Optional.empty()));
        assertThat(third.getValue().get("secrets"), contains(t2));

        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    void renameProjectByOldNameSuccessful() {
        service.addProject("secrets");
        Project project = service.renameProject("secrets", "super-secrets");

        assertThat(project.getName(), is("super-secrets"));
        assertThat(store.existsByName("secrets"), is(false));
        assertThat(store.existsByName("super-secrets"), is(true));
    }

    @Test
    void renameProjectByOldNameThrowsIllegalArgumentExceptionIfEmpty() {
        service.addProject("secrets");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.renameProject("secrets", "   ")
        );
    }

    @Test
    void renameProjectByOldNameThrowsProjectNotFound() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.renameProject("missing", "newName")
        );
    }

    @Test
    void renameProjectByOldNameThrowsProjectAlreadyExists() {
        service.addProject("secrets");
        service.addProject("training");
        assertThrows(
                ProjectAlreadyExistsException.class,
                () -> service.renameProject("secrets", "training")
        );
    }

    @Test
    void getProjectByIdSuccessful() {
        Project project = service.addProject("secrets");
        Project retrieved = service.getProject(project.getId());
        assertThat(retrieved, is(project));
    }

    @Test
    void getProjectByIdThrowsProjectNotFound() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.getProject(999L)
        );
    }

    @Test
    void removeProjectByIdSuccessful() {
        Project project = service.addProject("secrets");
        service.removeProject(project.getId());
        assertThat(store.existsByName("secrets"), is(false));
    }

    @Test
    void removeProjectByIdThrowsProjectNotFound() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.removeProject(999L)
        );
    }

    @Test
    void addTaskByProjectIdSuccessful() {
        Project project = service.addProject("secrets");
        Task task = new Task("Eat more donuts", false, null, null);
        Task returnedTask = service.addTask(project.getId(), task);

        assertThat(returnedTask.getDescription(), is("Eat more donuts"));
        assertThat(project.getTasks(), contains(returnedTask));
    }

    @Test
    void addTaskByProjectIdThrowsIllegalArgumentExceptionIfEmpty() {
        Project project = service.addProject("secrets");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addTask(project.getId(), new Task("  ", false, null, null))
        );
    }

    @Test
    void addTaskByProjectIdThrowsProjectNotFoundException() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.addTask(999L, new Task("task", false, null, null))
        );
    }

    @Test
    void updateTaskByProjectIdSuccessful() {
        Project project = service.addProject("secrets");
        Task task = service.addTask(project.getId(), new Task("task", false, null, null));

        UpdateTaskRequest req1 = new UpdateTaskRequest("new description", true, "25-11-2024");
        Task updated = service.updateTask(project.getId(), task.getId(), req1);
        assertThat(updated.getDescription(), is("new description"));
        assertThat(updated.isDone(), is(true));
        assertThat(updated.getDeadline(), is(LocalDate.of(2024, 11, 25)));
    }

    @Test
    void updateTaskByProjectIdThrowsProjectNotFound() {
        UpdateTaskRequest req = new UpdateTaskRequest("task", true, null);
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.updateTask(999L, 1L, req)
        );
    }

    @Test
    void updateTaskByProjectIdThrowsTaskNotFound() {
        Project project = service.addProject("secrets");
        UpdateTaskRequest req = new UpdateTaskRequest("task", true, null);
        assertThrows(
                TaskNotFoundException.class,
                () -> service.updateTask(project.getId(), 999L, req)
        );
    }

    @Test
    void removeTaskByProjectIdSuccessful() {
        Project project = service.addProject("secrets");
        Task task = service.addTask(project.getId(), new Task("task", false, null, null));

        service.removeTask(project.getId(), task.getId());
        assertThat(store.findTaskById(task.getId()).isEmpty(), is(true));
    }

    @Test
    void removeTaskByProjectIdThrowsProjectNotFound() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.removeTask(999L, 1L)
        );
    }

    @Test
    void removeTaskByProjectIdThrowsTaskNotFound() {
        Project project = service.addProject("secrets");
        assertThrows(
                TaskNotFoundException.class,
                () -> service.removeTask(project.getId(), 999L)
        );
    }

    @Test
    void renameProjectByIdSuccessful() {
        Project project = service.addProject("secrets");
        Project renamed = service.renameProject(project.getId(), "super-secrets");

        assertThat(renamed.getName(), is("super-secrets"));
        assertThat(store.existsByName("secrets"), is(false));
        assertThat(store.existsByName("super-secrets"), is(true));
    }

    @Test
    void renameProjectByIdThrowsIllegalArgumentExceptionIfEmpty() {
        Project project = service.addProject("secrets");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.renameProject(project.getId(), "   ")
        );
    }

    @Test
    void renameProjectByIdThrowsProjectNotFound() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.renameProject(999L, "newName")
        );
    }

    @Test
    void renameProjectByIdThrowsProjectAlreadyExists() {
        Project p1 = service.addProject("secrets");
        Project p2 = service.addProject("training");
        assertThrows(
                ProjectAlreadyExistsException.class,
                () -> service.renameProject(p1.getId(), "training")
        );
    }
}
