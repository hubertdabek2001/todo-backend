package com.example.todolist.controllers;

import com.example.todolist.model.ActivityLog;
import com.example.todolist.model.TodoList;
import com.example.todolist.model.User;
import com.example.todolist.repository.ActivityLogRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired private ActivityLogRepository activityLogRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private TodoListRepository listRepo;

    @GetMapping("/{listId}")
    public ResponseEntity<?> getListActivity(@PathVariable String listId, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepo.findByEmail(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        TodoList list = listRepo.findById(listId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono listy"));

        // Zabezpieczenie: Czy jesteś właścicielem lub współpracownikiem?
        boolean isOwner = list.getUser().getId().equals(currentUser.getId());
        boolean isCollaborator = list.getCollaborators().stream().anyMatch(c -> c.getId().equals(currentUser.getId()));

        if (!isOwner && !isCollaborator) {
            return ResponseEntity.status(403).body(Map.of("error", "Brak dostępu do historii tej listy."));
        }

        // Pobranie chronologicznej osi czasu z bazy
        List<ActivityLog> logs = activityLogRepo.findByListIdOrderByTimestampDesc(listId);

        // Tłumaczymy logi na format JSON (DTO) dodając ładne "Imię" autora
        // Tłumaczymy logi na format JSON (DTO) dodając ładne "Imię" autora
        List<Map<String, Object>> response = logs.stream().map(log -> {
            String authorName = "Nieznany użytkownik";
            var authorOpt = userRepo.findById(log.getUserId());
            if (authorOpt.isPresent()) {
                User author = authorOpt.get();
                authorName = (author.getFirstName() != null && !author.getFirstName().isEmpty())
                        ? author.getFirstName()
                        : "@" + author.getUsername();
            }

            // Używamy HashMap zamiast Map.of, aby wymusić typ <String, Object> i uodpornić na nulle
            Map<String, Object> logMap = new java.util.HashMap<>();
            logMap.put("id", log.getId());
            logMap.put("actionType", log.getActionType().name());
            logMap.put("entityType", log.getEntityType().name());
            logMap.put("entityName", log.getEntityName() != null ? log.getEntityName() : "");
            logMap.put("timestamp", log.getTimestamp().toString());
            logMap.put("authorName", authorName);

            return logMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}