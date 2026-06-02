package com.eulerity.taskmanager.controller;

import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Task;
import com.eulerity.taskmanager.model.TaskStatus;
import com.eulerity.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void crudEndpointsShouldWorkEndToEnd() throws Exception {
        Task createRequest = new Task(
                "Submit quarterly report",
                "Send report to finance",
                LocalDate.of(2026, 6, 5),
                null
        );

        String createdTaskJson = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Submit quarterly report"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Task createdTask = objectMapper.readValue(createdTaskJson, Task.class);

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(createdTask.getId()))
                .andExpect(jsonPath("$[0].title").value("Submit quarterly report"));

        mockMvc.perform(get("/tasks/{id}", createdTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTask.getId()))
                .andExpect(jsonPath("$.title").value("Submit quarterly report"));

        Task updateRequest = new Task(
                "Submit revised quarterly report",
                "Include the updated forecast",
                LocalDate.of(2026, 6, 6),
                Priority.HIGH
        );
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);

        mockMvc.perform(put("/tasks/{id}", createdTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTask.getId()))
                .andExpect(jsonPath("$.title").value("Submit revised quarterly report"))
                .andExpect(jsonPath("$.description").value("Include the updated forecast"))
                .andExpect(jsonPath("$.dueDate").value("2026-06-06"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(delete("/tasks/{id}", createdTask.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/{id}", createdTask.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: " + createdTask.getId()));
    }
}
