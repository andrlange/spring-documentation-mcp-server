package com.example.library.loans.events;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;

/**
 * Event published when a book is returned.
 *
 * <p>Externalized to topic 'library.loans.book-returned' for integration
 * with external systems (Kafka, RabbitMQ, etc.) when configured.
 */
@Externalized("library.loans.book-returned")
public record BookReturnedEvent(
    Long loanId,
    String bookIsbn,
    Long memberId,
    LocalDate returnDate,
    boolean wasOverdue,
    Double fineAmount
) {}
