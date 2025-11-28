package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * REST API Example Application
 *
 * Built with Spring Boot 3.5.8 using Spring MCP Server for version guidance.
 *
 * This example demonstrates:
 * - Simple REST API with CRUD operations
 * - In-memory data storage
 * - Java Records for DTOs
 * - Bean Validation
 * - Actuator health endpoints
 */
@SpringBootApplication
public class RestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestApiApplication.class, args);
    }
}
