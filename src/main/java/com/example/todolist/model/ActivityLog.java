// src/main/java/com/example/todolist/model/ActivityLog.java
package com.example.todolist.model;

import com.example.todolist.model.enums.ActionType;
import com.example.todolist.model.enums.EntityType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_logs")
@Data
public class ActivityLog {

    @Id
    private String id;

    @Column(nullable = false)
    private String listId; // Do jakiej listy należy zdarzenie

    @Column(nullable = false)
    private String userId; // Kto wywołał zdarzenie

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    private String entityName; // Np. "Zaprojektuj bazę", by nie musieć robić joinów do usuniętych zadań

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}