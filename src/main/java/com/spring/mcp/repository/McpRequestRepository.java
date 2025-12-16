package com.spring.mcp.repository;

import com.spring.mcp.model.entity.McpConnection;
import com.spring.mcp.model.entity.McpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for McpRequest entities
 */
@Repository
public interface McpRequestRepository extends JpaRepository<McpRequest, Long> {

    /**
     * Find requests by connection
     */
    List<McpRequest> findByConnection(McpConnection connection);

    /**
     * Find requests by tool name
     */
    List<McpRequest> findByToolName(String toolName);

    /**
     * Find requests by response status
     */
    List<McpRequest> findByResponseStatus(String responseStatus);

    /**
     * Find requests within a time range
     */
    List<McpRequest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count requests by tool name
     */
    long countByToolName(String toolName);

    /**
     * Get most used tools
     */
    @Query("SELECT r.toolName, COUNT(r) as count FROM McpRequest r GROUP BY r.toolName ORDER BY count DESC")
    List<Object[]> getMostUsedTools();

    /**
     * Get average execution time by tool
     */
    @Query("SELECT r.toolName, AVG(r.executionTimeMs) FROM McpRequest r GROUP BY r.toolName")
    List<Object[]> getAverageExecutionTimeByTool();

    /**
     * Find slow requests (execution time > threshold)
     */
    @Query("SELECT r FROM McpRequest r WHERE r.executionTimeMs > :threshold ORDER BY r.executionTimeMs DESC")
    List<McpRequest> findSlowRequests(@Param("threshold") int thresholdMs);

    /**
     * Find most recent requests
     */
    List<McpRequest> findTop20ByOrderByCreatedAtDesc();

    /**
     * Count requests created after a given time
     */
    long countByCreatedAtAfter(LocalDateTime since);

    /**
     * Count requests with non-success status created after a given time
     */
    @Query("SELECT COUNT(r) FROM McpRequest r WHERE r.responseStatus <> :status AND r.createdAt > :since")
    long countByResponseStatusNotAndCreatedAtAfter(@Param("status") String status, @Param("since") LocalDateTime since);

    /**
     * Get distinct tool names from requests
     */
    @Query("SELECT DISTINCT r.toolName FROM McpRequest r WHERE r.toolName IS NOT NULL ORDER BY r.toolName")
    List<String> findDistinctToolNames();
}
