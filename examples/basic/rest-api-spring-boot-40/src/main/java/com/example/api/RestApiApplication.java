package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * REST API Example Application
 *
 * Built with Spring Boot 4.0.0 using Spring MCP Server for version guidance.
 *
 * This example demonstrates:
 * - Simple REST API with CRUD operations
 * - In-memory data storage
 * - Java Records for DTOs
 * - Bean Validation
 * - Actuator health endpoints
 *
 * Spring Boot 4.0.0 Key Changes:
 * - Requires Gradle 8.14 or later
 * - Uses Spring Framework 7.0.x
 * - Health classes moved to spring-boot-health module
 */
@SpringBootApplication
public class RestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestApiApplication.class, args);
    }
}
