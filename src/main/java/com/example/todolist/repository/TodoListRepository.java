package com.example.todolist.repository;

import com.example.todolist.model.TodoList;
import com.example.todolist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TodoListRepository extends JpaRepository<TodoList, String> {
    List<TodoList> findByUserId(String userId);

    // Pobiera TYLKO AKTYWNE listy dla danego użytkownika (do Sidebara)
    List<TodoList> findByUserIdAndIsArchivedFalse(String userId);

    // Pobiera TYLKO ZARCHIWIZOWANE listy (do nowego ekranu Archiwum)
    List<TodoList> findByUserIdAndIsArchivedTrue(String userId);

    @Query("SELECT DISTINCT l FROM TodoList l LEFT JOIN l.collaborators c WHERE l.user = :user OR c = :user")
    List<TodoList> findAllByOwnerOrCollaborator(@Param("user") User user);

}