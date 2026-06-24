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
        String projResponse = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"training\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Four Elements of Simple Design\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Four Elements of Simple Design"))
                .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    void createTaskUnderMissingProjectReturns404() throws Exception {
        mockMvc.perform(post("/projects/9999/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Could not find a project with an ID of 9999."));
    }


    @Test
    void updateDeadlineViaPatch() throws Exception {
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"deadline-project\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        String response = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task with deadline\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/projects/" + projectId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\": \"25-11-2024\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2024-11-25"));
    }


    @Test
    void viewByDeadlineGroupsChronologicallyWithNoDeadlineLast() throws Exception {
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"view-by-deadline-project\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"no deadline task\"}"));

        mockMvc.perform(get("/projects/view_by_deadline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['No deadline'].['view-by-deadline-project']").isArray());
    }

    @Test
    void updateStatusViaPatch() throws Exception {
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"status-project\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        String response = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task for status update\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/projects/" + projectId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        mockMvc.perform(patch("/projects/" + projectId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(false));
    }


    @Test
    void getSingleProjectReturnsProjectDetails() throws Exception {
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"single-project\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        mockMvc.perform(get("/projects/" + projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("single-project"))
                .andExpect(jsonPath("$.tasks").isArray());
    }

    @Test
    void getSingleProjectReturns404IfMissing() throws Exception {
        mockMvc.perform(get("/projects/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Could not find a project with an ID of 9999."));
    }

    @Test
    void getTasksForTodayReturnsTodayTasks() throws Exception {
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"today-project\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        String response = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task due today\"}"))
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(response).get("id").asLong();
        String todayStr = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        mockMvc.perform(patch("/projects/" + projectId + "/tasks/" + taskId)
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
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"delete-project\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        mockMvc.perform(delete("/projects/" + projectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/projects/" + projectId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProjectReturns404IfMissing() throws Exception {
        mockMvc.perform(delete("/projects/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTaskRemovesTask() throws Exception {
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"delete-task-project\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        String response = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task to delete\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/projects/" + projectId + "/tasks/" + taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/projects/" + projectId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isNotFound());
    }


    @Test
    void deleteTaskReturns404IfMissing() throws Exception {
        mockMvc.perform(delete("/projects/9999/tasks/9999"))
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
        String projResponse = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"empty-desc-project\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Task description cannot be empty."));
    }

    @Test
    void removeDeadlineViaPatch() throws Exception {
        String projResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"remove-deadline-proj\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projResponse).get("id").asLong();

        String response = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task with deadline\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        // Set deadline initially
        mockMvc.perform(patch("/projects/" + projectId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\": \"25-11-2024\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2024-11-25"));

        mockMvc.perform(patch("/projects/" + projectId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value(org.hamcrest.Matchers.nullValue()));
    }


    @Test
    void updateTaskOwnershipValidationReturns404NotFound() throws Exception {
        String projAResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"project-owner-a\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectAId = objectMapper.readTree(projAResponse).get("id").asLong();

        String projBResponse = mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"project-owner-b\"}"))
                .andReturn().getResponse().getContentAsString();
        long projectBId = objectMapper.readTree(projBResponse).get("id").asLong();

        String response = mockMvc.perform(post("/projects/" + projectAId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task in project A\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/projects/" + projectBId + "/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Could not find a task with an ID of " + taskId + "."));
    }

}
