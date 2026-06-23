package com.example.todolist.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "time_logs")
@NoArgsConstructor
@AllArgsConstructor
public class TimeLog {

    @Id
    private String id;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Czas trwania w sekundach (obliczany przy stopie lub podawany przy manualnym dodaniu)
    private Long durationSeconds;

    // Do czego przypisany jest ten czas (tylko jedno z tych pól będzie pełne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_list_id")
    @JsonIgnore
    private TodoList todoList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @JsonIgnore
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtask_id")
    @JsonIgnore
    private SubTask subTask;


}
