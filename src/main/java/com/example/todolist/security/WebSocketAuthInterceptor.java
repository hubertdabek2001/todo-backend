package com.example.todolist.security;

import com.example.todolist.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("=================================================");
            System.out.println("[WEBSOCKET] Otrzymano żądanie CONNECT od klienta STOMP");

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            System.out.println("[WEBSOCKET] Nagłówek Authorization: " + (authHeader != null ? "OBECNY" : "BRAK"));

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String email = jwtUtils.getUserNameFromJwtToken(jwt);
                        System.out.println("[WEBSOCKET] Token prawidłowy. E-mail: " + email);

                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        accessor.setUser(authentication);
                        System.out.println("[WEBSOCKET] ✅ Pomyślnie uwierzytelniono sesję WebSocket!");
                    } else {
                        System.out.println("[WEBSOCKET] ❌ UWAGA: Token JWT jest nieprawidłowy lub wygasł!");
                    }
                } catch (Exception e) {
                    System.out.println("[WEBSOCKET] ❌ Błąd weryfikacji tokena: " + e.getMessage());
                }
            } else {
                System.out.println("[WEBSOCKET] ❌ Odrzucenie: Brak poprawnego nagłówka Bearer");
            }
            System.out.println("=================================================");
        }
        return message;
    }
}