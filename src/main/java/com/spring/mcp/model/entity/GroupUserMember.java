package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing the membership of a User in a FlavorGroup.
 * This is a junction table entity for the many-to-many relationship
 * between FlavorGroup and User.
 * <p>
 * When a User is deleted, this membership is automatically removed
 * (cascade delete from database).
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Entity
@Table(name = "group_user_members",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_group_user_member",
           columnNames = {"group_id", "user_id"}
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id"})
public class GroupUserMember {

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
     * The user who is a member of the group.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
     * Creates a new membership for a user in a group.
     *
     * @param group the group
     * @param user the user to add
     * @param addedBy who is adding this membership
     * @return the new membership entity
     */
    public static GroupUserMember create(FlavorGroup group, User user, String addedBy) {
        return GroupUserMember.builder()
                .group(group)
                .user(user)
                .addedBy(addedBy)
                .build();
    }

    @Override
    public String toString() {
        return "GroupUserMember{" +
               "id=" + id +
               ", groupId=" + (group != null ? group.getId() : null) +
               ", userId=" + (user != null ? user.getId() : null) +
               ", addedAt=" + addedAt +
               '}';
    }
}
