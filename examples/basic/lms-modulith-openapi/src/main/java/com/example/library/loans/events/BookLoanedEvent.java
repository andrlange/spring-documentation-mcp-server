package com.example.library.loans.events;

import java.time.LocalDate;

/**
 * Event published when a book is loaned.
 */
public record BookLoanedEvent(
    Long loanId,
    String bookIsbn,
    Long memberId,
    LocalDate dueDate
) {}
