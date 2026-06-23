// src/main/java/com/example/todolist/model/OtpToken.java
package com.example.todolist.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class OtpToken {
    @Id
    private String id;

    private String email;
    private String code;
    private LocalDateTime expiryDate;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}