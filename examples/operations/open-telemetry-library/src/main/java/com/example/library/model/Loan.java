package com.example.library.model;

import java.time.LocalDate;

/**
 * Represents a book loan in the library.
 */
public record Loan(
    Long id,
    String bookIsbn,
    Long memberId,
    LocalDate loanDate,
    LocalDate dueDate,
    LocalDate returnDate
) {
    /**
     * Returns true if the loan is overdue (past due date and not returned).
     */
    public boolean isOverdue() {
        return returnDate == null && LocalDate.now().isAfter(dueDate);
    }

    /**
     * Returns true if the loan is currently active (not yet returned).
     */
    public boolean isActive() {
        return returnDate == null;
    }

    /**
     * Creates a copy of this loan with the return date set.
     */
    public Loan withReturnDate(LocalDate date) {
        return new Loan(id, bookIsbn, memberId, loanDate, dueDate, date);
    }
}
