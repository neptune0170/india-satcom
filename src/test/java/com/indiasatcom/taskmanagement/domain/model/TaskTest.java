package com.indiasatcom.taskmanagement.domain.model;

import com.indiasatcom.taskmanagement.domain.exception.InvalidTaskException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TaskTest {

    private static final Logger log = LoggerFactory.getLogger(TaskTest.class);

    private final LocalDate future = LocalDate.now().plusDays(7);

    @Test
    void create_assignsIdAndDefaultsStatusToPending() {
        log.info("Input: title='Write report', description='Quarterly summary', status=null, dueDate={}", future);
        Task task = Task.create("Write report", "Quarterly summary", null, future);
        log.info("Result: id={}, title='{}', status={}, dueDate={}",
                task.getId(), task.getTitle(), task.getStatus(), task.getDueDate());

        assertThat(task.getId()).isNotNull();
        assertThat(task.getId().value()).isNotBlank();
        assertThat(task.getTitle()).isEqualTo("Write report");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getDueDate()).isEqualTo(future);
    }

    @Test
    void create_respectsExplicitStatus() {
        log.info("Input: title='X', status=IN_PROGRESS, dueDate={}", future);
        Task task = Task.create("X", null, TaskStatus.IN_PROGRESS, future);
        log.info("Result: status={}", task.getStatus());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void create_rejectsBlankTitle() {
        log.info("Input: title='  ' (blank), dueDate={}", future);
        Throwable thrown = catchThrowable(() -> Task.create("  ", null, null, future));
        log.info("Result: {} thrown -> '{}'", thrown.getClass().getSimpleName(), thrown.getMessage());

        assertThat(thrown).isInstanceOf(InvalidTaskException.class).hasMessageContaining("title");
    }

    @Test
    void create_rejectsNullDueDate() {
        log.info("Input: title='X', dueDate=null");
        Throwable thrown = catchThrowable(() -> Task.create("X", null, null, null));
        log.info("Result: {} thrown -> '{}'", thrown.getClass().getSimpleName(), thrown.getMessage());

        assertThat(thrown).isInstanceOf(InvalidTaskException.class).hasMessageContaining("due_date");
    }

    @Test
    void create_rejectsPastDueDate() {
        LocalDate past = LocalDate.now().minusDays(1);
        log.info("Input: title='X', dueDate={} (in the past)", past);
        Throwable thrown = catchThrowable(() -> Task.create("X", null, null, past));
        log.info("Result: {} thrown -> '{}'", thrown.getClass().getSimpleName(), thrown.getMessage());

        assertThat(thrown).isInstanceOf(InvalidTaskException.class).hasMessageContaining("past");
    }

    @Test
    void updateTitle_trimsAndRejectsBlank() {
        Task task = Task.create("X", null, null, future);

        log.info("Input: updateTitle('  new title  ')");
        task.updateTitle("  new title  ");
        log.info("Result: title='{}'", task.getTitle());
        assertThat(task.getTitle()).isEqualTo("new title");

        log.info("Input: updateTitle('')");
        Throwable thrown = catchThrowable(() -> task.updateTitle(""));
        log.info("Result: {} thrown -> '{}'", thrown.getClass().getSimpleName(), thrown.getMessage());
        assertThat(thrown).isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void updateStatus_rejectsNull() {
        Task task = Task.create("X", null, null, future);
        log.info("Input: updateStatus(null) on task id={}", task.getId());
        Throwable thrown = catchThrowable(() -> task.updateStatus(null));
        log.info("Result: {} thrown -> '{}'", thrown.getClass().getSimpleName(), thrown.getMessage());

        assertThat(thrown).isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void rehydrate_preservesProvidedId() {
        TaskId id = TaskId.of("fixed-id");
        log.info("Input: rehydrate(id={}, title='X', status=DONE, dueDate={})", id, future);
        Task task = Task.rehydrate(id, "X", null, TaskStatus.DONE, future);
        log.info("Result: id={}, status={}", task.getId(), task.getStatus());

        assertThat(task.getId()).isEqualTo(id);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
    }
}
