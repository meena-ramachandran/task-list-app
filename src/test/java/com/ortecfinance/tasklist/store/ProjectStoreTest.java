package com.ortecfinance.tasklist.store;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ProjectStoreTest {

    private final InMemoryProjectStore store = new InMemoryProjectStore();

    @Test
    void findByIdReturnsEmptyWhenProjectMissing() {
        assertThat(store.findByName("missing").isEmpty(), is(true));
    }

    @Test
    void saveMakesProjectFindable() {
        Project project = new Project("secrets");

        store.save(project);

        assertThat(store.existsByName("secrets"), is(true));
        assertThat(store.findByName("secrets").get(), is(project));
    }

    @Test
    void findAllReturnsEveryStoredProject() {
        store.save(new Project("secrets"));
        store.save(new Project("training"));

        int count = 0;
        for (Project ignored : store.findAll()) {
            count++;
        }

        assertThat(count, is(2));
    }

    @Test
    void findTaskByIdReturnsEmptyWhenMissing() {
        assertThat(store.findTaskById(99L).isEmpty(), is(true));
    }

    @Test
    void saveTaskAttachesTaskToProjectAndAssignsId() {
        Project project = new Project("secrets");
        store.save(project);

        Task task = store.saveTask(project, new Task("Eat more donuts.", false, null, null));

        assertThat(task.getId(), is(1L));
        assertThat(task.getDescription(), is("Eat more donuts."));
        assertThat(store.findTaskById(task.getId()).get(), is(task));
    }

    @Test
    void saveTaskAssignsIncrementingIdsAcrossProjects() {
        Project a = new Project("a");
        Project b = new Project("b");
        store.save(a);
        store.save(b);

        Task first = store.saveTask(a, new Task("first",  false, null, null));
        Task second = store.saveTask(b, new Task("second",   false, null, null));

        assertThat(first.getId(), is(1L));
        assertThat(second.getId(), is(2L));
    }

    @Test
    void deleteByIdRemovesProject() {
        Project project = new Project("to-delete");
        store.save(project);
        assertThat(store.existsByName("to-delete"), is(true));

        store.deleteByName("to-delete");

        assertThat(store.existsByName("to-delete"), is(false));
    }

    @Test
    void deleteTaskByIdRemovesTaskFromCorrectProjectAndReturnsTrue() {
        Project project = new Project("project");
        store.save(project);
        Task task = store.saveTask(project, new Task("task",  false, null, null));

        boolean removed = store.deleteTaskById(task.getId());

        assertThat(removed, is(true));
        assertThat(project.getTasks().isEmpty(), is(true));
    }

    @Test
    void deleteTaskByIdReturnsFalseIfTaskDoesNotExist() {
        boolean removed = store.deleteTaskById(999L);
        assertThat(removed, is(false));
    }

    @Test
    void findByIdReturnsProjectWhenExists() {
        Project project = new Project("secrets");
        store.save(project);
        assertThat(store.findById(project.getId()).get(), is(project));
    }

    @Test
    void findByIdReturnsEmptyWhenIdIsNull() {
        assertThat(store.findById(null).isEmpty(), is(true));
    }

    @Test
    void renameProjectUpdatesNameAndMapping() {
        Project project = new Project("secrets");
        store.save(project);

        store.renameProject(project, "super-secrets");

        assertThat(project.getName(), is("super-secrets"));
        assertThat(store.existsByName("secrets"), is(false));
        assertThat(store.existsByName("super-secrets"), is(true));
        assertThat(store.findByName("super-secrets").get(), is(project));
    }

    @Test
    void clearResetsStoreAndIds() {
        Project project = new Project("secrets");
        store.save(project);
        store.saveTask(project, new Task("Eat more donuts.", false, null, null));

        store.clear();

        assertThat(store.findAll(), is(empty()));
        assertThat(store.findTaskById(1L).isEmpty(), is(true));

        Project newProject = new Project("new-secrets");
        store.save(newProject);
        assertThat(newProject.getId(), is(1L));
    }

    @Test
    void findTasksByDeadlineFiltersCorrectly() {
        Project project = new Project("secrets");
        store.save(project);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate tomorrow = today.plusDays(1);

        Task t1 = store.saveTask(project, new Task("t1", false, today, null));
        Task t2 = store.saveTask(project, new Task("t2", false, tomorrow, null));
        Task t3 = store.saveTask(project, new Task("t3", false, today, null));

        java.util.Collection<Task> tasks = store.findTasksByDeadline(today);
        assertThat(tasks, containsInAnyOrder(t1, t3));

        assertThat(store.findTasksByDeadline(null), is(empty()));
    }
}
