package com.example.todo.actuator;

import com.example.todo.dto.TodoStats;
import com.example.todo.service.TodoService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class TodoHealthIndicator implements HealthIndicator {

    private final TodoService todoService;

    public TodoHealthIndicator(TodoService todoService) {
        this.todoService = todoService;
    }

    @Override
    public Health health() {
        try {
            TodoStats stats = todoService.getStats();

            Health.Builder builder = Health.up()
                .withDetail("totalUsers", stats.totalUsers())
                .withDetail("totalTodos", stats.totalTodos())
                .withDetail("overdueTodos", stats.overdueTodos());

            // Add warning if there are many overdue todos
            if (stats.overdueTodos() > 10) {
                builder = Health.status("WARNING")
                    .withDetail("totalUsers", stats.totalUsers())
                    .withDetail("totalTodos", stats.totalTodos())
                    .withDetail("overdueTodos", stats.overdueTodos())
                    .withDetail("message", "High number of overdue todos");
            }

            return builder.build();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
