package com.ortecfinance.tasklist.service;

import com.ortecfinance.tasklist.store.ProjectStore;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
    void removeProjectDeletesItIfPresent() {
        service.addProject("secrets");
        assertThat(store.existsByName("secrets"), is(true));

        service.removeProject("secrets");

        assertThat(store.existsByName("secrets"), is(false));
    }

    @Test
    void removeProjectThrowsIfMissing() {
        assertThrows(
                RuntimeException.class,
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
}
