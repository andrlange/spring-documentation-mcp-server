package com.spring.mcp.model.dto.crawler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO holding version information crawled from Spring project pages.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionData {

    /**
     * Version number (e.g., "3.5.8", "1.1.0")
     */
    private String version;

    /**
     * Version status: CURRENT, GA, PRE, SNAPSHOT
     */
    private String status;

    /**
     * Reference documentation URL
     */
    private String referenceDocUrl;

    /**
     * API documentation URL (Javadoc)
     */
    private String apiDocUrl;

    /**
     * Branch/version range (e.g., "3.5.x") from support tab
     */
    private String branch;

    /**
     * Initial release date from support tab
     */
    private LocalDate initialRelease;

    /**
     * OSS support end date from support tab
     */
    private LocalDate ossSupportEnd;

    /**
     * Enterprise support end date from support tab
     */
    private LocalDate enterpriseSupportEnd;
}
