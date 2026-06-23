// src/main/java/com/example/todolist/controllers/AuthController.java
package com.example.todolist.controllers;

import com.example.todolist.model.User;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.security.jwt.JwtUtils;
import com.example.todolist.security.services.OtpService;
import com.example.todolist.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private OtpService otpService;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtils jwtUtils;

    public static class OtpRequest { public String email; }
    public static class VerifyRequest { public String email; public String code; }
    public static class ProfileRequest { public String username; }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequest req) {
        otpService.generateAndSendOtp(req.email);
        return ResponseEntity.ok().body(Map.of("message", "Kod OTP został wysłany."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyRequest req) {
        boolean isValid = otpService.verifyOtp(req.email, req.code);
        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nieprawidłowy lub wygasły kod."));
        }

        // Sprawdzamy, czy użytkownik istnieje, jeśli nie - tworzymy "pusty" profil
        User user = userRepository.findByEmail(req.email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(req.email);
            newUser.setRole("ROLE_USER");
            return userRepository.save(newUser);
        });

        // Generowanie tokena na podstawie E-MAILA, nie username!
        String jwt = jwtUtils.generateJwtTokenFromEmail(user.getEmail());

        boolean needsSetup = (user.getUsername() == null || user.getUsername().trim().isEmpty());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("requiresProfileSetup", needsSetup);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/setup-profile")
    public ResponseEntity<?> setupProfile(@RequestBody ProfileRequest req, Authentication authentication) {

        // --- KLUCZOWA ZMIANA ---
        // Pobieramy pełny obiekt szczegółów użytkownika i wyciągamy z niego twardy e-mail
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String email = userDetails.getEmail();

        if (userRepository.existsByUsername(req.username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ta nazwa użytkownika jest już zajęta."));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika dla email: " + email));

        user.setUsername(req.username);
        userRepository.save(user);

        return ResponseEntity.ok().body(Map.of("message", "Profil ustawiony pomyślnie", "username", req.username));
    }
}