package com.example.todolist.security.services;

import com.example.todolist.model.*;
import com.example.todolist.payload.request.SyncRequest;
import com.example.todolist.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncService {

    @Autowired private TodoListRepository listRepo;
    @Autowired private TaskRepository taskRepo;
    @Autowired private SubTaskRepository subTaskRepo;

    @Transactional
    public void syncData(SyncRequest payload, User user) {

        // 1. Synchronizacja List
        if (payload.getLists() != null) {
            for (SyncRequest.ListSyncDto dto : payload.getLists()) {
                // Upsert: Pobierz z bazy, a jeśli nie ma (bo stworzono offline) to utwórz nowy obiekt
                TodoList list = listRepo.findById(dto.getId()).orElse(new TodoList());
                list.setId(dto.getId());
                list.setName(dto.getName());
                list.setIsArchived(dto.getIsArchived() != null ? dto.getIsArchived() : false);
                list.setSpentTimeSeconds(dto.getSpentTimeSeconds() != null ? dto.getSpentTimeSeconds() : 0L);
                list.setUser(user);

                listRepo.save(list);
            }
        }

        // 2. Synchronizacja Zadań
        if (payload.getTasks() != null) {
            for (SyncRequest.TaskSyncDto dto : payload.getTasks()) {
                Task task = taskRepo.findById(dto.getId()).orElse(new Task());
                task.setId(dto.getId());
                task.setTitle(dto.getTitle());
                task.setDescription(dto.getDescription());
                task.setIsCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
                task.setSpentTimeSeconds(dto.getSpentTimeSeconds() != null ? dto.getSpentTimeSeconds() : 0L);
                task.setUser(user);

                // Przypinamy relację do Listy
                if (dto.getTodoListId() != null) {
                    listRepo.findById(dto.getTodoListId()).ifPresent(task::setTodoList);
                }

                taskRepo.save(task);
            }
        }

        // 3. Synchronizacja Podzadań
        if (payload.getSubTasks() != null) {
            for (SyncRequest.SubTaskSyncDto dto : payload.getSubTasks()) {
                SubTask subTask = subTaskRepo.findById(dto.getId()).orElse(new SubTask());
                subTask.setId(dto.getId());
                subTask.setTitle(dto.getTitle());
                subTask.setIsCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
                subTask.setSpentTimeSeconds(dto.getSpentTimeSeconds() != null ? dto.getSpentTimeSeconds() : 0L);

                // Relacja do Zadania głównego
                if (dto.getTaskId() != null) {
                    taskRepo.findById(dto.getTaskId()).ifPresent(subTask::setTask);
                }
                // Relacja zagnieżdżona (Podzadanie w podzadaniu)
                if (dto.getParentSubTaskId() != null) {
                    subTaskRepo.findById(dto.getParentSubTaskId()).ifPresent(subTask::setParentSubTask);
                }

                subTaskRepo.save(subTask);
            }
        }
    }
}