package com.example.library.config;

import com.example.library.catalog.api.BookService;
import com.example.library.loans.api.LoanService;
import com.example.library.members.api.MemberService;
import com.example.library.notifications.api.NotificationService;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom info contributor that provides library statistics.
 * Accessible via /actuator/info endpoint.
 */
@Component
public class LibraryInfoContributor implements InfoContributor {

    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final NotificationService notificationService;

    public LibraryInfoContributor(BookService bookService,
                                  MemberService memberService,
                                  LoanService loanService,
                                  NotificationService notificationService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.notificationService = notificationService;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> libraryStats = new HashMap<>();

        // Catalog statistics
        Map<String, Object> catalogStats = new HashMap<>();
        catalogStats.put("totalBooks", bookService.countBooks());
        catalogStats.put("availableBooks", bookService.countAvailableBooks());
        libraryStats.put("catalog", catalogStats);

        // Member statistics
        Map<String, Object> memberStats = new HashMap<>();
        memberStats.put("totalMembers", memberService.countMembers());
        memberStats.put("activeMembers", memberService.countActiveMembers());
        memberStats.put("premiumMembers", memberService.countPremiumMembers());
        libraryStats.put("members", memberStats);

        // Loan statistics
        Map<String, Object> loanStats = new HashMap<>();
        loanStats.put("activeLoans", loanService.countActiveLoans());
        loanStats.put("overdueLoans", loanService.countOverdueLoans());
        loanStats.put("totalUnpaidFines", loanService.getTotalUnpaidFines());
        libraryStats.put("loans", loanStats);

        // Notification statistics
        Map<String, Object> notificationStats = new HashMap<>();
        notificationStats.put("pendingNotifications", notificationService.countPending());
        notificationStats.put("failedNotifications", notificationService.countFailed());
        libraryStats.put("notifications", notificationStats);

        builder.withDetail("library", libraryStats);
    }
}
