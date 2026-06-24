package com.ortecfinance.tasklist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortecfinance.tasklist.store.ProjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectStore store;

    @BeforeEach
    void setUp() {
        store.clear();
    }

    @Test
    void createAndListProjects() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"secrets\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("secrets"))
                .andExpect(jsonPath("$.tasks").isArray());

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='secrets')]").exists());
    }

    @Test
    void createTaskUnderExistingProject() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"training\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects/training/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Four Elements of Simple Design\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Four Elements of Simple Design"))
                .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    void createTaskUnderMissingProjectReturns404() throws Exception {
        mockMvc.perform(post("/projects/does-not-exist/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Could not find a project with the name does-not-exist."));
    }


    @Test
    void updateDeadlineViaPatch() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"deadline-project\"}"));

        String response = mockMvc.perform(post("/projects/deadline-project/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task with deadline\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/projects/deadline-project/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\": \"25-11-2024\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2024-11-25"));
    }


    @Test
    void viewByDeadlineGroupsChronologicallyWithNoDeadlineLast() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"view-by-deadline-project\"}"));

        mockMvc.perform(post("/projects/view-by-deadline-project/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"no deadline task\"}"));

        mockMvc.perform(get("/projects/view_by_deadline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['No deadline'].['view-by-deadline-project']").isArray());
    }

    @Test
    void updateStatusViaPatch() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"status-project\"}"));

        String response = mockMvc.perform(post("/projects/status-project/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task for status update\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/projects/status-project/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        mockMvc.perform(patch("/projects/status-project/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(false));
    }


    @Test
    void getSingleProjectReturnsProjectDetails() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"single-project\"}"));

        mockMvc.perform(get("/projects/single-project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("single-project"))
                .andExpect(jsonPath("$.tasks").isArray());
    }

    @Test
    void getSingleProjectReturns404IfMissing() throws Exception {
        mockMvc.perform(get("/projects/missing-project"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Could not find a project with the name missing-project."));
    }

    @Test
    void getTasksForTodayReturnsTodayTasks() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"today-project\"}"));
        String response = mockMvc.perform(post("/projects/today-project/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task due today\"}"))
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(response).get("id").asLong();
        String todayStr = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        mockMvc.perform(patch("/projects/today-project/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\": \"" + todayStr + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/projects/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today-project").isArray())
                .andExpect(jsonPath("$.today-project[0].description").value("task due today"));
    }

    @Test
    void deleteProjectRemovesProject() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"delete-project\"}"));

        mockMvc.perform(delete("/projects/delete-project"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/projects/delete-project"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProjectReturns404IfMissing() throws Exception {
        mockMvc.perform(delete("/projects/missing-project"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTaskRemovesTask() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"delete-task-project\"}"));

        String response = mockMvc.perform(post("/projects/delete-task-project/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task to delete\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/projects/delete-task-project/tasks/" + taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/projects/delete-task-project/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isNotFound());
    }


    @Test
    void deleteTaskReturns404IfMissing() throws Exception {
        mockMvc.perform(delete("/projects/any/tasks/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDuplicateProjectReturns409Conflict() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"duplicate-project\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"duplicate-project\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A project with the name duplicate-project already exists."));
    }

    @Test
    void createProjectWithEmptyNameReturns400BadRequest() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Project name cannot be empty."));
    }

    @Test
    void createTaskWithEmptyDescriptionReturns400BadRequest() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"empty-desc-project\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects/empty-desc-project/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Task description cannot be empty."));
    }

    @Test
    void removeDeadlineViaPatch() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"remove-deadline-proj\"}"));

        String response = mockMvc.perform(post("/projects/remove-deadline-proj/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task with deadline\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        // Set deadline initially
        mockMvc.perform(patch("/projects/remove-deadline-proj/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\": \"25-11-2024\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2024-11-25"));

        mockMvc.perform(patch("/projects/remove-deadline-proj/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value(org.hamcrest.Matchers.nullValue()));
    }


    @Test
    void updateTaskOwnershipValidationReturns404NotFound() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"project-owner-a\"}"));

        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"project-owner-b\"}"));

        String response = mockMvc.perform(post("/projects/project-owner-a/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task in project A\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/projects/project-owner-b/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Could not find a task with an ID of " + taskId + "."));
    }

}
