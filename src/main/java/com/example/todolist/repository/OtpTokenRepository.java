package com.example.todolist.repository;

import com.example.todolist.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, String> {
    Optional<OtpToken> findByEmailAndCode(String email, String code);
    void deleteByEmail(String email); // do czyszczenia starych kodów
}