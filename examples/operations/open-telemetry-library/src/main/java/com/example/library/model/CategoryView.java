package com.example.library.model;

/**
 * View model for displaying categories with book count.
 */
public record CategoryView(
    Long id,
    String name,
    String description,
    int bookCount
) {
    /**
     * Create a CategoryView from a Category and book count.
     */
    public static CategoryView from(Category category, int bookCount) {
        return new CategoryView(
            category.id(),
            category.name(),
            category.description(),
            bookCount
        );
    }
}
