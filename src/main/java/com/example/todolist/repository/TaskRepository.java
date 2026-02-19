package com.example.todolist.repository;

import com.example.todolist.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserId(Long userId);

    List<Task> findByTodoListId(Long todoListId);

    List<Task> findByUserIdAndIsCompleted(Long userId, boolean isCompleted);
}
