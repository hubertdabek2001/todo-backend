package com.example.todolist.security.services;

import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.model.TimeLog;
import com.example.todolist.model.TodoList;
import com.example.todolist.repository.SubTaskRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.TimeLogRepository;
import com.example.todolist.repository.TodoListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class TimeTrackingService {

    @Autowired private TimeLogRepository timeLogRepository;
    @Autowired private TodoListRepository listRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private SubTaskRepository subTaskRepository;

    // --- START TIMERA ---
    public TimeLog startTimer(String type, String entityId) {
        TimeLog log = new TimeLog();
        log.setStartTime(LocalDateTime.now());

        switch (type.toUpperCase()) {
            case "LIST":
                // Zabezpieczenie przed podwójnym timerem
                if(timeLogRepository.findByTodoListIdAndEndTimeIsNull(entityId).isPresent()) throw new RuntimeException("Timer already running");
                TodoList list = listRepository.findById(entityId).orElseThrow();
                log.setTodoList(list);
                break;
            case "TASK":
                if(timeLogRepository.findByTaskIdAndEndTimeIsNull(entityId).isPresent()) throw new RuntimeException("Timer already running");
                Task task = taskRepository.findById(entityId).orElseThrow();
                log.setTask(task);
                break;
            case "SUBTASK":
                if(timeLogRepository.findBySubTaskIdAndEndTimeIsNull(entityId).isPresent()) throw new RuntimeException("Timer already running");
                SubTask subTask = subTaskRepository.findById(entityId).orElseThrow();
                log.setSubTask(subTask);
                break;
            default:
                throw new IllegalArgumentException("Unknown target type");
        }
        return timeLogRepository.save(log);
    }

    // --- STOP TIMERA ---
    @Transactional
    public TimeLog stopTimer(String logId) {
        TimeLog log = timeLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found"));

        if (log.getEndTime() != null) {
            throw new RuntimeException("Timer already stopped");
        }

        log.setEndTime(LocalDateTime.now());
        long seconds = Duration.between(log.getStartTime(), log.getEndTime()).getSeconds();
        log.setDurationSeconds(seconds);

        // Kaskadowe dodawanie czasu
        addTimeToEntities(log, seconds);

        return timeLogRepository.save(log);
    }

    // --- DODAWANIE MANUALNE ---
    @Transactional
    public TimeLog addManualTime(String type, String entityId, Long durationSeconds) {
        TimeLog log = new TimeLog();
        log.setEndTime(LocalDateTime.now());
        log.setStartTime(LocalDateTime.now().minusSeconds(durationSeconds));
        log.setDurationSeconds(durationSeconds);

        switch (type.toUpperCase()) {
            case "LIST":
                log.setTodoList(listRepository.findById(entityId).orElseThrow());
                break;
            case "TASK":
                log.setTask(taskRepository.findById(entityId).orElseThrow());
                break;
            case "SUBTASK":
                log.setSubTask(subTaskRepository.findById(entityId).orElseThrow());
                break;
        }

        addTimeToEntities(log, durationSeconds);
        return timeLogRepository.save(log);
    }

    // --- METODA POMOCNICZA: KASKADOWE SUMOWANIE CZASU ---
    private void addTimeToEntities(TimeLog log, Long seconds) {
        if (log.getSubTask() != null) {
            SubTask st = log.getSubTask();
            st.setSpentTimeSeconds(st.getSpentTimeSeconds() + seconds);
            subTaskRepository.save(st);

            // Dodaj czas również do Taska matki
            if (st.getTask() != null) {
                Task t = st.getTask();
                t.setSpentTimeSeconds(t.getSpentTimeSeconds() + seconds);
                taskRepository.save(t);

                // Dodaj czas również do Listy głównej
                if (t.getTodoList() != null) {
                    TodoList l = t.getTodoList();
                    l.setSpentTimeSeconds(l.getSpentTimeSeconds() + seconds);
                    listRepository.save(l);
                }
            }
        }
        else if (log.getTask() != null) {
            Task t = log.getTask();
            t.setSpentTimeSeconds(t.getSpentTimeSeconds() + seconds);
            taskRepository.save(t);

            if (t.getTodoList() != null) {
                TodoList l = t.getTodoList();
                l.setSpentTimeSeconds(l.getSpentTimeSeconds() + seconds);
                listRepository.save(l);
            }
        }
        else if (log.getTodoList() != null) {
            TodoList l = log.getTodoList();
            l.setSpentTimeSeconds(l.getSpentTimeSeconds() + seconds);
            listRepository.save(l);
        }
    }
}