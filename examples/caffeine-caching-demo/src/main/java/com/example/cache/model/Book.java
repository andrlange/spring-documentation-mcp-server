package com.example.cache.model;

/**
 * Book record representing a book entity.
 * Uses Java 25 record feature for immutable data carrier.
 */
public record Book(
    Long id,
    String title,
    String author,
    String isbn,
    String genre,
    int publicationYear,
    String description
) {}
