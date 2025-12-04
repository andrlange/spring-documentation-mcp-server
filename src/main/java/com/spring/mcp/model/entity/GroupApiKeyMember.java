package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing the membership of an API Key in a FlavorGroup.
 * This is a junction table entity for the many-to-many relationship
 * between FlavorGroup and ApiKey.
 * <p>
 * When an API Key is deleted, this membership is automatically removed
 * (cascade delete from database).
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Entity
@Table(name = "group_apikey_members",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_group_apikey_member",
           columnNames = {"group_id", "api_key_id"}
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id"})
public class GroupApiKeyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The group this membership belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FlavorGroup group;

    /**
     * The API key that is a member of the group.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    /**
     * Timestamp when this membership was created.
     */
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    /**
     * Username of the user who added this membership.
     */
    @Column(name = "added_by")
    private String addedBy;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    /**
     * Creates a new membership for an API key in a group.
     *
     * @param group the group
     * @param apiKey the API key to add
     * @param addedBy who is adding this membership
     * @return the new membership entity
     */
    public static GroupApiKeyMember create(FlavorGroup group, ApiKey apiKey, String addedBy) {
        return GroupApiKeyMember.builder()
                .group(group)
                .apiKey(apiKey)
                .addedBy(addedBy)
                .build();
    }

    @Override
    public String toString() {
        return "GroupApiKeyMember{" +
               "id=" + id +
               ", groupId=" + (group != null ? group.getId() : null) +
               ", apiKeyId=" + (apiKey != null ? apiKey.getId() : null) +
               ", addedAt=" + addedAt +
               '}';
    }
}
