package com.indiasatcom.taskmanagement.application.service;

import com.indiasatcom.taskmanagement.application.dto.CreateTaskRequest;
import com.indiasatcom.taskmanagement.application.dto.TaskResponse;
import com.indiasatcom.taskmanagement.application.dto.UpdateTaskRequest;
import com.indiasatcom.taskmanagement.domain.exception.TaskNotFoundException;
import com.indiasatcom.taskmanagement.domain.model.Task;
import com.indiasatcom.taskmanagement.domain.model.TaskId;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;
import com.indiasatcom.taskmanagement.domain.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(CreateTaskRequest request) {
        Task task = Task.create(
                request.title(),
                request.description(),
                request.status(),
                request.dueDate()
        );
        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse getTask(String id) {
        return TaskResponse.from(loadTask(id));
    }

    public TaskResponse updateTask(String id, UpdateTaskRequest request) {
        Task task = loadTask(id);
        if (request.title() != null) {
            task.updateTitle(request.title());
        }
        if (request.description() != null) {
            task.updateDescription(request.description());
        }
        if (request.status() != null) {
            task.updateStatus(request.status());
        }
        if (request.dueDate() != null) {
            task.updateDueDate(request.dueDate());
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    public void deleteTask(String id) {
        TaskId taskId = TaskId.of(id);
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(taskId);
    }

    public List<TaskResponse> listTasks(TaskStatus status, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        return taskRepository.findAll(status, page, size).stream()
                .map(TaskResponse::from)
                .toList();
    }

    private Task loadTask(String id) {
        TaskId taskId = TaskId.of(id);
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
