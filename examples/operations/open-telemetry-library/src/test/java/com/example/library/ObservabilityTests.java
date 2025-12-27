package com.example.library;

import com.example.library.service.BookService;
import com.example.library.service.CategoryService;
import com.example.library.service.LoanService;
import com.example.library.service.MemberService;
import com.example.library.service.SimulationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OpenTelemetry observability features.
 * Verifies metrics, tracing, and services work correctly.
 */
@SpringBootTest
class ObservabilityTests {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Tracer tracer;

    @Autowired
    private BookService bookService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SimulationService simulationService;

    @Test
    void tracerIsConfigured() {
        assertThat(tracer).isNotNull();
    }

    @Test
    void meterRegistryIsConfigured() {
        assertThat(meterRegistry).isNotNull();
    }

    @Test
    void simulationServiceCreatesTraceIds() {
        var result = simulationService.simulateSearch(10);
        assertThat(result).isNotNull();
        assertThat(result.traceId()).isNotEmpty();
        assertThat(result.operation()).isEqualTo("search");
    }

    @Test
    void simulationSearchBatchWorks() {
        var results = simulationService.runSearchBatch(3, 5);
        assertThat(results).hasSize(3);
        results.forEach(result -> {
            assertThat(result.traceId()).isNotEmpty();
            assertThat(result.operation()).isEqualTo("search");
        });
    }

    @Test
    void simulationLoanBatchWorks() {
        var results = simulationService.runLoanBatch(2, 5);
        assertThat(results).hasSize(2);
        results.forEach(result -> {
            assertThat(result.operation()).isEqualTo("loan");
        });
    }

    @Test
    void simulationMixedWorks() {
        var results = simulationService.simulateMixed(5, 5);
        assertThat(results).hasSize(5);
    }

    @Test
    void customMetricsAreRegistered() {
        // Trigger some operations to ensure metrics are recorded
        bookService.search("test");

        // Verify custom metrics exist in the registry
        var meters = meterRegistry.getMeters();
        assertThat(meters).isNotEmpty();

        // Check for library-related metrics
        var libraryMeters = meters.stream()
                .filter(m -> m.getId().getName().startsWith("library."))
                .toList();
        assertThat(libraryMeters).isNotEmpty();
    }

    @Test
    void observedBookServiceMethods() {
        // Service methods with @Observed should create spans
        var books = bookService.findAll();
        assertThat(books).hasSize(200);

        var available = bookService.findAvailable();
        assertThat(available).isNotEmpty();

        var searchResults = bookService.search("Java");
        assertThat(searchResults).isNotNull();
    }

    @Test
    void observedMemberServiceMethods() {
        var members = memberService.findAll();
        assertThat(members).hasSize(20);

        var activeMembers = memberService.findActive();
        assertThat(activeMembers).isNotEmpty();
    }

    @Test
    void observedLoanServiceMethods() {
        var loans = loanService.findAll();
        assertThat(loans).isNotEmpty();

        var activeLoans = loanService.findActive();
        assertThat(activeLoans).isNotNull();

        var overdueLoans = loanService.findOverdue();
        assertThat(overdueLoans).isNotNull();
    }

    @Test
    void observedCategoryServiceMethods() {
        var categories = categoryService.findAll();
        assertThat(categories).hasSize(10);
    }

    @Test
    void metricsCountAreCorrect() {
        assertThat(bookService.count()).isEqualTo(200);
        assertThat(memberService.count()).isEqualTo(20);
        assertThat(categoryService.count()).isEqualTo(10);
        assertThat(loanService.count()).isGreaterThan(0);
    }

    @Test
    void bookAvailabilityMetricsWork() {
        long totalCopies = bookService.countAvailable();
        assertThat(totalCopies).isGreaterThan(0);
    }

    @Test
    void memberActivityMetricsWork() {
        long activeMembers = memberService.countActive();
        assertThat(activeMembers).isGreaterThan(0);
        assertThat(activeMembers).isLessThanOrEqualTo(20);
    }

    @Test
    void loanMetricsWork() {
        long activeLoans = loanService.countActive();
        long overdueLoans = loanService.countOverdue();

        // Active and overdue should not be negative
        assertThat(activeLoans).isGreaterThanOrEqualTo(0);
        assertThat(overdueLoans).isGreaterThanOrEqualTo(0);
    }
}
