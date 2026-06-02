package com.eulerity.taskmanager.controller;

import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Task;
import com.eulerity.taskmanager.model.TaskStatus;
import com.eulerity.taskmanager.service.GeminiService;
import com.eulerity.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskSuggestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private GeminiService geminiService;

    @Test
    void suggestTaskShouldReturnAiGeneratedTaskWithoutCallingExternalApi() throws Exception {
        String prompt = "remind me to submit the quarterly report before Friday";
        Task suggestedTask = new Task(
                "Submit quarterly report",
                "Submit the quarterly report before Friday",
                LocalDate.of(2026, 6, 5),
                Priority.HIGH
        );
        suggestedTask.setStatus(TaskStatus.TODO);

        when(geminiService.suggestTask(prompt)).thenReturn(suggestedTask);

        mockMvc.perform(post("/tasks/suggest")
                        .contentType(MediaType.TEXT_PLAIN)
                .content(prompt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(nullValue()))
                .andExpect(jsonPath("$.title").value("Submit quarterly report"))
                .andExpect(jsonPath("$.description").value("Submit the quarterly report before Friday"))
                .andExpect(jsonPath("$.dueDate").value("2026-06-05"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"));

        verify(geminiService).suggestTask(prompt);
    }
}
