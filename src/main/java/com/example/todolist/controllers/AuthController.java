// src/main/java/com/example/todolist/controllers/AuthController.java
package com.example.todolist.controllers;

import com.example.todolist.model.RefreshToken;
import com.example.todolist.model.User;
import com.example.todolist.repository.RefreshTokenRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.security.jwt.JwtUtils;
import com.example.todolist.security.services.OtpService;
import com.example.todolist.security.services.RefreshTokenService;
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
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    public static class OtpRequest { public String email; }
    public static class VerifyRequest { public String email; public String code; }
    public static class ProfileRequest { public String username; }
    public static class TokenRefreshRequest { public String refreshToken; }

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

        User user = userRepository.findByEmail(req.email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(req.email);
            newUser.setRole("ROLE_USER");
            return userRepository.save(newUser);
        });

        // 1. Generujemy krótki Access Token
        String jwt = jwtUtils.generateJwtTokenFromEmail(user.getEmail());

        // 2. Usuwamy stare sesje dla bezpieczeństwa i generujemy długi Refresh Token
        refreshTokenService.deleteByUserId(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        boolean needsSetup = (user.getUsername() == null || user.getUsername().trim().isEmpty());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", jwt);
        response.put("refreshToken", refreshToken.getToken()); // <-- Dodane
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

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshtoken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.refreshToken;

        try {
            return refreshTokenRepository.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        String token = jwtUtils.generateJwtTokenFromEmail(user.getEmail());
                        return ResponseEntity.ok(Map.of(
                                "accessToken", token,
                                "refreshToken", requestRefreshToken
                        ));
                    })
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono Refresh Tokena w bazie!"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(Authentication authentication) {
        String email = authentication.getName();
        refreshTokenService.deleteByUserId(email);
        return ResponseEntity.ok(Map.of("message", "Wylogowano pomyślnie. Zniszczono Refresh Token."));
    }

    public static class UserProfileDto {
        public String email;
        public String username;
        public String firstName;
        public String lastName;

        public UserProfileDto(String email, String username, String firstName, String lastName) {
            this.email = email;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    public static class UpdateProfileRequest {
        public String username;
        public String firstName;
        public String lastName;
    }
    // --- ENDPOINT POBIERAJĄCY PROFIL ---
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        // Poprawne wyciągnięcie e-maila
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String email = userDetails.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        return ResponseEntity.ok(new UserProfileDto(
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName()
        ));
    }

    // --- ENDPOINT AKTUALIZUJĄCY PROFIL ---
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest req, Authentication authentication) {
        // Poprawne wyciągnięcie e-maila
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String email = userDetails.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        // Sprawdzamy, czy ktoś nie próbuje zająć już istniejącego username
        if (req.username != null && !req.username.equals(user.getUsername()) && userRepository.existsByUsername(req.username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ta nazwa użytkownika jest już zajęta."));
        }

        user.setUsername(req.username);
        user.setFirstName(req.firstName);
        user.setLastName(req.lastName);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profil zaktualizowany pomyślnie!"));
    }



}