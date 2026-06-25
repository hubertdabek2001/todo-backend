// src/main/java/com/example/todolist/repository/RefreshTokenRepository.java
package com.example.todolist.repository;

import com.example.todolist.model.RefreshToken;
import com.example.todolist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    int deleteByUser(User user);
}