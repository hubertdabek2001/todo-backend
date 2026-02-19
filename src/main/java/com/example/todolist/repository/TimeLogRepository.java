package com.example.todolist.repository;

import com.example.todolist.model.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    // Szukanie aktywnego stopera (bez endTime) dla konkretnego elementu
    Optional<TimeLog> findByTodoListIdAndEndTimeIsNull(Long listId);
    Optional<TimeLog> findByTaskIdAndEndTimeIsNull(Long taskId);
    Optional<TimeLog> findBySubTaskIdAndEndTimeIsNull(Long subTaskId);

    // Historia logów
    List<TimeLog> findByTodoListIdOrderByStartTimeDesc(Long listId);
    List<TimeLog> findByTaskIdOrderByStartTimeDesc(Long taskId);
    List<TimeLog> findBySubTaskIdOrderByStartTimeDesc(Long subTaskId);
}