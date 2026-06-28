package com.example.todolist.controllers;

import com.example.todolist.model.ListSharing;
import com.example.todolist.model.TodoList;
import com.example.todolist.model.User;
import com.example.todolist.repository.ListSharingRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shares")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ListSharingController {

    @Autowired
    ListSharingRepository sharingRepository;

    @Autowired
    TodoListRepository listRepository;

    @Autowired
    UserRepository userRepository;

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getEmail()).orElseThrow();
    }

    @PostMapping("/invite")
    public ResponseEntity<?> inviteUser(@RequestBody Map<String, String> payload) {
        String listId = payload.get("listId");
        String email = payload.get("email");
        String permission = payload.getOrDefault("permission", "READ"); // READ or READ_WRITE

        User currentUser = getCurrentUser();
        TodoList list = listRepository.findById(listId).orElseThrow(() -> new RuntimeException("List not found"));

        if (!list.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Only the owner can invite users.");
        }

        User invitee = userRepository.findByEmail(email).orElse(null);
        if (invitee == null) {
            return ResponseEntity.status(404).body("User with email " + email + " not found.");
        }

        if (invitee.getId().equals(currentUser.getId())) {
            return ResponseEntity.status(400).body("You cannot invite yourself.");
        }

        Optional<ListSharing> existingShare = sharingRepository.findByListIdAndUser(listId, invitee);
        if (existingShare.isPresent()) {
            return ResponseEntity.status(400).body("User is already invited or a collaborator.");
        }

        ListSharing sharing = new ListSharing();
        sharing.setList(list);
        sharing.setUser(invitee);
        sharing.setStatus("PENDING");
        sharing.setPermission(permission);
        sharingRepository.save(sharing);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invitation sent.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitations")
    public ResponseEntity<?> getInvitations() {
        User currentUser = getCurrentUser();
        List<ListSharing> invitations = sharingRepository.findByUserAndStatus(currentUser, "PENDING");
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/{shareId}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable String shareId) {
        User currentUser = getCurrentUser();
        ListSharing sharing = sharingRepository.findById(shareId).orElseThrow(() -> new RuntimeException("Sharing not found"));

        if (!sharing.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        sharing.setStatus("ACCEPTED");
        sharingRepository.save(sharing);

        TodoList list = sharing.getList();
        list.getCollaborators().add(currentUser);
        listRepository.save(list);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invitation accepted");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{shareId}/decline")
    public ResponseEntity<?> declineInvitation(@PathVariable String shareId) {
        User currentUser = getCurrentUser();
        ListSharing sharing = sharingRepository.findById(shareId).orElseThrow(() -> new RuntimeException("Sharing not found"));

        if (!sharing.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Unauthorized");
        }

        sharing.setStatus("DECLINED");
        sharingRepository.save(sharing);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invitation declined");
        return ResponseEntity.ok(response);
    }
}
