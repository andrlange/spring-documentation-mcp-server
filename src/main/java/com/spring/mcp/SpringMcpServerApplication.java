package com.spring.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring MCP Server Application - Main Entry Point
 * <p>
 * A Spring Boot application that serves as an MCP (Model Context Protocol) Server
 * providing comprehensive access to Spring ecosystem documentation through
 * Server-Sent Events (SSE).
 *
 * @author Spring MCP Server Team
 * @version 1.6.2
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class SpringMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMcpServerApplication.class, args);
    }
}
