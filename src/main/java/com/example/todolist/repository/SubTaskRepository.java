package com.example.todolist.repository;

import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubTaskRepository extends JpaRepository<SubTask, String> {

    List<SubTask> findByTaskIn(List<Task> tasks);
}