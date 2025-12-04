package com.spring.mcp.repository;

import com.spring.mcp.model.entity.FlavorGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FlavorGroup entities.
 * Provides methods for querying groups with visibility filtering.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Repository
public interface FlavorGroupRepository extends JpaRepository<FlavorGroup, Long> {

    /**
     * Find a group by its unique name.
     *
     * @param uniqueName the unique name of the group
     * @return the group if found
     */
    Optional<FlavorGroup> findByUniqueName(String uniqueName);

    /**
     * Find a group by its unique name (only active groups).
     *
     * @param uniqueName the unique name of the group
     * @return the active group if found
     */
    Optional<FlavorGroup> findByUniqueNameAndIsActiveTrue(String uniqueName);

    /**
     * Find all active groups.
     *
     * @return list of active groups
     */
    List<FlavorGroup> findByIsActiveTrueOrderByDisplayNameAsc();

    /**
     * Find all groups ordered by display name.
     *
     * @return list of all groups
     */
    List<FlavorGroup> findAllByOrderByDisplayNameAsc();

    /**
     * Check if a group with the given unique name exists.
     *
     * @param uniqueName the unique name to check
     * @return true if exists
     */
    boolean existsByUniqueName(String uniqueName);

    /**
     * Find all PUBLIC active groups (no members).
     * A group is public if it has no user members AND no API key members.
     *
     * @return list of public active groups
     */
    @Query("""
        SELECT fg FROM FlavorGroup fg
        WHERE fg.isActive = true
        AND NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
        AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg)
        ORDER BY fg.displayName ASC
        """)
    List<FlavorGroup> findAllPublicActiveGroups();

    /**
     * Find all PRIVATE active groups where a specific API key is a member.
     *
     * @param apiKeyId the API key ID
     * @return list of private groups where the API key is a member
     */
    @Query("""
        SELECT fg FROM FlavorGroup fg
        JOIN GroupApiKeyMember gam ON gam.group = fg
        WHERE fg.isActive = true
        AND gam.apiKey.id = :apiKeyId
        ORDER BY fg.displayName ASC
        """)
    List<FlavorGroup> findPrivateGroupsForApiKey(@Param("apiKeyId") Long apiKeyId);

    /**
     * Find all PRIVATE active groups where a specific user is a member.
     *
     * @param userId the user ID
     * @return list of private groups where the user is a member
     */
    @Query("""
        SELECT fg FROM FlavorGroup fg
        JOIN GroupUserMember gum ON gum.group = fg
        WHERE fg.isActive = true
        AND gum.user.id = :userId
        ORDER BY fg.displayName ASC
        """)
    List<FlavorGroup> findPrivateGroupsForUser(@Param("userId") Long userId);

    /**
     * Find all accessible active groups for an API key.
     * Returns: public groups + private groups where API key is member.
     *
     * @param apiKeyId the API key ID
     * @return list of accessible groups
     */
    @Query("""
        SELECT DISTINCT fg FROM FlavorGroup fg
        WHERE fg.isActive = true
        AND (
            (NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
             AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg))
            OR
            EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg AND gam.apiKey.id = :apiKeyId)
        )
        ORDER BY fg.displayName ASC
        """)
    List<FlavorGroup> findAccessibleGroupsForApiKey(@Param("apiKeyId") Long apiKeyId);

    /**
     * Find all accessible active groups for a user.
     * Returns: public groups + private groups where user is member.
     *
     * @param userId the user ID
     * @return list of accessible groups
     */
    @Query("""
        SELECT DISTINCT fg FROM FlavorGroup fg
        WHERE fg.isActive = true
        AND (
            (NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
             AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg))
            OR
            EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg AND gum.user.id = :userId)
        )
        ORDER BY fg.displayName ASC
        """)
    List<FlavorGroup> findAccessibleGroupsForUser(@Param("userId") Long userId);

    /**
     * Check if an API key can access a specific group.
     *
     * @param groupId the group ID
     * @param apiKeyId the API key ID
     * @return true if the API key can access the group
     */
    @Query("""
        SELECT CASE WHEN COUNT(fg) > 0 THEN true ELSE false END
        FROM FlavorGroup fg
        WHERE fg.id = :groupId
        AND fg.isActive = true
        AND (
            (NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
             AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg))
            OR
            EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg AND gam.apiKey.id = :apiKeyId)
        )
        """)
    boolean canApiKeyAccessGroup(@Param("groupId") Long groupId, @Param("apiKeyId") Long apiKeyId);

    /**
     * Check if a group is public (has no members).
     *
     * @param groupId the group ID
     * @return true if the group is public
     */
    @Query("""
        SELECT CASE WHEN COUNT(fg) > 0 THEN true ELSE false END
        FROM FlavorGroup fg
        WHERE fg.id = :groupId
        AND NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
        AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg)
        """)
    boolean isGroupPublic(@Param("groupId") Long groupId);

    /**
     * Count groups by activity status.
     *
     * @param isActive the activity status
     * @return count of groups
     */
    long countByIsActive(Boolean isActive);

    /**
     * Count public groups (active and no members).
     *
     * @return count of public groups
     */
    @Query("""
        SELECT COUNT(fg) FROM FlavorGroup fg
        WHERE fg.isActive = true
        AND NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
        AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg)
        """)
    long countPublicGroups();

    /**
     * Count private groups (active and has members).
     *
     * @return count of private groups
     */
    @Query("""
        SELECT COUNT(fg) FROM FlavorGroup fg
        WHERE fg.isActive = true
        AND (EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
             OR EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg))
        """)
    long countPrivateGroups();
}
