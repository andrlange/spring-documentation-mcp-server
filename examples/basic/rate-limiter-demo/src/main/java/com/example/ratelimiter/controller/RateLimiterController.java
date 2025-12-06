package com.example.ratelimiter.controller;

import com.example.ratelimiter.model.ApiResponse;
import com.example.ratelimiter.service.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller demonstrating rate limiting.
 * Uses a token bucket algorithm: 8 requests per second allowed, rest REJECTED.
 */
@Controller
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final AtomicLong successCounter = new AtomicLong(0);
    private final AtomicLong rejectedCounter = new AtomicLong(0);

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Home page with test UI
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * Rate-limited API endpoint.
     * 8 requests per second allowed - excess requests are REJECTED with HTTP 429.
     */
    @PostMapping("/api/request")
    @ResponseBody
    public ResponseEntity<ApiResponse> makeRequest() {
        long requestId = requestCounter.incrementAndGet();

        // Try to acquire a permit from the rate limiter
        boolean allowed = rateLimiterService.tryAcquire();

        if (allowed) {
            successCounter.incrementAndGet();
            return ResponseEntity.ok(new ApiResponse(
                requestId,
                true,
                "Request successful",
                Instant.now().toString()
            ));
        } else {
            rejectedCounter.incrementAndGet();
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiResponse(
                    requestId,
                    false,
                    "Rate limit exceeded! Max 8 requests per second.",
                    Instant.now().toString()
                ));
        }
    }

    /**
     * Get current statistics
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(new StatsResponse(
            requestCounter.get(),
            successCounter.get(),
            rejectedCounter.get(),
            rateLimiterService.getAvailableTokens()
        ));
    }

    /**
     * Reset statistics
     */
    @PostMapping("/api/reset")
    @ResponseBody
    public ResponseEntity<String> resetStats() {
        requestCounter.set(0);
        successCounter.set(0);
        rejectedCounter.set(0);
        return ResponseEntity.ok("Stats reset");
    }

    /**
     * Statistics response record
     */
    public record StatsResponse(
        long totalRequests,
        long successfulRequests,
        long rejectedRequests,
        int availableTokens
    ) {}
}
