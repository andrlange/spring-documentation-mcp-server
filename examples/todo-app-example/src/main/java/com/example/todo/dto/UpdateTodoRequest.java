package com.example.todo.dto;

import com.example.todo.entity.Todo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTodoRequest(
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    String title,
    String description,
    Todo.Priority priority,
    Todo.Status status,
    LocalDate dueDate
) {}
