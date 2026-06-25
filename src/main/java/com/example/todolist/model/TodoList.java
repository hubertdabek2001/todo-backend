package com.example.todolist.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "todo_lists")
@Data
@NoArgsConstructor
public class TodoList {
    @Id
    private String id;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private LocalDateTime dueDate;

    @Column(length = 7)
    @ColumnDefault("'#ffffff'") // UWAGA: dla tekstu (String) w SQL muszą być pojedyncze cudzysłowy wewnątrz podwójnych!
    private String primaryColor = "#ffffff";

    @Column(length = 7)
    @ColumnDefault("'#f8fafc'")
    private String secondaryColor = "#f8fafc";

    // --- WYMIARY I POZYCJA ---
    @ColumnDefault("0")
    private Integer posX = 0;

    @ColumnDefault("0")
    private Integer posY = 0;

    @ColumnDefault("350")
    private Integer width = 350;

    @ColumnDefault("500")
    private Integer height = 500;

    // --- STANY UI ---
    @ColumnDefault("false")
    private Boolean isMinimized = false;

    @ColumnDefault("true")
    private Boolean isOpen = true;

    @OneToMany(mappedBy = "todoList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    @ColumnDefault("false")
    private Boolean isDualColor = false;

    @Column(length = 7)
    @ColumnDefault("''") // Pusty string oznacza tryb "Auto"
    private String headerTextColor = "";

    @Column(length = 7)
    @ColumnDefault("''") // Pusty string oznacza domyślny ciemny szary
    private String taskTextColor = "";

    @ColumnDefault("0")
    private Long spentTimeSeconds = 0L;

    public Long getSpentTimeSeconds() {
        return this.spentTimeSeconds == null ? 0L : this.spentTimeSeconds;
    }

    @ColumnDefault("false")
    private Boolean isArchived = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "list_collaborators",
            joinColumns = @JoinColumn(name = "list_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @EqualsAndHashCode.Exclude // Ważne: zapobiega zapętleniu metody equals w Lombok
    @JsonIgnore
    private Set<User> collaborators = new HashSet<>();

}