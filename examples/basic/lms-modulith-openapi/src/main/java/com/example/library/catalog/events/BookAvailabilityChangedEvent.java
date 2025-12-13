package com.example.library.catalog.events;

import org.springframework.modulith.events.Externalized;

/**
 * Event published when a book's availability changes (loaned or returned).
 */
@Externalized("catalog.book.availability::#{isbn()}")
public record BookAvailabilityChangedEvent(
    String isbn,
    String title,
    Integer previousAvailable,
    Integer currentAvailable,
    ChangeType changeType
) {
    public enum ChangeType {
        LOANED,
        RETURNED
    }
}
