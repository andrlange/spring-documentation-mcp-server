package com.example.todo.dto;

import com.example.todo.entity.User;
import java.time.LocalDateTime;
import java.util.Set;

public record UserDto(
    Long id,
    String username,
    String email,
    String displayName,
    boolean enabled,
    Set<String> roles,
    LocalDateTime createdAt,
    int todoCount
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.isEnabled(),
            user.getRoles(),
            user.getCreatedAt(),
            user.getTodos().size()
        );
    }
}
