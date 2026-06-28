package com.example.todolist.repository;

import com.example.todolist.model.ListLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ListLinkTokenRepository extends JpaRepository<ListLinkToken, String> {
    Optional<ListLinkToken> findByToken(String token);
}
