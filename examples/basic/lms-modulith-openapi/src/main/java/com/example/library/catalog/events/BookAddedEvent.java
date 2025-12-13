package com.example.library.catalog.events;

import org.springframework.modulith.events.Externalized;

/**
 * Event published when a new book is added to the catalog.
 */
@Externalized("catalog.book.added::#{isbn()}")
public record BookAddedEvent(
    String isbn,
    String title,
    String authorName,
    Integer availableCopies
) {}
