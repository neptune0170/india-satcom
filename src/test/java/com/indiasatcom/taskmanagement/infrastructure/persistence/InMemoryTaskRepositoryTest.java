package com.indiasatcom.taskmanagement.infrastructure.persistence;

import com.indiasatcom.taskmanagement.domain.model.Task;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTaskRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTaskRepositoryTest.class);

    private InMemoryTaskRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskRepository();
    }

    @Test
    void saveAndFindById_roundTripsTask() {
        Task task = Task.create("A", null, null, LocalDate.now().plusDays(1));
        log.info("Input: save(Task id={}, title='{}')", task.getId(), task.getTitle());
        repository.save(task);

        Optional<Task> found = repository.findById(task.getId());
        boolean exists = repository.existsById(task.getId());
        log.info("Result: findById -> {}, existsById -> {}",
                found.map(t -> "Task(title='" + t.getTitle() + "')").orElse("empty"), exists);

        assertThat(found).contains(task);
        assertThat(exists).isTrue();
    }

    @Test
    void deleteById_removesTask() {
        Task task = Task.create("A", null, null, LocalDate.now().plusDays(1));
        repository.save(task);
        log.info("Input: deleteById({}) (task was previously saved)", task.getId());
        repository.deleteById(task.getId());

        Optional<Task> found = repository.findById(task.getId());
        boolean exists = repository.existsById(task.getId());
        log.info("Result: findById -> {}, existsById -> {}", found.isPresent() ? "present" : "empty", exists);

        assertThat(found).isEmpty();
        assertThat(exists).isFalse();
    }

    @Test
    void findAll_returnsTasksSortedByDueDateAscending() {
        Task t1 = Task.create("A", null, null, LocalDate.now().plusDays(5));
        Task t2 = Task.create("B", null, null, LocalDate.now().plusDays(1));
        Task t3 = Task.create("C", null, null, LocalDate.now().plusDays(3));
        repository.save(t1);
        repository.save(t2);
        repository.save(t3);
        log.info("Input: saved 3 tasks with due offsets [+5, +1, +3] days; calling findAll(null, 0, 10)");

        List<Task> result = repository.findAll(null, 0, 10);
        log.info("Result: ordered titles = {}", result.stream().map(Task::getTitle).toList());

        assertThat(result).extracting(Task::getTitle).containsExactly("B", "C", "A");
    }

    @Test
    void findAll_filtersByStatus() {
        Task done = Task.create("done", null, TaskStatus.DONE, LocalDate.now().plusDays(1));
        Task pending = Task.create("pending", null, TaskStatus.PENDING, LocalDate.now().plusDays(2));
        repository.save(done);
        repository.save(pending);
        log.info("Input: saved 1 DONE + 1 PENDING task; calling findAll(status=DONE, 0, 10)");

        List<Task> result = repository.findAll(TaskStatus.DONE, 0, 10);
        log.info("Result: size={}, titles={}", result.size(),
                result.stream().map(Task::getTitle).toList());

        assertThat(result).containsExactly(done);
    }

    @Test
    void findAll_paginatesResults() {
        for (int i = 1; i <= 5; i++) {
            repository.save(Task.create("t" + i, null, null, LocalDate.now().plusDays(i)));
        }
        log.info("Input: saved 5 tasks t1..t5; calling findAll(null, page, size=2) for page 0, 1, 2");

        List<Task> page0 = repository.findAll(null, 0, 2);
        List<Task> page1 = repository.findAll(null, 1, 2);
        List<Task> page2 = repository.findAll(null, 2, 2);
        log.info("Result: page0={}, page1={}, page2={}",
                page0.stream().map(Task::getTitle).toList(),
                page1.stream().map(Task::getTitle).toList(),
                page2.stream().map(Task::getTitle).toList());

        assertThat(page0).extracting(Task::getTitle).containsExactly("t1", "t2");
        assertThat(page1).extracting(Task::getTitle).containsExactly("t3", "t4");
        assertThat(page2).extracting(Task::getTitle).containsExactly("t5");
    }

}
