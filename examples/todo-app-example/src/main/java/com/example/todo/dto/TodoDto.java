package com.example.todo.dto;

import com.example.todo.entity.Todo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TodoDto(
    Long id,
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    String title,
    String description,
    Todo.Priority priority,
    Todo.Status status,
    LocalDate dueDate,
    boolean overdue,
    boolean dueToday,
    boolean dueSoon
) {
    public static TodoDto fromEntity(Todo todo) {
        return new TodoDto(
            todo.getId(),
            todo.getTitle(),
            todo.getDescription(),
            todo.getPriority(),
            todo.getStatus(),
            todo.getDueDate(),
            todo.isOverdue(),
            todo.isDueToday(),
            todo.isDueSoon()
        );
    }

    public static TodoDto empty() {
        return new TodoDto(null, "", "", Todo.Priority.MEDIUM, Todo.Status.PENDING, null, false, false, false);
    }
}
