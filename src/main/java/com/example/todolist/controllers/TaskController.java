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
    public List<Task> getTasksByList(@PathVariable Long listId) {
    return taskRepository.findByTodoListId(listId);
    }

    @PostMapping
    public Task addTask(@RequestBody Task task) {
        return taskService.saveTask(task, getCurrentUsername());
    }

    @PostMapping("/list/{listId}")
    public Task addTaskToList(@PathVariable Long listId, @RequestBody Task task) {

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
    public SubTask addSubTask(@PathVariable Long taskId, @RequestBody SubTask subTask) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        subTask.setTask(task);
        return subTaskRepository.save(subTask);
    }

    @PostMapping("/subtasks/{parentSubTaskId}/add")
    public SubTask addNestedSubTask(@PathVariable Long parentSubTaskId, @RequestBody SubTask subTask) {
        SubTask parent = subTaskRepository.findById(parentSubTaskId)
                .orElseThrow(() -> new RuntimeException("Parent SubTask not found"));
        subTask.setParentSubTask(parent);
        subTask.setTask(parent.getTask());
        return subTaskRepository.save(subTask);
    }

    @PatchMapping("/subtasks/{subTaskId}/toggle")
    public SubTask toggleSubTask(@PathVariable Long subTaskId) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("SubTask not found"));

        subTask.setIsCompleted(!subTask.getIsCompleted());

        return subTaskRepository.save(subTask);
    }

    @PatchMapping({"/{taskId}/toggle"})
    public Task toggleTaskStatus(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setIsCompleted(!task.getIsCompleted());
        return taskRepository.save(task);
    }

    @PatchMapping("/{taskId}/duedate")
    public Task updateTaskDueDate(@PathVariable Long taskId, @RequestBody(required = false) LocalDateTime date) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setDueDate(date);
        return taskRepository.save(task);
    }

    @PatchMapping("/subtasks/{subTaskId}/duedate")
    public SubTask updateSubTaskDueDate(@PathVariable Long subTaskId, @RequestBody(required = false) LocalDateTime date) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("SubTask not found"));
        subTask.setDueDate(date);
        return subTaskRepository.save(subTask);
    }

    @DeleteMapping("subtasks/{subTaskId}")
    public ResponseEntity<?> deleteSubTask(@PathVariable Long subTaskId) {
        subTaskRepository.deleteById(subTaskId);
        return ResponseEntity.ok("SubTask deleted successfully");
    }
}
