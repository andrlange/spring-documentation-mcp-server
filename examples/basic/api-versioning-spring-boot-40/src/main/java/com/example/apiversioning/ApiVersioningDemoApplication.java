package com.example.apiversioning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Versioning Demo Application
 *
 * Demonstrates Spring Boot 4 / Spring Framework 7's first-class API versioning
 * support using header-based versioning.
 */
@SpringBootApplication
public class ApiVersioningDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiVersioningDemoApplication.class, args);
    }
}
