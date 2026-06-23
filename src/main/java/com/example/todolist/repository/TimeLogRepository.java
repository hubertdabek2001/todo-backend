package com.example.todolist.repository;

import com.example.todolist.model.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, String> {

    // Szukanie aktywnego stopera (bez endTime) dla konkretnego elementu
    Optional<TimeLog> findByTodoListIdAndEndTimeIsNull(String listId);
    Optional<TimeLog> findByTaskIdAndEndTimeIsNull(String taskId);
    Optional<TimeLog> findBySubTaskIdAndEndTimeIsNull(String subTaskId);

    // Historia logów
    List<TimeLog> findByTodoListIdOrderByStartTimeDesc(String listId);
    List<TimeLog> findByTaskIdOrderByStartTimeDesc(String taskId);
    List<TimeLog> findBySubTaskIdOrderByStartTimeDesc(String subTaskId);
}