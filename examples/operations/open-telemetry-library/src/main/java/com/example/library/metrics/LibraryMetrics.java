package com.example.library.metrics;

import com.example.library.service.BookService;
import com.example.library.service.CategoryService;
import com.example.library.service.LoanService;
import com.example.library.service.MemberService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Micrometer metrics for the library demo.
 * Provides counters for operations and gauges for current state.
 */
@Component
public class LibraryMetrics {

    private final MeterRegistry registry;

    // Counters for operations
    private final Counter bookSearchCounter;
    private final Counter loanCreatedCounter;
    private final Counter loanReturnedCounter;
    private final Counter simulationRequestCounter;

    // Atomic integers for gauges (updated by scheduled task)
    private final AtomicInteger activeLoanCount = new AtomicInteger(0);
    private final AtomicInteger overdueLoanCount = new AtomicInteger(0);

    public LibraryMetrics(MeterRegistry registry,
                          BookService bookService,
                          MemberService memberService,
                          CategoryService categoryService,
                          LoanService loanService) {
        this.registry = registry;

        // Register counters
        this.bookSearchCounter = Counter.builder("library.books.searches")
            .description("Number of book search operations")
            .tag("application", "opentelemetry-library")
            .register(registry);

        this.loanCreatedCounter = Counter.builder("library.loans.created")
            .description("Number of loans created")
            .tag("application", "opentelemetry-library")
            .register(registry);

        this.loanReturnedCounter = Counter.builder("library.loans.returned")
            .description("Number of loans returned")
            .tag("application", "opentelemetry-library")
            .register(registry);

        this.simulationRequestCounter = Counter.builder("library.simulation.requests")
            .description("Number of simulation requests")
            .tag("application", "opentelemetry-library")
            .register(registry);

        // Register gauges for static counts (these read from services)
        Gauge.builder("library.books.total", bookService, bs -> bs.count())
            .description("Total number of books in the catalog")
            .tag("application", "opentelemetry-library")
            .register(registry);

        Gauge.builder("library.books.available", bookService, bs -> bs.countAvailable())
            .description("Number of available book copies")
            .tag("application", "opentelemetry-library")
            .register(registry);

        Gauge.builder("library.members.total", memberService, ms -> ms.count())
            .description("Total number of members")
            .tag("application", "opentelemetry-library")
            .register(registry);

        Gauge.builder("library.members.active", memberService, ms -> ms.countActive())
            .description("Number of active members")
            .tag("application", "opentelemetry-library")
            .register(registry);

        Gauge.builder("library.categories.total", categoryService, cs -> cs.count())
            .description("Number of book categories")
            .tag("application", "opentelemetry-library")
            .register(registry);

        Gauge.builder("library.loans.total", loanService, ls -> ls.count())
            .description("Total number of loans")
            .tag("application", "opentelemetry-library")
            .register(registry);

        Gauge.builder("library.loans.active", loanService, ls -> ls.countActive())
            .description("Number of active loans")
            .tag("application", "opentelemetry-library")
            .register(registry);

        Gauge.builder("library.loans.overdue", loanService, ls -> ls.countOverdue())
            .description("Number of overdue loans")
            .tag("application", "opentelemetry-library")
            .register(registry);
    }

    /**
     * Record a book search operation.
     */
    public void recordBookSearch() {
        bookSearchCounter.increment();
    }

    /**
     * Record a loan creation.
     */
    public void recordLoanCreated() {
        loanCreatedCounter.increment();
    }

    /**
     * Record a loan return.
     */
    public void recordLoanReturned() {
        loanReturnedCounter.increment();
    }

    /**
     * Record a simulation request with its type.
     */
    public void recordSimulationRequest(String type, int latencyMs) {
        Counter.builder("library.simulation.requests")
            .description("Number of simulation requests")
            .tag("application", "opentelemetry-library")
            .tag("type", type)
            .tag("latency_ms", String.valueOf(latencyMs))
            .register(registry)
            .increment();
    }

    /**
     * Record simulation batch size.
     */
    public void recordSimulationBatch(int size) {
        Counter.builder("library.simulation.batch.total")
            .description("Total requests in simulation batches")
            .tag("application", "opentelemetry-library")
            .register(registry)
            .increment(size);
    }

    public MeterRegistry getRegistry() {
        return registry;
    }
}
