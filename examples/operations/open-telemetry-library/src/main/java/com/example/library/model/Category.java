package com.example.library.model;

/**
 * Represents a book category in the library.
 */
public record Category(
    Long id,
    String name,
    String description
) {}
