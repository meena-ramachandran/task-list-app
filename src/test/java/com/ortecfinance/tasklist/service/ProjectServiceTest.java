package com.ortecfinance.tasklist.service;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.exception.ProjectAlreadyExistsException;
import com.ortecfinance.tasklist.exception.ProjectNotFoundException;
import com.ortecfinance.tasklist.exception.TaskNotFoundException;
import com.ortecfinance.tasklist.store.ProjectStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProjectServiceTest {
    private final ProjectStore store = new ProjectStore();
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
    void addTaskSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", "Eat more donuts");
        assertThat(task.getDescription(), is("Eat more donuts"));
        assertThat(task.isDone(), is(false));
        assertThat(task.getDeadline(), is(nullValue()));
    }

    @Test
    void addTaskThrowsIllegalArgumentExceptionIfNullDescription() {
        service.addProject("secrets");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addTask("secrets", null)
        );
    }

    @Test
    void addTaskThrowsIllegalArgumentExceptionIfEmptyDescription() {
        service.addProject("secrets");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addTask("secrets", "  ")
        );
    }

    @Test
    void addTaskThrowsProjectNotFoundException() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.addTask("missing", "Some task")
        );
    }

    @Test
    void setDeadlineByTaskIdSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", "Eat more donuts");
        LocalDate deadline = LocalDate.of(2024, 11, 25);

        Task updatedTask = service.setDeadline(task.getId(), deadline);
        assertThat(updatedTask.getDeadline(), is(deadline));
    }

    @Test
    void setDeadlineByTaskIdThrowsTaskNotFoundException() {
        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDeadline(999L, LocalDate.now())
        );
    }

    @Test
    void setDoneByTaskIdSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", "Eat more donuts");

        Task updatedTask = service.setDone(task.getId(), true);
        assertThat(updatedTask.isDone(), is(true));

        updatedTask = service.setDone(task.getId(), false);
        assertThat(updatedTask.isDone(), is(false));
    }

    @Test
    void setDoneByTaskIdThrowsTaskNotFoundException() {
        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDone(999L, true)
        );
    }

    @Test
    void setDoneByProjectAndTaskIdSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", "Eat more donuts");

        Task updatedTask = service.setDone("secrets", task.getId(), true);
        assertThat(updatedTask.isDone(), is(true));
    }

    @Test
    void setDoneByProjectAndTaskIdThrowsProjectNotFoundException() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.setDone("missing", 1L, true)
        );
    }

    @Test
    void setDoneByProjectAndTaskIdThrowsTaskNotFoundException() {
        service.addProject("secrets");
        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDone("secrets", 999L, true)
        );
    }

    @Test
    void removeTaskSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", "Eat more donuts");

        service.removeTask(task.getId());

        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDone(task.getId(), true)
        );
    }

    @Test
    void removeTaskThrowsTaskNotFoundException() {
        assertThrows(
                TaskNotFoundException.class,
                () -> service.removeTask(999L)
        );
    }

    @Test
    void setDeadlineByProjectAndTaskIdSuccessful() {
        service.addProject("secrets");
        Task task = service.addTask("secrets", "Eat more donuts");
        LocalDate deadline = LocalDate.of(2024, 11, 25);

        Task updatedTask = service.setDeadline("secrets", task.getId(), deadline);
        assertThat(updatedTask.getDeadline(), is(deadline));
    }

    @Test
    void setDeadlineByProjectAndTaskIdThrowsProjectNotFoundException() {
        assertThrows(
                ProjectNotFoundException.class,
                () -> service.setDeadline("missing", 1L, LocalDate.now())
        );
    }

    @Test
    void setDeadlineByProjectAndTaskIdThrowsTaskNotFoundException() {
        service.addProject("secrets");
        assertThrows(
                TaskNotFoundException.class,
                () -> service.setDeadline("secrets", 999L, LocalDate.now())
        );
    }

    @Test
    void getTasksForTodayReturnsCorrectTasks() {
        service.addProject("secrets");
        service.addProject("training");

        Task t1 = service.addTask("secrets", "Eat more donuts");
        Task t2 = service.addTask("secrets", "Destroy all humans");
        Task t3 = service.addTask("training", "TDD");

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        service.setDeadline(t1.getId(), today);
        service.setDeadline(t2.getId(), tomorrow);
        service.setDeadline(t3.getId(), today);

        Map<String, List<Task>> tasksForToday = service.getTasksForToday(today);
        assertThat(tasksForToday.keySet(), containsInAnyOrder("secrets", "training"));
        assertThat(tasksForToday.get("secrets"), contains(t1));
        assertThat(tasksForToday.get("training"), contains(t3));
    }

    @Test
    void getTasksForTodayReturnsEmptyIfNoTasks() {
        service.addProject("secrets");
        service.addTask("secrets", "Eat more donuts");

        Map<String, List<Task>> tasksForToday = service.getTasksForToday(LocalDate.now());
        assertThat(tasksForToday.entrySet(), empty());
    }

    @Test
    void getTasksGroupedByDeadlineSortedCorrectly() {
        service.addProject("secrets");
        service.addProject("training");

        Task t1 = service.addTask("secrets", "Eat more donuts");
        Task t2 = service.addTask("secrets", "Destroy all humans");
        Task t3 = service.addTask("training", "TDD");

        LocalDate d1 = LocalDate.of(2024, 11, 25);
        LocalDate d2 = LocalDate.of(2024, 11, 26);

        service.setDeadline(t1.getId(), d2);
        service.setDeadline(t2.getId(), null);
        service.setDeadline(t3.getId(), d1);

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
}
