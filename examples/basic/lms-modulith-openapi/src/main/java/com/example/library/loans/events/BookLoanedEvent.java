package com.example.library.loans.events;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;

/**
 * Event published when a book is loaned.
 *
 * <p>Externalized to topic 'library.loans.book-loaned' for integration
 * with external systems (Kafka, RabbitMQ, etc.) when configured.
 */
@Externalized("library.loans.book-loaned")
public record BookLoanedEvent(
    Long loanId,
    String bookIsbn,
    Long memberId,
    LocalDate dueDate
) {}
