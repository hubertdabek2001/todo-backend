package com.example.todolist.repository;

import com.example.todolist.model.Task;
import com.example.todolist.model.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {

    List<Task> findByUserId(String userId);

    List<Task> findByTodoListId(String todoListId);

    List<Task> findByUserIdAndIsCompleted(String userId, boolean isCompleted);

    List<Task> findByTodoListIn(List<TodoList> lists);
}
