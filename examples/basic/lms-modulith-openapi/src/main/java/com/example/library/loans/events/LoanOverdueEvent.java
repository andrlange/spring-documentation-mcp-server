package com.example.library.loans.events;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;

/**
 * Event published when a loan becomes overdue.
 *
 * <p>Externalized to topic 'library.loans.loan-overdue' for integration
 * with external systems (Kafka, RabbitMQ, etc.) when configured.
 */
@Externalized("library.loans.loan-overdue")
public record LoanOverdueEvent(
    Long loanId,
    String bookIsbn,
    Long memberId,
    LocalDate dueDate,
    int daysOverdue
) {}
