package com.spring.mcp.controller;

import com.spring.mcp.service.bootstrap.DocumentationBootstrapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing documentation bootstrap operations.
 * <p>
 * This controller provides endpoints to:
 * <ul>
 *   <li>Trigger manual bootstrap of all projects</li>
 *   <li>Bootstrap individual projects</li>
 *   <li>Check bootstrap status and progress</li>
 *   <li>View bootstrap statistics</li>
 * </ul>
 * <p>
 * All bootstrap endpoints require ADMIN role for security.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 * @since Phase 3
 */
@RestController
@RequestMapping("/api/bootstrap")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bootstrap", description = "Documentation bootstrap management endpoints")
public class BootstrapController {

    private final DocumentationBootstrapService bootstrapService;

    /**
     * Gets the current bootstrap status.
     * <p>
     * Returns detailed information about:
     * <ul>
     *   <li>Bootstrap enabled/disabled state</li>
     *   <li>Current progress (in progress, completed)</li>
     *   <li>Statistics (projects, versions, links created)</li>
     *   <li>Individual project status</li>
     *   <li>Any errors encountered</li>
     * </ul>
     *
     * @return ResponseEntity containing bootstrap status map
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get bootstrap status",
        description = "Returns the current status of the documentation bootstrap process"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved bootstrap status")
    })
    public ResponseEntity<Map<String, Object>> getBootstrapStatus() {
        log.info("Getting bootstrap status");
        Map<String, Object> status = bootstrapService.getBootstrapStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Triggers a full bootstrap of all configured Spring projects.
     * <p>
     * This endpoint:
     * <ol>
     *   <li>Creates documentation types if they don't exist</li>
     *   <li>Creates/updates all configured projects</li>
     *   <li>Detects and stores project versions</li>
     *   <li>Creates documentation links for each version</li>
     * </ol>
     * <p>
     * The operation is idempotent and will skip existing data.
     * This is a long-running operation that runs asynchronously.
     *
     * @return ResponseEntity with operation status
     */
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Bootstrap all projects",
        description = "Triggers a full bootstrap of all configured Spring projects"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Bootstrap started successfully"),
        @ApiResponse(responseCode = "409", description = "Bootstrap already in progress"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> bootstrapAll() {
        log.info("Triggering full bootstrap via API");

        if (!bootstrapService.isBootstrapComplete() &&
            bootstrapService.getBootstrapStatus().get("inProgress").equals(true)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "status", "error",
                    "message", "Bootstrap is already in progress"
                ));
        }

        // Run bootstrap asynchronously on a virtual thread (Spring-managed)
        bootstrapService.bootstrapAllProjectsAsync();

        return ResponseEntity.accepted()
            .body(Map.of(
                "status", "accepted",
                "message", "Bootstrap started successfully on virtual thread. Check /api/bootstrap/status for progress."
            ));
    }

    /**
     * Bootstraps a single Spring project by slug.
     * <p>
     * Supported project slugs:
     * <ul>
     *   <li>spring-boot</li>
     *   <li>spring-framework</li>
     *   <li>spring-data</li>
     *   <li>spring-security</li>
     *   <li>spring-cloud</li>
     * </ul>
     *
     * @param projectSlug the project slug to bootstrap
     * @return ResponseEntity with operation status
     */
    @PostMapping("/project/{projectSlug}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Bootstrap single project",
        description = "Bootstraps a single Spring project by slug"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project bootstrapped successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid project slug"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "500", description = "Bootstrap failed")
    })
    public ResponseEntity<Map<String, Object>> bootstrapProject(
        @PathVariable String projectSlug) {

        log.info("Triggering bootstrap for project: {}", projectSlug);

        try {
            bootstrapService.bootstrapProject(projectSlug);

            Map<String, Object> status = bootstrapService.getBootstrapStatus();
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> projectStatus =
                (Map<String, Map<String, Object>>) status.get("projectStatus");

            Map<String, Object> result = projectStatus.get(projectSlug);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Project bootstrapped successfully",
                "projectSlug", projectSlug,
                "details", result != null ? result : Map.of()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Invalid project slug: {}", projectSlug, e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "status", "error",
                    "message", "Invalid project slug: " + projectSlug,
                    "error", e.getMessage()
                ));

        } catch (Exception e) {
            log.error("Error bootstrapping project: {}", projectSlug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "Failed to bootstrap project: " + projectSlug,
                    "error", e.getMessage()
                ));
        }
    }

    /**
     * Checks if the bootstrap process is complete.
     *
     * @return ResponseEntity with completion status
     */
    @GetMapping("/complete")
    @Operation(
        summary = "Check bootstrap completion",
        description = "Returns whether the bootstrap process has completed"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked completion status")
    })
    public ResponseEntity<Map<String, Object>> isBootstrapComplete() {
        boolean complete = bootstrapService.isBootstrapComplete();
        return ResponseEntity.ok(Map.of(
            "complete", complete,
            "message", complete ? "Bootstrap is complete" : "Bootstrap is not complete or in progress"
        ));
    }
}
