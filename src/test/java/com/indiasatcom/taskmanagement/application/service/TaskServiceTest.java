package com.indiasatcom.taskmanagement.application.service;

import com.indiasatcom.taskmanagement.application.dto.CreateTaskRequest;
import com.indiasatcom.taskmanagement.application.dto.TaskResponse;
import com.indiasatcom.taskmanagement.application.dto.UpdateTaskRequest;
import com.indiasatcom.taskmanagement.domain.exception.TaskNotFoundException;
import com.indiasatcom.taskmanagement.domain.model.Task;
import com.indiasatcom.taskmanagement.domain.model.TaskId;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;
import com.indiasatcom.taskmanagement.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceTest.class);

    private TaskRepository repository;
    private TaskService service;
    private final LocalDate future = LocalDate.now().plusDays(7);

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        service = new TaskService(repository);
    }

    @Test
    void createTask_persistsAndReturnsResponse() {
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateTaskRequest request =
                new CreateTaskRequest("Buy groceries", "milk, bread", TaskStatus.PENDING, future);
        log.info("Input: createTask({})", request);

        TaskResponse response = service.createTask(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        log.info("Result: response=(id={}, title='{}', status={}); repository.save was called with Task(title='{}')",
                response.id(), response.title(), response.status(), captor.getValue().getTitle());

        assertThat(captor.getValue().getTitle()).isEqualTo("Buy groceries");
        assertThat(response.title()).isEqualTo("Buy groceries");
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.id()).isNotBlank();
    }

    @Test
    void getTask_returnsExistingTask() {
        Task task = Task.create("X", null, null, future);
        when(repository.findById(task.getId())).thenReturn(Optional.of(task));
        log.info("Input: getTask(id={}), repository will return existing task", task.getId());

        TaskResponse response = service.getTask(task.getId().value());
        log.info("Result: response=(id={}, title='{}')", response.id(), response.title());

        assertThat(response.id()).isEqualTo(task.getId().value());
    }

    @Test
    void getTask_whenMissing_throwsNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        log.info("Input: getTask(id='missing'), repository returns Optional.empty()");

        Throwable thrown = catchThrowable(() -> service.getTask("missing"));
        log.info("Result: {} thrown -> '{}'", thrown.getClass().getSimpleName(), thrown.getMessage());

        assertThat(thrown).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateTask_appliesOnlyProvidedFields() {
        Task existing = Task.create("Old title", "old desc", TaskStatus.PENDING, future);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest req = new UpdateTaskRequest("New title", null, TaskStatus.IN_PROGRESS, null);
        log.info("Input: updateTask(id={}, request={}); existing=(title='Old title', desc='old desc', status=PENDING, dueDate={})",
                existing.getId(), req, future);

        TaskResponse response = service.updateTask(existing.getId().value(), req);
        log.info("Result: response=(title='{}', desc='{}', status={}, dueDate={})",
                response.title(), response.description(), response.status(), response.dueDate());

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("old desc");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.dueDate()).isEqualTo(future);
    }

    @Test
    void updateTask_whenMissing_throwsNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        UpdateTaskRequest req = new UpdateTaskRequest("t", null, null, null);
        log.info("Input: updateTask(id='nope', request={}), repository returns Optional.empty()", req);

        Throwable thrown = catchThrowable(() -> service.updateTask("nope", req));
        log.info("Result: {} thrown -> '{}'; repository.save was NOT called",
                thrown.getClass().getSimpleName(), thrown.getMessage());

        assertThat(thrown).isInstanceOf(TaskNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteTask_removesWhenExists() {
        TaskId id = TaskId.of("abc");
        when(repository.existsById(id)).thenReturn(true);
        log.info("Input: deleteTask(id='abc'), repository.existsById -> true");

        service.deleteTask("abc");
        log.info("Result: repository.deleteById was called for id={}", id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteTask_whenMissing_throwsNotFound() {
        when(repository.existsById(any())).thenReturn(false);
        log.info("Input: deleteTask(id='missing'), repository.existsById -> false");

        Throwable thrown = catchThrowable(() -> service.deleteTask("missing"));
        log.info("Result: {} thrown -> '{}'; repository.deleteById was NOT called",
                thrown.getClass().getSimpleName(), thrown.getMessage());

        assertThat(thrown).isInstanceOf(TaskNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void listTasks_delegatesToRepositoryWithFilterAndPagination() {
        Task t = Task.create("only", null, null, future);
        when(repository.findAll(TaskStatus.PENDING, 0, 10)).thenReturn(List.of(t));
        log.info("Input: listTasks(status=PENDING, page=0, size=10); repository returns 1 task");

        List<TaskResponse> result = service.listTasks(TaskStatus.PENDING, 0, 10);
        log.info("Result: size={}, titles={}", result.size(),
                result.stream().map(TaskResponse::title).toList());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("only");
    }

    @Test
    void listTasks_rejectsInvalidPagination() {
        log.info("Input: listTasks(null, page=-1, size=10)");
        Throwable t1 = catchThrowable(() -> service.listTasks(null, -1, 10));
        log.info("Result: {} thrown -> '{}'", t1.getClass().getSimpleName(), t1.getMessage());

        log.info("Input: listTasks(null, page=0, size=0)");
        Throwable t2 = catchThrowable(() -> service.listTasks(null, 0, 0));
        log.info("Result: {} thrown -> '{}'", t2.getClass().getSimpleName(), t2.getMessage());

        assertThat(t1).isInstanceOf(IllegalArgumentException.class);
        assertThat(t2).isInstanceOf(IllegalArgumentException.class);
    }
}
