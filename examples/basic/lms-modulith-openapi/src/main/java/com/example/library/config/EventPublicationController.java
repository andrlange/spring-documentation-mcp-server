package com.example.library.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for monitoring and managing Spring Modulith Event Publications.
 *
 * <p>Provides endpoints to:
 * <ul>
 *   <li>View event publication statistics</li>
 *   <li>View incomplete (pending) event publications</li>
 *   <li>View completed event publications</li>
 *   <li>List externalized event topics</li>
 * </ul>
 *
 * <p>Note: The event_publication table stores all events before processing.
 * Completed events have a non-null completion_date.
 * Spring Modulith automatically retries incomplete publications on restart.
 */
@RestController
@RequestMapping("/api/admin/events")
@Tag(name = "Event Publications", description = "Monitor and manage Spring Modulith event publications")
public class EventPublicationController {

    private final JdbcTemplate jdbcTemplate;

    public EventPublicationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get event publication statistics",
               description = "Returns counts of completed and incomplete event publications")
    public Map<String, Object> getStatistics() {
        Long totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_publication", Long.class);

        Long completedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NOT NULL", Long.class);

        Long incompleteCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL", Long.class);

        return Map.of(
            "total", totalCount != null ? totalCount : 0,
            "completed", completedCount != null ? completedCount : 0,
            "incomplete", incompleteCount != null ? incompleteCount : 0,
            "timestamp", Instant.now()
        );
    }

    @GetMapping("/incomplete")
    @Operation(summary = "List incomplete event publications",
               description = "Returns all pending event publications that haven't been processed yet")
    public List<Map<String, Object>> getIncompletePublications() {
        return jdbcTemplate.queryForList(
            """
            SELECT id, event_type, listener_id, publication_date
            FROM event_publication
            WHERE completion_date IS NULL
            ORDER BY publication_date DESC
            LIMIT 100
            """
        );
    }

    @GetMapping("/completed")
    @Operation(summary = "List recent completed event publications",
               description = "Returns the most recent completed event publications")
    public List<Map<String, Object>> getCompletedPublications(
            @RequestParam(defaultValue = "50") int limit) {
        return jdbcTemplate.queryForList(
            """
            SELECT id, event_type, listener_id, publication_date, completion_date
            FROM event_publication
            WHERE completion_date IS NOT NULL
            ORDER BY completion_date DESC
            LIMIT ?
            """,
            Math.min(limit, 100)
        );
    }

    @GetMapping("/by-type/{eventType}")
    @Operation(summary = "List event publications by type",
               description = "Returns event publications filtered by event type")
    public List<Map<String, Object>> getPublicationsByType(
            @PathVariable String eventType,
            @RequestParam(defaultValue = "50") int limit) {
        return jdbcTemplate.queryForList(
            """
            SELECT id, event_type, listener_id, publication_date, completion_date
            FROM event_publication
            WHERE event_type LIKE ?
            ORDER BY publication_date DESC
            LIMIT ?
            """,
            "%" + eventType + "%",
            Math.min(limit, 100)
        );
    }

    @GetMapping("/status")
    @Operation(summary = "Get event publication system status",
               description = "Returns information about the event publication system")
    public Map<String, Object> getStatus() {
        return Map.of(
            "status", "ACTIVE",
            "description", "Spring Modulith Event Publication Registry is active",
            "features", Map.of(
                "persistentEvents", true,
                "atLeastOnceDelivery", true,
                "automaticRetryOnRestart", true,
                "idempotencyChecks", true
            ),
            "actuatorEndpoint", "/actuator/eventpublications",
            "timestamp", Instant.now()
        );
    }

    @GetMapping("/topics")
    @Operation(summary = "List externalized event topics",
               description = "Returns the list of event topics for external integration")
    public Map<String, Object> getExternalizedTopics() {
        return Map.of(
            "description", "Topics available for external consumers (Kafka/RabbitMQ)",
            "topics", Map.of(
                "loans", new String[]{
                    "library.loans.book-loaned",
                    "library.loans.book-returned",
                    "library.loans.loan-overdue"
                },
                "members", new String[]{
                    "library.members.member-registered",
                    "library.members.status-changed"
                },
                "catalog", new String[]{
                    "catalog.book.added",
                    "catalog.book.availability"
                }
            ),
            "note", "Add spring-modulith-events-kafka or spring-modulith-events-amqp to enable",
            "timestamp", Instant.now()
        );
    }

    @DeleteMapping("/completed/older-than/{days}")
    @Operation(summary = "Delete old completed publications",
               description = "Removes completed event publications older than specified days")
    public Map<String, Object> deleteOldCompletedPublications(@PathVariable int days) {
        int deleted = jdbcTemplate.update(
            """
            DELETE FROM event_publication
            WHERE completion_date IS NOT NULL
            AND completion_date < NOW() - INTERVAL '? days'
            """.replace("?", String.valueOf(Math.max(1, days)))
        );

        return Map.of(
            "deleted", deleted,
            "olderThanDays", days,
            "timestamp", Instant.now()
        );
    }
}
