package com.example.todo.dto;

public record TodoStats(
    long totalTodos,
    long pendingTodos,
    long inProgressTodos,
    long completedTodos,
    long cancelledTodos,
    long overdueTodos,
    long dueTodayTodos,
    long totalUsers,
    long activeUsers
) {}
