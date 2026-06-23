package com.example.todolist.controllers;


import com.example.todolist.model.SubTask;
import com.example.todolist.model.Task;
import com.example.todolist.model.TodoList;
import com.example.todolist.model.User;
import com.example.todolist.repository.SubTaskRepository;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.TodoListRepository;
import com.example.todolist.repository.UserRepository;
import com.example.todolist.security.services.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TodoListRepository todoListRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SubTaskRepository subTaskRepository;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // Placeholder, replace with actual implementation
    }

    @GetMapping
    public List<Task> getTasks() {
        return taskService.getAllTasks(getCurrentUsername());
    }

    @GetMapping({"/list/{listId}"})
    public List<Task> getTasksByList(@PathVariable String listId) {
    return taskRepository.findByTodoListId(listId);
    }

    @PostMapping
    public Task addTask(@RequestBody Task task) {
        return taskService.saveTask(task, getCurrentUsername());
    }

    @PostMapping("/list/{listId}")
    public Task addTaskToList(@PathVariable String listId, @RequestBody Task task) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        TodoList list = todoListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        task.setUser(user);
        task.setTodoList(list);
        task.setIsCompleted(false);
        return taskRepository.save(task);
    }

    @PostMapping("/import")
    public ResponseEntity<?> importTasks(@RequestParam("file") MultipartFile file) {
        try {
            taskService.importTasksFromXml(file, getCurrentUsername());
            return ResponseEntity.ok("Tasks imported successfully");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error importing tasks: " + e.getMessage());
        }
    }

    @PostMapping("/{taskId}/subtasks")
    public SubTask addSubTask(@PathVariable String taskId, @RequestBody SubTask subTask) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        subTask.setTask(task);
        return subTaskRepository.save(subTask);
    }

    @PostMapping("/subtasks/{parentSubTaskId}/add")
    public SubTask addNestedSubTask(@PathVariable String parentSubTaskId, @RequestBody SubTask subTask) {
        SubTask parent = subTaskRepository.findById(parentSubTaskId)
                .orElseThrow(() -> new RuntimeException("Parent SubTask not found"));
        subTask.setParentSubTask(parent);
        subTask.setTask(parent.getTask());
        return subTaskRepository.save(subTask);
    }

    @PatchMapping("/subtasks/{subTaskId}/toggle")
    public SubTask toggleSubTask(@PathVariable String subTaskId) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("SubTask not found"));

        subTask.setIsCompleted(!subTask.getIsCompleted());

        return subTaskRepository.save(subTask);
    }

    @PatchMapping({"/{taskId}/toggle"})
    public Task toggleTaskStatus(@PathVariable String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setIsCompleted(!task.getIsCompleted());
        return taskRepository.save(task);
    }

    @PatchMapping("/{taskId}/duedate")
    public Task updateTaskDueDate(@PathVariable String taskId, @RequestBody(required = false) LocalDateTime date) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setDueDate(date);
        return taskRepository.save(task);
    }

    @PatchMapping("/subtasks/{subTaskId}/duedate")
    public SubTask updateSubTaskDueDate(@PathVariable String subTaskId, @RequestBody(required = false) LocalDateTime date) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("SubTask not found"));
        subTask.setDueDate(date);
        return subTaskRepository.save(subTask);
    }

    @PatchMapping("/{taskId}/description")
    public ResponseEntity<?> updateTaskDescription(
            @PathVariable String taskId,
            @RequestBody Map<String, String> payload) {

        try {
            // Wyciągamy wartość "description" z JSON-a
            String description = payload.get("description");
            Task updatedTask = taskService.updateTaskDescription(taskId, description);

            return ResponseEntity.ok(updatedTask);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Błąd podczas zapisu notatki: " + e.getMessage());
        }
    }

    @DeleteMapping("subtasks/{subTaskId}")
    public ResponseEntity<?> deleteSubTask(@PathVariable String subTaskId) {
        subTaskRepository.deleteById(subTaskId);
        return ResponseEntity.ok("SubTask deleted successfully");
    }

    @PatchMapping("/{taskId}/title")
    public ResponseEntity<?> updateTaskTitle(
            @PathVariable String taskId,
            @RequestParam String title) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zadania o ID: " + taskId));

        task.setTitle(title);
        taskRepository.save(task);

        return ResponseEntity.ok(task);
    }

    // --- AKTUALIZACJA TYTUŁU PODZADANIA ---
    @PatchMapping("/subtasks/{subTaskId}/title")
    public ResponseEntity<?> updateSubTaskTitle(
            @PathVariable String subTaskId,
            @RequestParam String title) {

        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono podzadania o ID: " + subTaskId));

        subTask.setTitle(title);
        subTaskRepository.save(subTask);

        return ResponseEntity.ok(subTask);
    }
}
