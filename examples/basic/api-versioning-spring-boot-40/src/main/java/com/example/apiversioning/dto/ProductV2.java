package com.example.apiversioning.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Product DTO for API Version 2.0
 *
 * Enhanced product representation with additional fields:
 * - description: Product description
 * - category: Product category
 * - tags: List of tags
 * - stock: Available stock count
 * - createdAt: Creation timestamp
 * - rating: Customer rating
 */
public record ProductV2(
    Long id,
    String name,
    double price,
    String description,
    String category,
    List<String> tags,
    int stock,
    LocalDateTime createdAt,
    Rating rating
) {
    public record Rating(double average, int count) {}

    public static ProductV2 sample() {
        return new ProductV2(
            1L,
            "Spring Boot in Action",
            49.99,
            "A comprehensive guide to building enterprise applications with Spring Boot 4",
            "Books",
            List.of("spring", "java", "microservices", "enterprise"),
            42,
            LocalDateTime.of(2025, 1, 15, 10, 30, 0),
            new Rating(4.8, 156)
        );
    }
}
