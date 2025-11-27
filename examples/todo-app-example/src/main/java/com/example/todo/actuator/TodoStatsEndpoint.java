package com.example.todo.actuator;

import com.example.todo.dto.TodoStats;
import com.example.todo.service.TodoService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Endpoint(id = "todostats")
public class TodoStatsEndpoint {

    private final TodoService todoService;

    public TodoStatsEndpoint(TodoService todoService) {
        this.todoService = todoService;
    }

    @ReadOperation
    public Map<String, Object> stats() {
        TodoStats stats = todoService.getStats();

        return Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "users", Map.of(
                "total", stats.totalUsers(),
                "active", stats.activeUsers()
            ),
            "todos", Map.of(
                "total", stats.totalTodos(),
                "pending", stats.pendingTodos(),
                "inProgress", stats.inProgressTodos(),
                "completed", stats.completedTodos(),
                "cancelled", stats.cancelledTodos(),
                "overdue", stats.overdueTodos(),
                "dueToday", stats.dueTodayTodos()
            ),
            "metrics", Map.of(
                "completionRate", calculateCompletionRate(stats),
                "averageTodosPerUser", calculateAverageTodosPerUser(stats)
            )
        );
    }

    private double calculateCompletionRate(TodoStats stats) {
        if (stats.totalTodos() == 0) return 0.0;
        return Math.round((double) stats.completedTodos() / stats.totalTodos() * 10000) / 100.0;
    }

    private double calculateAverageTodosPerUser(TodoStats stats) {
        if (stats.activeUsers() == 0) return 0.0;
        return Math.round((double) stats.totalTodos() / stats.activeUsers() * 100) / 100.0;
    }
}
