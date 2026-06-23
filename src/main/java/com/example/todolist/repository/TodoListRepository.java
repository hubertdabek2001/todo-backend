package com.example.todolist.repository;

import com.example.todolist.model.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TodoListRepository extends JpaRepository<TodoList, String> {
    List<TodoList> findByUserId(String userId);

    // Pobiera TYLKO AKTYWNE listy dla danego użytkownika (do Sidebara)
    List<TodoList> findByUserIdAndIsArchivedFalse(String userId);

    // Pobiera TYLKO ZARCHIWIZOWANE listy (do nowego ekranu Archiwum)
    List<TodoList> findByUserIdAndIsArchivedTrue(String userId);

}