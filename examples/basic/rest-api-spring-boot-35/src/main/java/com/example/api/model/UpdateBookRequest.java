package com.example.api.model;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;

/**
 * Request DTO for updating an existing book.
 * All fields are optional - only provided fields will be updated.
 */
public record UpdateBookRequest(
    String title,
    String author,
    String isbn,
    LocalDate publishedDate,
    String genre,
    @Min(value = 1, message = "Pages must be at least 1")
    Integer pages
) {}
