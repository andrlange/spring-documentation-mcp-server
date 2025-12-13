package com.example.library.config;

import com.example.library.catalog.api.BookService;
import com.example.library.loans.api.LoanService;
import com.example.library.members.api.MemberService;
import com.example.library.notifications.api.NotificationService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Actuator endpoint for detailed library metrics.
 * Accessible via /actuator/library endpoint.
 */
@Component
@Endpoint(id = "library")
public class LibraryMetricsEndpoint {

    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final NotificationService notificationService;

    public LibraryMetricsEndpoint(BookService bookService,
                                  MemberService memberService,
                                  LoanService loanService,
                                  NotificationService notificationService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.notificationService = notificationService;
    }

    @ReadOperation
    public Map<String, Object> libraryMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", LocalDateTime.now().toString());
        metrics.put("catalog", getCatalogMetrics());
        metrics.put("members", getMemberMetrics());
        metrics.put("loans", getLoanMetrics());
        metrics.put("notifications", getNotificationMetrics());
        return metrics;
    }

    @ReadOperation
    public Map<String, Object> libraryMetricsByModule(@Selector String module) {
        return switch (module.toLowerCase()) {
            case "catalog" -> getCatalogMetrics();
            case "members" -> getMemberMetrics();
            case "loans" -> getLoanMetrics();
            case "notifications" -> getNotificationMetrics();
            default -> Map.of("error", "Unknown module: " + module);
        };
    }

    private Map<String, Object> getCatalogMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalBooks", bookService.countBooks());
        metrics.put("availableBooks", bookService.countAvailableBooks());
        metrics.put("loanedBooks", bookService.countBooks() - bookService.countAvailableBooks());
        return metrics;
    }

    private Map<String, Object> getMemberMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalMembers", memberService.countMembers());
        metrics.put("activeMembers", memberService.countActiveMembers());
        metrics.put("premiumMembers", memberService.countPremiumMembers());
        metrics.put("standardMembers", memberService.countMembers() - memberService.countPremiumMembers());
        return metrics;
    }

    private Map<String, Object> getLoanMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeLoans", loanService.countActiveLoans());
        metrics.put("overdueLoans", loanService.countOverdueLoans());
        metrics.put("totalUnpaidFines", loanService.getTotalUnpaidFines());

        // Calculate health score (0-100)
        long activeLoans = loanService.countActiveLoans();
        long overdueLoans = loanService.countOverdueLoans();
        int healthScore = activeLoans == 0 ? 100 :
            (int) ((1 - ((double) overdueLoans / activeLoans)) * 100);
        metrics.put("healthScore", healthScore);

        return metrics;
    }

    private Map<String, Object> getNotificationMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("pendingNotifications", notificationService.countPending());
        metrics.put("failedNotifications", notificationService.countFailed());
        return metrics;
    }
}
