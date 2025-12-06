package com.spring.mcp.service.initializr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a dependency category from Spring Initializr.
 *
 * <p>Categories group related dependencies together (e.g., "Web", "SQL", "NoSQL",
 * "Security", "Cloud"). Each category has a name and contains a list of available
 * dependencies within that category.</p>
 *
 * <p>Example categories from start.spring.io:</p>
 * <ul>
 *   <li>Developer Tools - DevTools, Lombok, Spring Configuration Processor</li>
 *   <li>Web - Spring Web, Spring Reactive Web, Spring GraphQL</li>
 *   <li>SQL - Spring Data JPA, H2, MySQL, PostgreSQL</li>
 *   <li>NoSQL - Spring Data MongoDB, Spring Data Redis</li>
 *   <li>Security - Spring Security, OAuth2</li>
 *   <li>Cloud - Config Client, Discovery, Gateway</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DependencyCategory {

    /**
     * Display name of the category.
     * Examples: "Developer Tools", "Web", "SQL", "NoSQL", "Security"
     */
    private String name;

    /**
     * List of dependencies within this category.
     * Note: The API returns this as "values" but we use "content" internally.
     */
    @JsonProperty("values")
    private List<DependencyInfo> content;
}
