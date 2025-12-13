package com.example.library.loans.events;

import java.time.LocalDate;

/**
 * Event published when a loan becomes overdue.
 */
public record LoanOverdueEvent(
    Long loanId,
    String bookIsbn,
    Long memberId,
    LocalDate dueDate,
    int daysOverdue
) {}
