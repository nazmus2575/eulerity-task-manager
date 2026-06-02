package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Task;
import com.eulerity.taskmanager.model.TaskStatus;
import com.eulerity.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void createTaskShouldSaveTaskWithDefaultStatus() {
        Task newTask = new Task(
                "Submit quarterly report",
                "Send report to finance",
                LocalDate.of(2026, 6, 5),
                null
        );
        newTask.setStatus(TaskStatus.DONE);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task taskToSave = invocation.getArgument(0);
            Task savedTask = new Task(
                    taskToSave.getTitle(),
                    taskToSave.getDescription(),
                    taskToSave.getDueDate(),
                    taskToSave.getPriority()
            );
            savedTask.setId(1L);
            savedTask.setStatus(taskToSave.getStatus());
            return savedTask;
        });

        Task createdTask = taskService.createTask(newTask);

        assertThat(createdTask.getId()).isEqualTo(1L);
        assertThat(createdTask.getTitle()).isEqualTo("Submit quarterly report");
        assertThat(createdTask.getDescription()).isEqualTo("Send report to finance");
        assertThat(createdTask.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(createdTask.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(createdTask.getStatus()).isEqualTo(TaskStatus.TODO);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        assertThat(taskCaptor.getValue().getId()).isNull();
        assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
    }
}
