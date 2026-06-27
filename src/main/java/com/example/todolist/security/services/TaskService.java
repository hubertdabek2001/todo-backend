package com.example.todolist.security.services;

import com.example.todolist.model.Task;
import com.example.todolist.model.User;
import com.example.todolist.payload.xml.TaskListXmlDto;
import com.example.todolist.payload.xml.TaskXmlDto;
import com.example.todolist.repository.TaskRepository;
import com.example.todolist.repository.UserRepository;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Task> getAllTasks(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return taskRepository.findByUserId(user.getId());
    }

    public Task saveTask(Task task, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        task.setUser(user);
        return taskRepository.save(task);
    }

    public void importTasksFromXml(MultipartFile file, String email) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        XmlMapper xmlMapper = new XmlMapper();
        TaskListXmlDto taskListXmlDto = xmlMapper.readValue(file.getInputStream(), TaskListXmlDto.class);

        if (taskListXmlDto.getTasks() != null) {
            for (TaskXmlDto xmlTask : taskListXmlDto.getTasks()) {
                Task newTask = new Task();
                newTask.setTitle(xmlTask.getTitle());
                newTask.setDescription(xmlTask.getDescription());
                newTask.setUser(user);
                newTask.setIsCompleted(false);

                taskRepository.save(newTask);
            }
        }
    }
    @Transactional
    public Task updateTaskDescription(String taskId, String newDescription) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono zadania o ID: " + taskId));

        // Ustawiamy nowy, długi tekst (jeśli null, zapisujemy pusty string dla bezpieczeństwa)
        task.setDescription(newDescription == null ? "" : newDescription);

        return taskRepository.save(task);
    }


}
