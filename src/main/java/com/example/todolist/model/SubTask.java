package com.example.todolist.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name = "sub_tasks")
@Data
@NoArgsConstructor

public class SubTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime dueDate;

    @Column(columnDefinition = "boolean default false")
    @JsonProperty("completed")
    private Boolean isCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @JsonIgnore // Ważne: Żeby nie robić pętli w JSON (SubTask -> Task -> SubTask...)
    @ToString.Exclude
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_subtask_id")
    @JsonIgnore
    @ToString.Exclude
    private SubTask parentSubTask;

    @ColumnDefault("0")
    private Long spentTimeSeconds = 0L;

    @OneToMany(mappedBy = "parentSubTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubTask> children = new ArrayList<>();

    @JsonProperty("parentId")
    public Long getParentId() {
        return parentSubTask != null ? parentSubTask.getId() : null;
    }

    public Long getSpentTimeSeconds() {
        return this.spentTimeSeconds == null ? 0L : this.spentTimeSeconds;
    }
}
