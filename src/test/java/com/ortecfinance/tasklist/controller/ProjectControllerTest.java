package com.ortecfinance.tasklist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    void updateDeadlineViaQueryParam() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"deadline-project\"}"));

        String response = mockMvc.perform(post("/projects/deadline-project/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task with deadline\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/projects/deadline-project/tasks/" + taskId)
                        .param("deadline", "25-11-2024"))
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
    void updateStatusViaQueryParam() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"status-project\"}"));

        String response = mockMvc.perform(post("/projects/status-project/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task for status update\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/projects/status-project/tasks/" + taskId)
                        .param("done", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        mockMvc.perform(put("/projects/status-project/tasks/" + taskId)
                        .param("done", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(false));
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

        mockMvc.perform(put("/projects/today-project/tasks/" + taskId)
                        .param("deadline", todayStr))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today-project").isArray())
                .andExpect(jsonPath("$.today-project[0].description").value("task due today"));
    }

    @Test
    void removeDeadlineViaEmptyRequestParam() throws Exception {
        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"remove-deadline-proj\"}"));

        String response = mockMvc.perform(post("/projects/remove-deadline-proj/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"task with deadline\"}"))
                .andReturn().getResponse().getContentAsString();

        long taskId = objectMapper.readTree(response).get("id").asLong();

        // Set deadline
        mockMvc.perform(put("/projects/remove-deadline-proj/tasks/" + taskId)
                        .param("deadline", "25-11-2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value("2024-11-25"));

        // Remove deadline by passing empty value
        mockMvc.perform(put("/projects/remove-deadline-proj/tasks/" + taskId)
                        .param("deadline", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadline").value(org.hamcrest.Matchers.nullValue()));
    }

}
