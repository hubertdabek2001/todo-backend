package com.example.todolist.controllers;

import com.example.todolist.model.TodoList;
import com.example.todolist.model.User;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/lists")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ListController {

    @Autowired
    TodoListRepository listRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping
    public List<TodoList> getUserLists() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return listRepository.findByUserId(user.getId());
    }

    @GetMapping("/{id}")
    public TodoList getListById(@PathVariable Long id) {
        return listRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("List not found"));
    }

    @PostMapping
    public TodoList createList(@RequestBody TodoList list) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        list.setUser(user);
        return listRepository.save(list);
    }

    @PatchMapping("/{id}/duedate")
    public TodoList updateListDueDate(@PathVariable Long id, @RequestBody(required = false) LocalDateTime date) {
        TodoList list = listRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("List not found"));
        list.setDueDate(date);
        return listRepository.save(list);
    }

    @PatchMapping("/{id}/colors")
    public TodoList updateListColors(
            @PathVariable Long id,
            @RequestParam String primary,
            @RequestParam String secondary,
            @RequestParam Boolean isDualColor,
            @RequestParam(required = false) String headerTextColor, // <- required = false jest ważne!
            @RequestParam(required = false) String taskTextColor) {

        TodoList list = listRepository.findById(id).orElseThrow();
        list.setPrimaryColor(primary);
        list.setSecondaryColor(secondary);
        list.setIsDualColor(isDualColor);
        list.setHeaderTextColor(headerTextColor);
        list.setTaskTextColor(taskTextColor);
        return listRepository.save(list);
    }

    @PatchMapping("/{id}/width")
    public TodoList updateListWidth(@PathVariable Long id, @RequestParam Integer width) {
        TodoList list = listRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("List not found"));

        list.setWidth(width);
        return listRepository.save(list);
    }

    @PatchMapping("/{id}/height")
    public TodoList updateListHeight(@PathVariable Long id, @RequestParam Integer height) {
        TodoList list = listRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("List not found"));

        list.setHeight(height);
        return listRepository.save(list);
    }

    @PatchMapping("/{id}/position")
    public TodoList updateListPosition(@PathVariable Long id, @RequestParam Integer x, @RequestParam Integer y) {
        TodoList list = listRepository.findById(id).orElseThrow();
        list.setPosX(x);
        list.setPosY(y);
        return listRepository.save(list);
    }

    // Aktualizacja stanu minimalizacji
    @PatchMapping("/{id}/minimize")
    public TodoList toggleMinimize(@PathVariable Long id, @RequestParam Boolean minimized) {
        TodoList list = listRepository.findById(id).orElseThrow();
        list.setIsMinimized(minimized);
        return listRepository.save(list);
    }

    @PatchMapping("/{id}/open")
    public TodoList updateListOpenStatus(@PathVariable Long id, @RequestParam Boolean isOpen) {
        TodoList list = listRepository.findById(id).orElseThrow();
        list.setIsOpen(isOpen);
        return listRepository.save(list);
    }

    @PatchMapping("/{id}/name")
    public TodoList updateListName(@PathVariable Long id, @RequestParam String name) {
        TodoList list = listRepository.findById(id).orElseThrow();
        list.setName(name);
        return listRepository.save(list);
    }




}