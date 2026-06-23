package com.example.todolist.repository;

import com.example.todolist.model.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubTaskRepository extends JpaRepository<SubTask, String> {
}