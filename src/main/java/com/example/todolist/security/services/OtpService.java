// src/main/java/com/example/todolist/security/services/OtpService.java
package com.example.todolist.security.services;

import com.example.todolist.model.OtpToken;
import com.example.todolist.repository.OtpTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public void generateAndSendOtp(String email) {
        // Usuwamy stare kody dla tego e-maila
        otpTokenRepository.deleteByEmail(email);

        // Generowanie 6-cyfrowego kodu
        String code = String.format("%06d", new Random().nextInt(999999));

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setCode(code);
        otpToken.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // Ważny 10 minut
        otpTokenRepository.save(otpToken);

        // Wyślij e-mail z kodem OTP
        emailService.sendOtpEmail(email, code);

        System.out.println("=================================================");
        System.out.println("WYGENEROWANO KOD OTP DLA " + email + ": " + code);
        System.out.println("=================================================");
    }

    public boolean verifyOtp(String email, String code) {
        return otpTokenRepository.findByEmailAndCode(email, code)
                .filter(token -> token.getExpiryDate().isAfter(LocalDateTime.now()))
                .isPresent();
    }
}