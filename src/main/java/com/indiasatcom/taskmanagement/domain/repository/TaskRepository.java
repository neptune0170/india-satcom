package com.indiasatcom.taskmanagement.domain.repository;

import com.indiasatcom.taskmanagement.domain.model.Task;
import com.indiasatcom.taskmanagement.domain.model.TaskId;
import com.indiasatcom.taskmanagement.domain.model.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Domain port for Task persistence. The infrastructure layer
 * supplies an implementation; the application layer depends only
 * on this abstraction.
 */
public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(TaskId id);

    boolean existsById(TaskId id);

    void deleteById(TaskId id);

    /**
     * Returns tasks ordered by due_date ascending. An optional status filter
     * narrows results to a single status when supplied. Paging is applied
     * after sorting and filtering.
     *
     * @param status optional status filter; null returns all statuses
     * @param page   zero-based page index (must be >= 0)
     * @param size   page size (must be > 0)
     */
    List<Task> findAll(TaskStatus status, int page, int size);
}
