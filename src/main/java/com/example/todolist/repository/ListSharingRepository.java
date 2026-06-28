package com.example.todolist.repository;

import com.example.todolist.model.ListSharing;
import com.example.todolist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ListSharingRepository extends JpaRepository<ListSharing, String> {
    List<ListSharing> findByUserAndStatus(User user, String status);
    Optional<ListSharing> findByListIdAndUser(String listId, User user);
}
