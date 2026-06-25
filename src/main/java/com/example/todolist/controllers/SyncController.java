package com.example.todolist.controllers;

import com.example.todolist.model.User;
import com.example.todolist.payload.request.SyncRequest;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.security.services.SyncService;
import com.example.todolist.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private SyncService syncService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/push")
    public ResponseEntity<?> pushData(@RequestBody SyncRequest payload) {

        // --- KLUCZOWA ZMIANA ---
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = userDetails.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika w trakcie synchronizacji"));

        // Przekazujemy ładunek do Huba
        syncService.syncData(payload, user);

        return ResponseEntity.ok(Map.of("message", "Synchronizacja zakończona pomyślnie."));
    }

    @GetMapping("/pull")
    public ResponseEntity<?> pullSyncData(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findByEmail(userDetails.getEmail())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono usera"));

        return ResponseEntity.ok(syncService.pullData(currentUser));
    }
}