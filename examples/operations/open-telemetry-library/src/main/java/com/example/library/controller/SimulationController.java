package com.example.library.controller;

import com.example.library.service.SimulationService;
import com.example.library.service.SimulationService.SimulationResult;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for simulation endpoints.
 * Provides API for generating traces with configurable latency.
 */
@RestController
@RequestMapping("/api/simulate")
@Observed(name = "simulation.controller")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);
    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * Simulate search operations.
     *
     * @param count Number of search operations (default 1, max 100)
     * @param latencyMs Simulated latency in milliseconds (default 50)
     * @return List of simulation results with trace IDs
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> simulateSearch(
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "50") int latencyMs) {

        count = Math.min(Math.max(count, 1), 100);
        latencyMs = Math.max(latencyMs, 0);

        log.info("Simulating {} search operations with {}ms latency", count, latencyMs);

        List<SimulationResult> results = simulationService.runSearchBatch(count, latencyMs);

        return ResponseEntity.ok(Map.of(
                "operation", "search",
                "count", count,
                "latencyMs", latencyMs,
                "traces", results.stream().map(this::toTraceInfo).toList()
        ));
    }

    /**
     * Simulate loan operations.
     *
     * @param count Number of loan operations (default 1, max 100)
     * @param latencyMs Simulated latency in milliseconds (default 50)
     * @return List of simulation results with trace IDs
     */
    @PostMapping("/loan")
    public ResponseEntity<Map<String, Object>> simulateLoan(
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "50") int latencyMs) {

        count = Math.min(Math.max(count, 1), 100);
        latencyMs = Math.max(latencyMs, 0);

        log.info("Simulating {} loan operations with {}ms latency", count, latencyMs);

        List<SimulationResult> results = simulationService.runLoanBatch(count, latencyMs);

        return ResponseEntity.ok(Map.of(
                "operation", "loan",
                "count", count,
                "latencyMs", latencyMs,
                "traces", results.stream().map(this::toTraceInfo).toList()
        ));
    }

    /**
     * Simulate return operations.
     *
     * @param count Number of return operations (default 1, max 100)
     * @param latencyMs Simulated latency in milliseconds (default 50)
     * @return List of simulation results with trace IDs
     */
    @PostMapping("/return")
    public ResponseEntity<Map<String, Object>> simulateReturn(
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "50") int latencyMs) {

        count = Math.min(Math.max(count, 1), 100);
        latencyMs = Math.max(latencyMs, 0);

        log.info("Simulating {} return operations with {}ms latency", count, latencyMs);

        List<SimulationResult> results = simulationService.runReturnBatch(count, latencyMs);

        return ResponseEntity.ok(Map.of(
                "operation", "return",
                "count", count,
                "latencyMs", latencyMs,
                "traces", results.stream().map(this::toTraceInfo).toList()
        ));
    }

    /**
     * Simulate mixed operations (search, loan, return).
     *
     * @param count Number of mixed operations (default 10, max 100)
     * @param latencyMs Simulated latency in milliseconds (default 50)
     * @return List of simulation results with trace IDs
     */
    @PostMapping("/mixed")
    public ResponseEntity<Map<String, Object>> simulateMixed(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "50") int latencyMs) {

        count = Math.min(Math.max(count, 1), 100);
        latencyMs = Math.max(latencyMs, 0);

        log.info("Simulating {} mixed operations with {}ms latency", count, latencyMs);

        List<SimulationResult> results = simulationService.simulateMixed(count, latencyMs);

        return ResponseEntity.ok(Map.of(
                "operation", "mixed",
                "count", count,
                "latencyMs", latencyMs,
                "traces", results.stream().map(this::toTraceInfo).toList()
        ));
    }

    private Map<String, Object> toTraceInfo(SimulationResult result) {
        return Map.of(
                "traceId", result.traceId(),
                "operation", result.operation(),
                "duration", result.duration(),
                "resultCount", result.resultCount()
        );
    }
}
