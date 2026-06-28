package com.example.todolist.controllers;

import com.example.todolist.model.ListLinkToken;
import com.example.todolist.model.ListSharing;
import com.example.todolist.model.TodoList;
import com.example.todolist.model.User;
import com.example.todolist.repository.ListLinkTokenRepository;
import com.example.todolist.repository.ListSharingRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shares")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ListLinkController {

    @Autowired
    ListLinkTokenRepository tokenRepository;

    @Autowired
    TodoListRepository listRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ListSharingRepository sharingRepository;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getEmail()).orElseThrow();
    }

    @PostMapping("/link/{listId}")
    public ResponseEntity<?> generateShareableLink(@PathVariable String listId, @RequestBody Map<String, String> payload) {
        User currentUser = getCurrentUser();
        TodoList list = listRepository.findById(listId).orElseThrow(() -> new RuntimeException("List not found"));

        if (!list.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Only the owner can generate a link.");
        }

        String permission = payload.getOrDefault("permission", "READ");

        ListLinkToken linkToken = new ListLinkToken();
        linkToken.setList(list);
        linkToken.setPermission(permission);
        linkToken.setExpiryDate(LocalDateTime.now().plusDays(7)); // Link expires in 7 days
        tokenRepository.save(linkToken);

        String link = baseUrl + "/api/shares/join/" + linkToken.getToken();

        Map<String, String> response = new HashMap<>();
        response.put("link", link);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/join/{token}")
    public ResponseEntity<?> joinViaLink(@PathVariable String token) {
        Optional<ListLinkToken> linkTokenOpt = tokenRepository.findByToken(token);
        if (linkTokenOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Invalid or expired link.");
        }

        ListLinkToken linkToken = linkTokenOpt.get();
        if (linkToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("Link has expired.");
        }

        // Redirect the user to a custom URL scheme that the mobile app handles
        String mobileDeepLink = "todolist://join?token=" + linkToken.getToken() + "&listId=" + linkToken.getList().getId();

        return ResponseEntity.status(302).location(URI.create(mobileDeepLink)).build();
    }

    @PostMapping("/join/{token}/confirm")
    public ResponseEntity<?> confirmJoinViaLink(@PathVariable String token) {
        User currentUser = getCurrentUser();

        Optional<ListLinkToken> linkTokenOpt = tokenRepository.findByToken(token);
        if (linkTokenOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Invalid or expired link.");
        }

        ListLinkToken linkToken = linkTokenOpt.get();
        if (linkToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body("Link has expired.");
        }

        TodoList list = linkToken.getList();

        if (list.getUser().getId().equals(currentUser.getId())) {
             return ResponseEntity.status(400).body("You are already the owner of this list.");
        }

        Optional<ListSharing> existingShare = sharingRepository.findByListIdAndUser(list.getId(), currentUser);
        if (existingShare.isPresent() && existingShare.get().getStatus().equals("ACCEPTED")) {
            return ResponseEntity.status(400).body("You are already a collaborator.");
        }

        if (existingShare.isPresent()) {
             ListSharing sharing = existingShare.get();
             sharing.setStatus("ACCEPTED");
             sharing.setPermission(linkToken.getPermission());
             sharingRepository.save(sharing);
        } else {
             ListSharing sharing = new ListSharing();
             sharing.setList(list);
             sharing.setUser(currentUser);
             sharing.setStatus("ACCEPTED");
             sharing.setPermission(linkToken.getPermission());
             sharingRepository.save(sharing);
        }

        list.getCollaborators().add(currentUser);
        listRepository.save(list);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully joined the list.");
        return ResponseEntity.ok(response);
    }
}
