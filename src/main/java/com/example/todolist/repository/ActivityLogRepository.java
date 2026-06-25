package com.example.todolist.repository;

import com.example.todolist.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {
    // Pozwoli na szybkie pobranie chronologicznej osi czasu dla konkretnej listy
    List<ActivityLog> findByListIdOrderByTimestampDesc(String listId);
}