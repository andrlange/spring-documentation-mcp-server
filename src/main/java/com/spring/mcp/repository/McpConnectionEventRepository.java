package com.spring.mcp.repository;

import com.spring.mcp.model.entity.McpConnectionEvent;
import com.spring.mcp.model.enums.ConnectionEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for McpConnectionEvent entities.
 * Provides queries for accessing connection event data.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Repository
public interface McpConnectionEventRepository extends JpaRepository<McpConnectionEvent, Long> {

    /**
     * Find events by session ID.
     */
    List<McpConnectionEvent> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Find events by type since a given time.
     */
    List<McpConnectionEvent> findByEventTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            ConnectionEventType eventType,
            LocalDateTime since);

    /**
     * Find recent events.
     */
    List<McpConnectionEvent> findTop50ByOrderByCreatedAtDesc();

    /**
     * Find recent events by type.
     */
    List<McpConnectionEvent> findTop20ByEventTypeOrderByCreatedAtDesc(ConnectionEventType eventType);

    /**
     * Count distinct active sessions (sessions with connection activity in window).
     * For MCP HTTP+SSE transport, connections are short-lived, so we count sessions
     * that have had any connection event (CONNECTED) in the time window.
     * This represents "recently active" sessions rather than "currently connected" sessions.
     */
    @Query("SELECT COUNT(DISTINCT e.sessionId) FROM McpConnectionEvent e " +
           "WHERE e.eventType = 'CONNECTED' AND e.createdAt >= :since")
    long countActiveConnections(@Param("since") LocalDateTime since);

    /**
     * Count events by type in a time range.
     */
    @Query("SELECT COUNT(e) FROM McpConnectionEvent e WHERE e.eventType = :type AND e.createdAt >= :since")
    long countByEventTypeSince(
            @Param("type") ConnectionEventType type,
            @Param("since") LocalDateTime since);

    /**
     * Count errors in a time range.
     */
    @Query("SELECT COUNT(e) FROM McpConnectionEvent e WHERE e.eventType = 'ERROR' AND e.createdAt >= :since")
    long countErrorsSince(@Param("since") LocalDateTime since);

    /**
     * Get event counts grouped by type.
     */
    @Query("SELECT e.eventType, COUNT(e) FROM McpConnectionEvent e " +
           "WHERE e.createdAt >= :since GROUP BY e.eventType")
    List<Object[]> countByEventTypeGrouped(@Param("since") LocalDateTime since);

    /**
     * Find last event for a session.
     */
    @Query("SELECT e FROM McpConnectionEvent e WHERE e.sessionId = :sessionId ORDER BY e.createdAt DESC LIMIT 1")
    McpConnectionEvent findLastEventForSession(@Param("sessionId") String sessionId);

    /**
     * Find distinct session IDs with connection events since a given time.
     */
    @Query("SELECT DISTINCT e.sessionId FROM McpConnectionEvent e WHERE e.createdAt >= :since")
    List<String> findDistinctSessionsSince(@Param("since") LocalDateTime since);

    /**
     * Delete events older than cutoff date.
     */
    @Modifying
    @Query("DELETE FROM McpConnectionEvent e WHERE e.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find events in time range.
     */
    List<McpConnectionEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end);

    /**
     * Get client usage statistics grouped by user agent (client name).
     * Returns user agent string and connection count.
     */
    @Query(value = "SELECT client_info->>'userAgent' as user_agent, COUNT(*) as connection_count " +
                   "FROM mcp_connection_events " +
                   "WHERE event_type = 'CONNECTED' " +
                   "AND created_at >= :since " +
                   "AND client_info->>'userAgent' IS NOT NULL " +
                   "GROUP BY client_info->>'userAgent' " +
                   "ORDER BY connection_count DESC",
           nativeQuery = true)
    List<Object[]> getClientUsageStats(@Param("since") LocalDateTime since);

    /**
     * Get top N clients by connection count.
     */
    @Query(value = "SELECT client_info->>'userAgent' as user_agent, COUNT(*) as connection_count " +
                   "FROM mcp_connection_events " +
                   "WHERE event_type = 'CONNECTED' " +
                   "AND created_at >= :since " +
                   "AND client_info->>'userAgent' IS NOT NULL " +
                   "GROUP BY client_info->>'userAgent' " +
                   "ORDER BY connection_count DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<Object[]> getTopClientsByConnections(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit);
}
