package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.ConnectionEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing MCP connection events for real-time monitoring.
 * Tracks connection lifecycle events with detailed metadata.
 *
 * @author Spring MCP Server
 * @version 1.4.4
 * @since 2025-12-16
 */
@Entity
@Table(name = "mcp_connection_events",
        indexes = {
                @Index(name = "idx_conn_events_session", columnList = "session_id"),
                @Index(name = "idx_conn_events_created", columnList = "created_at DESC"),
                @Index(name = "idx_conn_events_type", columnList = "event_type")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class McpConnectionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ConnectionEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "client_info", columnDefinition = "jsonb")
    private Map<String, Object> clientInfo;

    @Column(name = "protocol_version", length = 50)
    private String protocolVersion;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this is an error event.
     */
    public boolean isError() {
        return eventType == ConnectionEventType.ERROR;
    }

    /**
     * Check if this is a connection establishment event.
     */
    public boolean isConnectionEstablished() {
        return eventType == ConnectionEventType.CONNECTED ||
               eventType == ConnectionEventType.INITIALIZED;
    }

    /**
     * Check if this is a connection termination event.
     */
    public boolean isConnectionTerminated() {
        return eventType == ConnectionEventType.DISCONNECTED ||
               eventType == ConnectionEventType.TIMEOUT ||
               eventType == ConnectionEventType.ERROR;
    }

    /**
     * Get client name from client info if available.
     */
    public String getClientName() {
        if (clientInfo == null) {
            return null;
        }
        Object name = clientInfo.get("name");
        return name != null ? name.toString() : null;
    }

    /**
     * Get client version from client info if available.
     */
    public String getClientVersion() {
        if (clientInfo == null) {
            return null;
        }
        Object version = clientInfo.get("version");
        return version != null ? version.toString() : null;
    }

    /**
     * Create a connection event.
     */
    public static McpConnectionEvent create(
            String sessionId,
            ConnectionEventType eventType,
            Map<String, Object> clientInfo,
            String protocolVersion) {
        return McpConnectionEvent.builder()
                .sessionId(sessionId)
                .eventType(eventType)
                .clientInfo(clientInfo)
                .protocolVersion(protocolVersion)
                .build();
    }

    /**
     * Create an error event.
     */
    public static McpConnectionEvent createError(
            String sessionId,
            String errorMessage,
            Map<String, Object> clientInfo) {
        return McpConnectionEvent.builder()
                .sessionId(sessionId)
                .eventType(ConnectionEventType.ERROR)
                .errorMessage(errorMessage)
                .clientInfo(clientInfo)
                .build();
    }
}
