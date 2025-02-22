package com.vishnuus.task_api.controller;  // Changed from taskapi to task_api

import com.vishnuus.task_api.model.Task;
import com.vishnuus.task_api.model.TaskExecution;
import com.vishnuus.task_api.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam(required = false) String id) {
        if (id != null) {
            Optional<Task> task = taskRepository.findById(id);
            return task.isPresent() ? ResponseEntity.ok(task.get()) : ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(taskRepository.findAll());
    }

    @PutMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        if (task.getCommand() == null || task.getCommand().contains("rm") || task.getCommand().contains("sudo")) {
            return ResponseEntity.badRequest().build();
        }
        Task savedTask = taskRepository.save(task);
        return ResponseEntity.ok(savedTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(params = "name")
    public ResponseEntity<List<Task>> findTasksByName(@RequestParam String name) {
        List<Task> tasks = taskRepository.findByNameContaining(name);
        return tasks.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}/execute")
public ResponseEntity<Task> executeTask(@PathVariable String id) {
    Optional<Task> optionalTask = taskRepository.findById(id);
    if (!optionalTask.isPresent()) {
        return ResponseEntity.notFound().build();
    }
    Task task = optionalTask.get();

    try {
        String command = task.getCommand();
        StringBuilder output = new StringBuilder();
        Date startTime = new Date();

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // Use cmd.exe for Windows
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } else {
            // Use bash for Unix-like systems (Linux, macOS)
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        }

        Date endTime = new Date();
        TaskExecution execution = new TaskExecution(startTime, endTime, output.toString().trim());
        task.getTaskExecutions().add(execution);
        taskRepository.save(task);
        return ResponseEntity.ok(task);
    } catch (Exception e) {
        return ResponseEntity.status(500).build();
    }
}
}