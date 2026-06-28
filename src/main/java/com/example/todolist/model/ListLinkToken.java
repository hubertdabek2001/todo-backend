package com.example.todolist.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "list_link_tokens")
@Data
@NoArgsConstructor
public class ListLinkToken {

    @Id
    private String id;

    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    @JsonIgnore
    private TodoList list;

    private String permission; // READ or READ_WRITE
    private LocalDateTime expiryDate;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.token == null) {
            this.token = UUID.randomUUID().toString();
        }
    }
}
