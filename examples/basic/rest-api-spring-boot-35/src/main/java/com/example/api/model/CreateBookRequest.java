package com.example.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Request DTO for creating a new book.
 * Uses Jakarta Bean Validation annotations.
 */
public record CreateBookRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Author is required")
    String author,

    @Pattern(regexp = "^(?=(?:\\D*\\d){10}(?:(?:\\D*\\d){3})?$)[\\d-]+$",
             message = "Invalid ISBN format")
    String isbn,

    LocalDate publishedDate,

    String genre,

    @Min(value = 1, message = "Pages must be at least 1")
    int pages
) {
    /**
     * Convert request to Book entity.
     */
    public Book toBook() {
        return Book.create(title, author, isbn, publishedDate, genre, pages);
    }
}
