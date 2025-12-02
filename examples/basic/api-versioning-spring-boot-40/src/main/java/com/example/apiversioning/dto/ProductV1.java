package com.example.apiversioning.dto;

/**
 * Product DTO for API Version 1.0
 *
 * Simple product representation with basic fields only.
 */
public record ProductV1(
    Long id,
    String name,
    double price
) {
    public static ProductV1 sample() {
        return new ProductV1(1L, "Spring Boot in Action", 49.99);
    }
}
