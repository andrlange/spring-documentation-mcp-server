package com.example.library.config;

import com.example.library.catalog.api.BookService;
import com.example.library.loans.api.LoanService;
import com.example.library.members.api.MemberService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the Library Management System.
 * Reports overall library health status including data statistics.
 */
@Component
public class LibraryHealthIndicator implements HealthIndicator {

    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;

    public LibraryHealthIndicator(BookService bookService,
                                  MemberService memberService,
                                  LoanService loanService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
    }

    @Override
    public Health health() {
        try {
            long totalBooks = bookService.countBooks();
            long availableBooks = bookService.countAvailableBooks();
            long totalMembers = memberService.countMembers();
            long activeMembers = memberService.countActiveMembers();
            long activeLoans = loanService.countActiveLoans();
            long overdueLoans = loanService.countOverdueLoans();

            Health.Builder builder = Health.up()
                .withDetail("catalog.totalBooks", totalBooks)
                .withDetail("catalog.availableBooks", availableBooks)
                .withDetail("members.total", totalMembers)
                .withDetail("members.active", activeMembers)
                .withDetail("loans.active", activeLoans)
                .withDetail("loans.overdue", overdueLoans);

            // Warn if more than 20% of loans are overdue
            if (activeLoans > 0 && overdueLoans > activeLoans * 0.2) {
                builder.status("DEGRADED")
                    .withDetail("warning", "High percentage of overdue loans");
            }

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
