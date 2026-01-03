package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.McpToolGroup;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an MCP Tool configuration.
 * Allows dynamic control over tool visibility and descriptions exposed to LLMs.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@Entity
@Table(name = "mcp_tools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"description", "originalDescription"})
@EqualsAndHashCode(of = {"id"})
public class McpTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_name", nullable = false, unique = true, length = 100)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_group", nullable = false, length = 50)
    private McpToolGroup toolGroup;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_description", nullable = false, columnDefinition = "TEXT")
    private String originalDescription;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the description has been modified from the original.
     *
     * @return true if description differs from original
     */
    public boolean isDescriptionModified() {
        if (description == null && originalDescription == null) {
            return false;
        }
        if (description == null || originalDescription == null) {
            return true;
        }
        return !description.equals(originalDescription);
    }

    /**
     * Reset the description to the original value.
     */
    public void resetDescription() {
        this.description = this.originalDescription;
    }
}
