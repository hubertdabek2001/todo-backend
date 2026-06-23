package com.example.todolist.payload.request;

import lombok.Data;
import java.util.List;

@Data
public class SyncRequest {
    private List<ListSyncDto> lists;
    private List<TaskSyncDto> tasks;
    private List<SubTaskSyncDto> subTasks;

    @Data
    public static class ListSyncDto {
        private String id;
        private String name;
        private Boolean isArchived;
        private Boolean isShared;
        private Long spentTimeSeconds;
    }

    @Data
    public static class TaskSyncDto {
        private String id;
        private String todoListId;
        private String title;
        private String description;
        private Boolean isCompleted;
        private Long spentTimeSeconds;
    }

    @Data
    public static class SubTaskSyncDto {
        private String id;
        private String taskId;
        private String parentSubTaskId;
        private String title;
        private Boolean isCompleted;
        private Long spentTimeSeconds;
    }
}