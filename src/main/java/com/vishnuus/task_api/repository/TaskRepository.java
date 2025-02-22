package com.vishnuus.task_api.repository;  // Changed from taskapi to task_api

import com.vishnuus.task_api.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByNameContaining(String name);
}