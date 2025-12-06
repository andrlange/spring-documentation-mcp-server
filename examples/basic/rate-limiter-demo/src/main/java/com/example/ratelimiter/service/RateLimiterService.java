package com.example.ratelimiter.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple token bucket rate limiter.
 * Allows exactly N requests per second, rejects the rest.
 *
 * No external libraries - pure Java implementation.
 */
@Service
public class RateLimiterService {

    private final int limitPerSecond;
    private final AtomicInteger availableTokens;
    private final AtomicLong lastRefillTime;

    public RateLimiterService() {
        this.limitPerSecond = 8;  // 8 requests per second
        this.availableTokens = new AtomicInteger(limitPerSecond);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * Try to acquire a permit.
     * Returns true if allowed, false if rate limit exceeded.
     */
    public synchronized boolean tryAcquire() {
        refillTokens();

        if (availableTokens.get() > 0) {
            availableTokens.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Refill tokens based on elapsed time.
     * Tokens refill at the start of each second.
     */
    private void refillTokens() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();
        long elapsedMs = now - lastRefill;

        // Refill tokens every second
        if (elapsedMs >= 1000) {
            availableTokens.set(limitPerSecond);
            lastRefillTime.set(now);
        }
    }

    /**
     * Get current available tokens (for monitoring)
     */
    public int getAvailableTokens() {
        refillTokens();
        return availableTokens.get();
    }

    /**
     * Get the configured limit
     */
    public int getLimitPerSecond() {
        return limitPerSecond;
    }
}
