package com.example.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Library Management System - Spring Boot 4 Demo Application
 *
 * Demonstrates:
 * - Spring Boot 4.0.0
 * - Spring Modulith 2.0 (modular monolith architecture)
 * - Spring Framework 7 API Versioning
 * - springdoc-openapi 3.0 (Swagger UI)
 * - Custom Actuators
 * - Event-driven module communication
 */
@SpringBootApplication
@Modulith(
    systemName = "Library Management System",
    sharedModules = "shared"
)
@EnableAsync
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
