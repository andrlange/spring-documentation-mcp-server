package com.example.ratelimiter.model;

/**
 * API Response record for rate-limited endpoint
 */
public record ApiResponse(
    long requestId,
    boolean success,
    String message,
    String timestamp
) {}
