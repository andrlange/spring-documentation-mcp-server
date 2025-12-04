package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Flavor Group for team-based authorization.
 * <p>
 * Groups can be:
 * <ul>
 *   <li><b>Public</b> - No members (users or API keys) → visible to everyone</li>
 *   <li><b>Private</b> - Has at least one member → visible only to members</li>
 * </ul>
 * <p>
 * When a group is marked as <b>inactive</b>, the group AND all its flavors
 * are completely hidden from the UI and MCP tools.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Entity
@Table(name = "flavor_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"userMembers", "apiKeyMembers", "groupFlavors"})
@EqualsAndHashCode(of = {"id"})
public class FlavorGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier for the group (immutable after creation, URL-friendly).
     */
    @NotBlank(message = "Group unique name is required")
    @Size(min = 3, max = 255, message = "Group unique name must be between 3 and 255 characters")
    @Column(name = "unique_name", nullable = false, unique = true, updatable = false)
    private String uniqueName;

    /**
     * Human-readable display name for the group.
     */
    @NotBlank(message = "Group display name is required")
    @Size(max = 255, message = "Group display name must not exceed 255 characters")
    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * Optional description of the group's purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this group is active.
     * When inactive, the group AND all its flavors are hidden from UI and MCP.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when the group was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the group was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Username of the user who created this group.
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * Username of the user who last updated this group.
     */
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * User members of this group.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupUserMember> userMembers = new ArrayList<>();

    /**
     * API Key members of this group.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupApiKeyMember> apiKeyMembers = new ArrayList<>();

    /**
     * Flavors belonging to this group.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupFlavor> groupFlavors = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Determines if this group is public (no members).
     * A group is public if it has no user members AND no API key members.
     *
     * @return true if the group is public, false if private
     */
    public boolean isPublic() {
        return (userMembers == null || userMembers.isEmpty()) &&
               (apiKeyMembers == null || apiKeyMembers.isEmpty());
    }

    /**
     * Gets the total member count (users + API keys).
     *
     * @return total number of members
     */
    public int getTotalMemberCount() {
        int userCount = userMembers != null ? userMembers.size() : 0;
        int apiKeyCount = apiKeyMembers != null ? apiKeyMembers.size() : 0;
        return userCount + apiKeyCount;
    }

    /**
     * Gets the flavor count in this group.
     *
     * @return number of flavors in this group
     */
    public int getFlavorCount() {
        return groupFlavors != null ? groupFlavors.size() : 0;
    }

    /**
     * Checks if a specific user is a member of this group.
     *
     * @param userId the user ID to check
     * @return true if the user is a member
     */
    public boolean hasUserMember(Long userId) {
        if (userMembers == null || userId == null) {
            return false;
        }
        return userMembers.stream()
                .anyMatch(m -> m.getUser() != null && userId.equals(m.getUser().getId()));
    }

    /**
     * Checks if a specific API key is a member of this group.
     *
     * @param apiKeyId the API key ID to check
     * @return true if the API key is a member
     */
    public boolean hasApiKeyMember(Long apiKeyId) {
        if (apiKeyMembers == null || apiKeyId == null) {
            return false;
        }
        return apiKeyMembers.stream()
                .anyMatch(m -> m.getApiKey() != null && apiKeyId.equals(m.getApiKey().getId()));
    }
}
