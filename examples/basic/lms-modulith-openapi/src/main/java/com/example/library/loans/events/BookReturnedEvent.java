package com.example.library.loans.events;

import java.time.LocalDate;

/**
 * Event published when a book is returned.
 */
public record BookReturnedEvent(
    Long loanId,
    String bookIsbn,
    Long memberId,
    LocalDate returnDate,
    boolean wasOverdue,
    Double fineAmount
) {}
