package com.example.todolist.security.websocket;

import com.example.todolist.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PresenceEventListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Map: listId -> Map(sessionId -> userDetails)
    private final Map<String, Map<String, UserDetailsImpl>> activeSubscriptions = new ConcurrentHashMap<>();

    // Map: sessionId -> set of listIds user is subscribed to (for quick cleanup on disconnect)
    private final Map<String, Set<String>> sessionToListIds = new ConcurrentHashMap<>();

    // Map: sessionId -> subscriptionId -> listId (to handle unsubscriptions properly)
    private final Map<String, Map<String, String>> sessionSubscriptionToListId = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        String subscriptionId = headers.getSubscriptionId();
        String destination = headers.getDestination();
        Principal userPrincipal = headers.getUser();

        if (destination != null && destination.startsWith("/topic/list/") && destination.endsWith("/presence")) {
            String listId = extractListId(destination);
            if (listId != null && userPrincipal instanceof UsernamePasswordAuthenticationToken) {
                Object principal = ((UsernamePasswordAuthenticationToken) userPrincipal).getPrincipal();
                if (principal instanceof UserDetailsImpl) {
                    UserDetailsImpl userDetails = (UserDetailsImpl) principal;

                    // Update Maps
                    activeSubscriptions.computeIfAbsent(listId, k -> new ConcurrentHashMap<>()).put(sessionId, userDetails);
                    sessionToListIds.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(listId);
                    sessionSubscriptionToListId.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(subscriptionId, listId);

                    broadcastPresence(listId);
                }
            }
        }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        // 1. Get all listIds this session was subscribed to
        Set<String> listIds = sessionToListIds.remove(sessionId);

        // 2. Remove session subscription mappings
        sessionSubscriptionToListId.remove(sessionId);

        // 3. Remove user from all active lists and broadcast
        if (listIds != null) {
            for (String listId : listIds) {
                Map<String, UserDetailsImpl> viewers = activeSubscriptions.get(listId);
                if (viewers != null) {
                    viewers.remove(sessionId);
                    if (viewers.isEmpty()) {
                        activeSubscriptions.remove(listId);
                    }
                }
                broadcastPresence(listId);
            }
        }
    }

    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        String subscriptionId = headers.getSubscriptionId();

        Map<String, String> subs = sessionSubscriptionToListId.get(sessionId);
        if (subs != null && subs.containsKey(subscriptionId)) {
            String listId = subs.remove(subscriptionId);

            // Clean up list subscriptions
            Map<String, UserDetailsImpl> listViewers = activeSubscriptions.get(listId);
            if (listViewers != null) {
                listViewers.remove(sessionId);
                if (listViewers.isEmpty()) {
                    activeSubscriptions.remove(listId);
                }
            }

            // Clean up session to list ids
            Set<String> listIds = sessionToListIds.get(sessionId);
            if (listIds != null) {
                listIds.remove(listId);
            }

            // Clean up sessionSubscriptionToListId if empty
            if (subs.isEmpty()) {
                sessionSubscriptionToListId.remove(sessionId);
            }

            broadcastPresence(listId);
        }
    }

    private String extractListId(String destination) {
        // Expected format: /topic/list/{listId}/presence
        String[] parts = destination.split("/");
        if (parts.length == 5 && parts[1].equals("topic") && parts[2].equals("list") && parts[4].equals("presence")) {
            return parts[3];
        }
        return null;
    }

    private void broadcastPresence(String listId) {
        Map<String, UserDetailsImpl> viewers = activeSubscriptions.getOrDefault(listId, Collections.emptyMap());

        List<Map<String, Object>> viewersList = viewers.values().stream()
                .distinct() // Deduplicate in case same user has multiple sessions
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", u.getId());
                    map.put("username", u.getUsername());
                    map.put("email", u.getEmail());
                    return map;
                })
                .collect(Collectors.toList());

        messagingTemplate.convertAndSend("/topic/list/" + listId + "/presence", viewersList);
    }
}
