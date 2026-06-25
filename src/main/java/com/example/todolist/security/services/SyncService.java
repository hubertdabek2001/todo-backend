// src/main/java/com/example/todolist/security/services/SyncService.java
package com.example.todolist.security.services;

import com.example.todolist.model.*;
import com.example.todolist.model.enums.ActionType;
import com.example.todolist.model.enums.EntityType;
import com.example.todolist.payload.request.SyncRequest;
import com.example.todolist.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SyncService {

    @Autowired private TodoListRepository listRepo;
    @Autowired private TaskRepository taskRepo;
    @Autowired private SubTaskRepository subTaskRepo;
    @Autowired private ActivityLogRepository activityLogRepo; // Zastrzyk nowego repozytorium
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // Metoda pomocnicza do tworzenia logów
    private void logActivity(String listId, String userId, ActionType action, EntityType entity, String name) {
        if (listId == null) return;
        ActivityLog log = new ActivityLog();
        log.setListId(listId);
        log.setUserId(userId);
        log.setActionType(action);
        log.setEntityType(entity);
        log.setEntityName(name);
        activityLogRepo.save(log);

        messagingTemplate.convertAndSend("/topic/list" + listId, log);
    }

    @Transactional
    public void syncData(SyncRequest payload, User currentUser) {

        // 1. Synchronizacja List i zarządzanie "Współpracownikami"
        if (payload.getLists() != null) {
            for (SyncRequest.ListSyncDto dto : payload.getLists()) {
                Optional<TodoList> existingOpt = listRepo.findById(dto.getId());
                TodoList list;
                boolean isNew = false;

                if (existingOpt.isPresent()) {
                    list = existingOpt.get();
                    // Automatyczne spinanie Kolaboracji:
                    // Jeśli lista już istnieje, a jej główny właściciel to nie Ty, to znaczy, że dołączasz przez QR
                    if (!list.getUser().getId().equals(currentUser.getId())) {
                        list.getCollaborators().add(currentUser);
                    }
                } else {
                    list = new TodoList();
                    list.setId(dto.getId());
                    list.setUser(currentUser); // Zakładasz ją po raz pierwszy w chmurze
                    isNew = true;
                }

                list.setName(dto.getName());
                list.setIsArchived(dto.getIsArchived() != null ? dto.getIsArchived() : false);
                list.setSpentTimeSeconds(dto.getSpentTimeSeconds() != null ? dto.getSpentTimeSeconds() : 0L);

                listRepo.save(list);

                if (isNew) {
                    logActivity(list.getId(), currentUser.getId(), ActionType.CREATE, EntityType.LIST, list.getName());
                }
            }
        }

        // 2. Synchronizacja Zadań i śledzenie zmian "Zakończono"
        if (payload.getTasks() != null) {
            for (SyncRequest.TaskSyncDto dto : payload.getTasks()) {
                Optional<Task> existingTaskOpt = taskRepo.findById(dto.getId());
                Task task;
                boolean isNew = !existingTaskOpt.isPresent();
                boolean wasCompleted = false;

                if (isNew) {
                    task = new Task();
                    task.setId(dto.getId());
                    task.setUser(currentUser);
                } else {
                    task = existingTaskOpt.get();
                    wasCompleted = task.getIsCompleted();
                }

                task.setTitle(dto.getTitle());
                task.setDescription(dto.getDescription());
                task.setIsCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
                task.setSpentTimeSeconds(dto.getSpentTimeSeconds() != null ? dto.getSpentTimeSeconds() : 0L);

                if (dto.getTodoListId() != null) {
                    listRepo.findById(dto.getTodoListId()).ifPresent(task::setTodoList);
                }

                taskRepo.save(task);

                // Zapisywanie odpowiednich logów
                if (isNew && task.getTodoList() != null) {
                    logActivity(task.getTodoList().getId(), currentUser.getId(), ActionType.CREATE, EntityType.TASK, task.getTitle());
                } else if (!wasCompleted && task.getIsCompleted() && task.getTodoList() != null) {
                    // Ktoś odhaczył zadanie w trybie offline i właśnie to wysyła
                    logActivity(task.getTodoList().getId(), currentUser.getId(), ActionType.COMPLETE, EntityType.TASK, task.getTitle());
                }
            }
        }

        // 3. Synchronizacja Podzadań
        if (payload.getSubTasks() != null) {
            for (SyncRequest.SubTaskSyncDto dto : payload.getSubTasks()) {
                Optional<SubTask> existingSubOpt = subTaskRepo.findById(dto.getId());
                SubTask subTask = existingSubOpt.orElse(new SubTask());
                boolean isNew = !existingSubOpt.isPresent();
                boolean wasCompleted = !isNew && subTask.getIsCompleted();

                subTask.setId(dto.getId());
                subTask.setTitle(dto.getTitle());
                subTask.setIsCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
                subTask.setSpentTimeSeconds(dto.getSpentTimeSeconds() != null ? dto.getSpentTimeSeconds() : 0L);

                if (dto.getTaskId() != null) {
                    taskRepo.findById(dto.getTaskId()).ifPresent(subTask::setTask);
                }
                if (dto.getParentSubTaskId() != null) {
                    subTaskRepo.findById(dto.getParentSubTaskId()).ifPresent(subTask::setParentSubTask);
                }

                subTaskRepo.save(subTask);

                // Rejestrowanie logu dla podzadania - musimy wyciągnąć ListId z zadania nadrzędnego
                if (subTask.getTask() != null && subTask.getTask().getTodoList() != null) {
                    String listId = subTask.getTask().getTodoList().getId();
                    if (isNew) {
                        logActivity(listId, currentUser.getId(), ActionType.CREATE, EntityType.SUBTASK, subTask.getTitle());
                    } else if (!wasCompleted && subTask.getIsCompleted()) {
                        logActivity(listId, currentUser.getId(), ActionType.COMPLETE, EntityType.SUBTASK, subTask.getTitle());
                    }
                }
            }
        }
    }
    @Transactional(readOnly = true)
    public Map<String, Object> pullData(User currentUser) {
        List<TodoList> userLists = listRepo.findAllByOwnerOrCollaborator(currentUser);

        List<Task> userTasks = userLists.isEmpty() ?
                List.of() : taskRepo.findByTodoListIn(userLists);

        List<SubTask> userSubTasks = userTasks.isEmpty() ?
                List.of() : subTaskRepo.findByTaskIn(userTasks);

        // JAWNY ŚWIADEK TYPU Map.<String, Object>of zapobiega błędnej inferencji typów przez kompilator
        List<Map<String, Object>> listsDto = userLists.stream().map(l -> Map.<String, Object>of(
                "id", l.getId(),
                "name", l.getName(),
                "isArchived", l.getIsArchived() != null ? l.getIsArchived() : false,
                "spentTimeSeconds", l.getSpentTimeSeconds() != null ? l.getSpentTimeSeconds() : 0L,
                "isShared", !l.getCollaborators().isEmpty()
        )).toList();

        // JAWNY ŚWIADEK TYPU Map.<String, Object>of również tutaj
        List<Map<String, Object>> tasksDto = userTasks.stream().map(t -> Map.<String, Object>of(
                "id", t.getId(),
                "todoListId", t.getTodoList().getId(),
                "title", t.getTitle(),
                "isCompleted", t.getIsCompleted() != null ? t.getIsCompleted() : false,
                "spentTimeSeconds", t.getSpentTimeSeconds() != null ? t.getSpentTimeSeconds() : 0L
        )).toList();

        // Tutaj używamy HashMap, więc typ generyczny jest od początku twardo określony
        List<Map<String, Object>> subTasksDto = userSubTasks.stream().map(st -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", st.getId());
            map.put("taskId", st.getTask().getId());
            map.put("title", st.getTitle());
            map.put("isCompleted", st.getIsCompleted() != null ? st.getIsCompleted() : false);
            map.put("spentTimeSeconds", st.getSpentTimeSeconds() != null ? st.getSpentTimeSeconds() : 0L);
            if (st.getParentSubTask() != null) {
                map.put("parentSubTaskId", st.getParentSubTask().getId());
            }
            return map;
        }).toList();

        return Map.of(
                "lists", listsDto,
                "tasks", tasksDto,
                "subTasks", subTasksDto
        );
    }
}