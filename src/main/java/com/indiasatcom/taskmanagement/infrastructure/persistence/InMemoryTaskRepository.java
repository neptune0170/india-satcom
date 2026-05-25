package com.indiasatcom.taskmanagement.infrastructure.persistence;

import com.indiasatcom.taskmanagement.domain.model.Task;
import com.indiasatcom.taskmanagement.domain.model.TaskId;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;
import com.indiasatcom.taskmanagement.domain.repository.TaskRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory implementation of {@link TaskRepository}.
 *
 * Note: this stores live Task references, so updates from the service layer
 * become visible immediately. Production code backed by a database would map
 * to and from persistence records here instead.
 */
@Repository
public class InMemoryTaskRepository implements TaskRepository {

    private final ConcurrentMap<TaskId, Task> store = new ConcurrentHashMap<>();

    @Override
    public Task save(Task task) {
        store.put(task.getId(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(TaskId id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(TaskId id) {
        store.remove(id);
    }

    @Override
    public List<Task> findAll(TaskStatus status, int page, int size) {
        return store.values().stream()
                .filter(task -> status == null || task.getStatus() == status)
                .sorted(Comparator.comparing(Task::getDueDate))
                .skip((long) page * size)
                .limit(size)
                .toList();
    }
}
