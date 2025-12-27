package com.example.library.service;

import com.example.library.data.DataInitializer;
import com.example.library.metrics.LibraryMetrics;
import com.example.library.model.Book;
import com.example.library.model.Loan;
import com.example.library.model.Member;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service for simulating library operations with configurable latency.
 * Used to generate traces for OpenTelemetry demonstration.
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final Tracer tracer;
    private final BookService bookService;
    private final MemberService memberService;
    private final LoanService loanService;
    private final LibraryMetrics metrics;
    private final DataInitializer dataInitializer;
    private final Random random = new Random();

    public SimulationService(Tracer tracer, BookService bookService,
                              MemberService memberService, LoanService loanService,
                              LibraryMetrics metrics, DataInitializer dataInitializer) {
        this.tracer = tracer;
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.metrics = metrics;
        this.dataInitializer = dataInitializer;
    }

    /**
     * Simulates a book search operation with configurable latency.
     */
    @Observed(name = "simulation.search", contextualName = "simulate-search")
    public SimulationResult simulateSearch(int latencyMs) {
        String traceId = Span.current().getSpanContext().getTraceId();
        log.info("Starting search simulation with latency {}ms, traceId: {}", latencyMs, traceId);

        Span span = tracer.spanBuilder("simulate-db-search").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("simulation.type", "search");
            span.setAttribute("simulation.latency_ms", latencyMs);

            // Simulate database query latency
            simulateLatency(latencyMs);

            // Perform actual search
            String[] searchTerms = {"Java", "Spring", "Python", "Data", "Cloud", "Web", "AI", "Machine"};
            String query = searchTerms[random.nextInt(searchTerms.length)];
            var results = bookService.search(query);

            span.setAttribute("simulation.results_count", results.size());
            log.info("Search completed: found {} results for '{}'", results.size(), query);

            metrics.recordBookSearch();

            return new SimulationResult(traceId, "search", latencyMs, results.size());
        } finally {
            span.end();
        }
    }

    /**
     * Simulates a loan creation operation with configurable latency.
     */
    @Observed(name = "simulation.loan", contextualName = "simulate-loan")
    public SimulationResult simulateLoan(int latencyMs) {
        String traceId = Span.current().getSpanContext().getTraceId();
        log.info("Starting loan simulation with latency {}ms, traceId: {}", latencyMs, traceId);

        Span span = tracer.spanBuilder("simulate-loan-creation").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("simulation.type", "loan");
            span.setAttribute("simulation.latency_ms", latencyMs);

            // Simulate database operations
            simulateLatency(latencyMs / 2);

            // Find an available book
            var availableBooks = bookService.findAvailable();
            if (availableBooks.isEmpty()) {
                log.warn("No available books for loan simulation");
                return new SimulationResult(traceId, "loan", latencyMs, 0);
            }

            // Find an active member
            var activeMembers = memberService.findActive();
            if (activeMembers.isEmpty()) {
                log.warn("No active members for loan simulation");
                return new SimulationResult(traceId, "loan", latencyMs, 0);
            }

            Book book = availableBooks.get(random.nextInt(availableBooks.size()));
            Member member = activeMembers.get(random.nextInt(activeMembers.size()));

            // Simulate additional processing
            simulateLatency(latencyMs / 2);

            span.setAttribute("simulation.book_isbn", book.isbn());
            span.setAttribute("simulation.member_id", member.id());

            try {
                Loan loan = loanService.createLoan(book.isbn(), member.id());
                log.info("Loan created: {} borrowed '{}' by {}", loan.id(), book.title(), member.name());
                metrics.recordLoanCreated();
                return new SimulationResult(traceId, "loan", latencyMs, 1);
            } catch (Exception e) {
                log.warn("Failed to create loan: {}", e.getMessage());
                span.recordException(e);
                return new SimulationResult(traceId, "loan", latencyMs, 0);
            }
        } finally {
            span.end();
        }
    }

    /**
     * Simulates a book return operation with configurable latency.
     */
    @Observed(name = "simulation.return", contextualName = "simulate-return")
    public SimulationResult simulateReturn(int latencyMs) {
        String traceId = Span.current().getSpanContext().getTraceId();
        log.info("Starting return simulation with latency {}ms, traceId: {}", latencyMs, traceId);

        Span span = tracer.spanBuilder("simulate-book-return").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("simulation.type", "return");
            span.setAttribute("simulation.latency_ms", latencyMs);

            // Find an active loan
            var activeLoans = loanService.findActive();
            if (activeLoans.isEmpty()) {
                log.warn("No active loans for return simulation");
                return new SimulationResult(traceId, "return", latencyMs, 0);
            }

            Loan loan = activeLoans.get(random.nextInt(activeLoans.size()));

            // Simulate processing time
            simulateLatency(latencyMs);

            span.setAttribute("simulation.loan_id", loan.id());

            Loan returnedLoan = loanService.returnLoan(loan.id());
            log.info("Loan {} returned successfully", returnedLoan.id());
            metrics.recordLoanReturned();

            return new SimulationResult(traceId, "return", latencyMs, 1);
        } finally {
            span.end();
        }
    }

    /**
     * Simulates mixed operations (search, loan, return) with configurable latency.
     */
    @Observed(name = "simulation.mixed", contextualName = "simulate-mixed")
    public List<SimulationResult> simulateMixed(int count, int latencyMs) {
        String traceId = Span.current().getSpanContext().getTraceId();
        log.info("Starting mixed simulation: {} operations with latency {}ms, traceId: {}",
                count, latencyMs, traceId);

        List<SimulationResult> results = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int operation = random.nextInt(3);
            SimulationResult result = switch (operation) {
                case 0 -> simulateSearch(latencyMs);
                case 1 -> simulateLoan(latencyMs);
                case 2 -> simulateReturn(latencyMs);
                default -> null;
            };
            if (result != null) {
                results.add(result);
            }
        }

        log.info("Mixed simulation completed: {} operations", results.size());
        return results;
    }

    /**
     * Runs batch search operations.
     */
    public List<SimulationResult> runSearchBatch(int count, int latencyMs) {
        List<SimulationResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(simulateSearch(latencyMs));
        }
        return results;
    }

    /**
     * Runs batch loan operations.
     */
    public List<SimulationResult> runLoanBatch(int count, int latencyMs) {
        List<SimulationResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(simulateLoan(latencyMs));
        }
        return results;
    }

    /**
     * Runs batch return operations.
     */
    public List<SimulationResult> runReturnBatch(int count, int latencyMs) {
        List<SimulationResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(simulateReturn(latencyMs));
        }
        return results;
    }

    private void simulateLatency(int milliseconds) {
        if (milliseconds > 0) {
            Span span = tracer.spanBuilder("simulated-latency").startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("latency.duration_ms", milliseconds);
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Latency simulation interrupted");
            } finally {
                span.end();
            }
        }
    }

    /**
     * Result of a simulation operation.
     */
    public record SimulationResult(
            String traceId,
            String operation,
            int duration,
            int resultCount
    ) {}
}
