package com.example.todolist.controllers;

import com.example.todolist.model.TimeLog;
import com.example.todolist.repository.TimeLogRepository;
import com.example.todolist.security.services.TimeTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/time")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TimeTrackingController {

    @Autowired
    private TimeTrackingService timeTrackingService;

    @Autowired
    private TimeLogRepository timeLogRepository;

    // DTO do requestu
    public static class StartTimeRequest {
        public String type; // "LIST", "TASK", "SUBTASK"
        public String entityId;
    }

    public static class ManualTimeRequest extends StartTimeRequest {
        public Long durationSeconds;
    }

    @PostMapping("/start")
    public ResponseEntity<TimeLog> startTimer(@RequestBody StartTimeRequest req) {
        return ResponseEntity.ok(timeTrackingService.startTimer(req.type, req.entityId));
    }

    @PostMapping("/stop/{logId}")
    public ResponseEntity<TimeLog> stopTimer(@PathVariable String logId) {
        return ResponseEntity.ok(timeTrackingService.stopTimer(logId));
    }

    @PostMapping("/manual")
    public ResponseEntity<TimeLog> addManualTime(@RequestBody ManualTimeRequest req) {
        return ResponseEntity.ok(timeTrackingService.addManualTime(req.type, req.entityId, req.durationSeconds));
    }

    // Pobranie AKTYWNEGO timera dla danego elementu (potrzebne, aby po odświeżeniu strony w React stoper nadal leciał)
    @GetMapping("/active/{type}/{entityId}")
    public ResponseEntity<TimeLog> getActiveTimer(@PathVariable String type, @PathVariable String entityId) {
        Optional<TimeLog> activeLog = Optional.empty();
        switch (type.toUpperCase()) {
            case "LIST": activeLog = timeLogRepository.findByTodoListIdAndEndTimeIsNull(entityId); break;
            case "TASK": activeLog = timeLogRepository.findByTaskIdAndEndTimeIsNull(entityId); break;
            case "SUBTASK": activeLog = timeLogRepository.findBySubTaskIdAndEndTimeIsNull(entityId); break;
        }
        return activeLog.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    // Historia czasu
    @GetMapping("/history/{type}/{entityId}")
    public ResponseEntity<List<TimeLog>> getHistory(@PathVariable String type, @PathVariable String entityId) {
        switch (type.toUpperCase()) {
            case "LIST": return ResponseEntity.ok(timeLogRepository.findByTodoListIdOrderByStartTimeDesc(entityId));
            case "TASK": return ResponseEntity.ok(timeLogRepository.findByTaskIdOrderByStartTimeDesc(entityId));
            case "SUBTASK": return ResponseEntity.ok(timeLogRepository.findBySubTaskIdOrderByStartTimeDesc(entityId));
            default: return ResponseEntity.badRequest().build();
        }
    }
}