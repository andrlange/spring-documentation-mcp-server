package com.example.apiversioning.dto;

import java.time.LocalDateTime;

/**
 * Wrapper for API responses with metadata
 */
public record ApiResponse<T>(
    String apiVersion,
    LocalDateTime timestamp,
    T data
) {
    public static <T> ApiResponse<T> of(String version, T data) {
        return new ApiResponse<>(version, LocalDateTime.now(), data);
    }
}
