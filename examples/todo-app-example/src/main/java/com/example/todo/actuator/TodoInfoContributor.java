package com.example.todo.actuator;

import com.example.todo.dto.TodoStats;
import com.example.todo.service.TodoService;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TodoInfoContributor implements InfoContributor {

    private final TodoService todoService;

    public TodoInfoContributor(TodoService todoService) {
        this.todoService = todoService;
    }

    @Override
    public void contribute(Info.Builder builder) {
        TodoStats stats = todoService.getStats();

        builder.withDetail("todo-app", Map.of(
            "description", "Spring Boot 4 Todo Application Example",
            "features", Map.of(
                "multiUser", true,
                "authentication", "Spring Security with JPA",
                "database", "PostgreSQL",
                "ui", "Thymeleaf with Bootstrap dark theme"
            ),
            "statistics", Map.of(
                "users", stats.totalUsers(),
                "todos", stats.totalTodos()
            )
        ));
    }
}
