package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing the association of a Flavor with a FlavorGroup.
 * This is a junction table entity for the many-to-many relationship
 * between FlavorGroup and Flavor.
 * <p>
 * A Flavor can belong to multiple groups. When a Flavor is deleted,
 * this association is automatically removed (cascade delete from database).
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Entity
@Table(name = "group_flavors",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_group_flavor",
           columnNames = {"group_id", "flavor_id"}
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id"})
public class GroupFlavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The group this flavor belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FlavorGroup group;

    /**
     * The flavor that belongs to the group.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flavor_id", nullable = false)
    private Flavor flavor;

    /**
     * Timestamp when this association was created.
     */
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    /**
     * Username of the user who added this association.
     */
    @Column(name = "added_by")
    private String addedBy;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    /**
     * Creates a new association between a flavor and a group.
     *
     * @param group the group
     * @param flavor the flavor to add
     * @param addedBy who is adding this association
     * @return the new association entity
     */
    public static GroupFlavor create(FlavorGroup group, Flavor flavor, String addedBy) {
        return GroupFlavor.builder()
                .group(group)
                .flavor(flavor)
                .addedBy(addedBy)
                .build();
    }

    @Override
    public String toString() {
        return "GroupFlavor{" +
               "id=" + id +
               ", groupId=" + (group != null ? group.getId() : null) +
               ", flavorId=" + (flavor != null ? flavor.getId() : null) +
               ", addedAt=" + addedAt +
               '}';
    }
}
