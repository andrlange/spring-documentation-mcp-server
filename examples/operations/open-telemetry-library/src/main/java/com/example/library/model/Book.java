package com.example.library.model;

/**
 * Represents a book in the library catalog.
 */
public record Book(
    String isbn,
    String title,
    String author,
    Long categoryId,
    int publicationYear,
    int availableCopies,
    int totalCopies
) {
    /**
     * Creates a copy of this book with updated available copies.
     */
    public Book withAvailableCopies(int newAvailableCopies) {
        return new Book(isbn, title, author, categoryId, publicationYear, newAvailableCopies, totalCopies);
    }

    /**
     * Returns true if at least one copy is available for loan.
     */
    public boolean isAvailable() {
        return availableCopies > 0;
    }
}
