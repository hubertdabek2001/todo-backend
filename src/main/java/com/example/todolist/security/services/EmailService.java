package com.example.todolist.security.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ResourceLoader resourceLoader;

    public void sendOtpEmail(String to, String otpCode) {
        try {
            // Load the HTML template
            Resource resource = resourceLoader.getResource("classpath:templates/otp-email.html");
            String htmlContent;
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                htmlContent = FileCopyUtils.copyToString(reader);
            }

            // Replace the placeholder with the actual OTP code
            htmlContent = htmlContent.replace("{{OTP_CODE}}", otpCode);

            // Create and send the email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Your Verification Code");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("Email sent successfully to " + to);
        } catch (MessagingException | IOException e) {
            System.err.println("Failed to send OTP email to " + to);
            e.printStackTrace();
        }
    }
}
