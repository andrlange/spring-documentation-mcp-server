package com.example.ratelimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Rate Limiter Demo Application
 *
 * Demonstrates Resilience4j Rate Limiting with Spring Boot 4.
 * Configured to allow 8 requests per second.
 */
@SpringBootApplication
public class RateLimiterDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RateLimiterDemoApplication.class, args);
    }
}
