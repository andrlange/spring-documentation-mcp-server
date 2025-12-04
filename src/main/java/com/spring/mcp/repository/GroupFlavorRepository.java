package com.spring.mcp.repository;

import com.spring.mcp.model.entity.Flavor;
import com.spring.mcp.model.entity.GroupFlavor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GroupFlavor entities.
 * Manages flavor associations with flavor groups.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Repository
public interface GroupFlavorRepository extends JpaRepository<GroupFlavor, Long> {

    /**
     * Find all flavor associations for a specific group.
     *
     * @param groupId the group ID
     * @return list of flavor associations
     */
    List<GroupFlavor> findByGroupId(Long groupId);

    /**
     * Find all group associations for a specific flavor.
     *
     * @param flavorId the flavor ID
     * @return list of group associations
     */
    List<GroupFlavor> findByFlavorId(Long flavorId);

    /**
     * Find a specific association.
     *
     * @param groupId the group ID
     * @param flavorId the flavor ID
     * @return the association if exists
     */
    Optional<GroupFlavor> findByGroupIdAndFlavorId(Long groupId, Long flavorId);

    /**
     * Check if a flavor is in a group.
     *
     * @param groupId the group ID
     * @param flavorId the flavor ID
     * @return true if the flavor is in the group
     */
    boolean existsByGroupIdAndFlavorId(Long groupId, Long flavorId);

    /**
     * Check if a flavor is in ANY group.
     *
     * @param flavorId the flavor ID
     * @return true if the flavor is in at least one group
     */
    boolean existsByFlavorId(Long flavorId);

    /**
     * Count flavors in a group.
     *
     * @param groupId the group ID
     * @return number of flavors
     */
    long countByGroupId(Long groupId);

    /**
     * Count groups a flavor belongs to.
     *
     * @param flavorId the flavor ID
     * @return number of groups
     */
    long countByFlavorId(Long flavorId);

    /**
     * Delete a specific association.
     *
     * @param groupId the group ID
     * @param flavorId the flavor ID
     */
    @Modifying
    @Query("DELETE FROM GroupFlavor gf WHERE gf.group.id = :groupId AND gf.flavor.id = :flavorId")
    void deleteByGroupIdAndFlavorId(@Param("groupId") Long groupId, @Param("flavorId") Long flavorId);

    /**
     * Delete all associations for a group.
     *
     * @param groupId the group ID
     */
    @Modifying
    void deleteByGroupId(Long groupId);

    /**
     * Delete all associations for a flavor.
     *
     * @param flavorId the flavor ID
     */
    @Modifying
    void deleteByFlavorId(Long flavorId);

    /**
     * Find all flavors in a specific active group.
     *
     * @param groupId the group ID
     * @return list of flavors
     */
    @Query("""
        SELECT gf.flavor FROM GroupFlavor gf
        WHERE gf.group.id = :groupId
        AND gf.group.isActive = true
        AND gf.flavor.isActive = true
        ORDER BY gf.flavor.displayName ASC
        """)
    List<Flavor> findActiveFlavorsInGroup(@Param("groupId") Long groupId);

    /**
     * Find all flavors in a specific group by unique name.
     *
     * @param groupUniqueName the group's unique name
     * @return list of flavors
     */
    @Query("""
        SELECT gf.flavor FROM GroupFlavor gf
        WHERE gf.group.uniqueName = :groupUniqueName
        AND gf.group.isActive = true
        AND gf.flavor.isActive = true
        ORDER BY gf.flavor.displayName ASC
        """)
    List<Flavor> findActiveFlavorsInGroupByUniqueName(@Param("groupUniqueName") String groupUniqueName);

    /**
     * Find all UNASSIGNED flavors (not in any group).
     *
     * @return list of unassigned active flavors
     */
    @Query("""
        SELECT f FROM Flavor f
        WHERE f.isActive = true
        AND NOT EXISTS (SELECT 1 FROM GroupFlavor gf WHERE gf.flavor = f)
        ORDER BY f.displayName ASC
        """)
    List<Flavor> findUnassignedFlavors();

    /**
     * Find all accessible flavor IDs for an API key.
     * Returns: unassigned + public group flavors + member private group flavors.
     *
     * @param apiKeyId the API key ID
     * @return list of accessible flavor IDs
     */
    @Query("""
        SELECT DISTINCT f.id FROM Flavor f
        WHERE f.isActive = true
        AND (
            NOT EXISTS (SELECT 1 FROM GroupFlavor gf WHERE gf.flavor = f)
            OR
            EXISTS (
                SELECT 1 FROM GroupFlavor gf
                JOIN gf.group fg
                WHERE gf.flavor = f
                AND fg.isActive = true
                AND NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
                AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg)
            )
            OR
            EXISTS (
                SELECT 1 FROM GroupFlavor gf
                JOIN gf.group fg
                JOIN GroupApiKeyMember gam ON gam.group = fg
                WHERE gf.flavor = f
                AND fg.isActive = true
                AND gam.apiKey.id = :apiKeyId
            )
        )
        """)
    List<Long> findAccessibleFlavorIdsForApiKey(@Param("apiKeyId") Long apiKeyId);

    /**
     * Find all accessible flavor IDs for a user.
     * Returns: unassigned + public group flavors + member private group flavors.
     *
     * @param userId the user ID
     * @return list of accessible flavor IDs
     */
    @Query("""
        SELECT DISTINCT f.id FROM Flavor f
        WHERE f.isActive = true
        AND (
            NOT EXISTS (SELECT 1 FROM GroupFlavor gf WHERE gf.flavor = f)
            OR
            EXISTS (
                SELECT 1 FROM GroupFlavor gf
                JOIN gf.group fg
                WHERE gf.flavor = f
                AND fg.isActive = true
                AND NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
                AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg)
            )
            OR
            EXISTS (
                SELECT 1 FROM GroupFlavor gf
                JOIN gf.group fg
                JOIN GroupUserMember gum ON gum.group = fg
                WHERE gf.flavor = f
                AND fg.isActive = true
                AND gum.user.id = :userId
            )
        )
        """)
    List<Long> findAccessibleFlavorIdsForUser(@Param("userId") Long userId);

    /**
     * Check if an API key can access a specific flavor.
     *
     * @param flavorId the flavor ID
     * @param apiKeyId the API key ID
     * @return true if accessible
     */
    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM Flavor f
        WHERE f.id = :flavorId
        AND f.isActive = true
        AND (
            NOT EXISTS (SELECT 1 FROM GroupFlavor gf WHERE gf.flavor = f)
            OR
            EXISTS (
                SELECT 1 FROM GroupFlavor gf
                JOIN gf.group fg
                WHERE gf.flavor = f
                AND fg.isActive = true
                AND NOT EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
                AND NOT EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg)
            )
            OR
            EXISTS (
                SELECT 1 FROM GroupFlavor gf
                JOIN gf.group fg
                JOIN GroupApiKeyMember gam ON gam.group = fg
                WHERE gf.flavor = f
                AND fg.isActive = true
                AND gam.apiKey.id = :apiKeyId
            )
        )
        """)
    boolean canApiKeyAccessFlavor(@Param("flavorId") Long flavorId, @Param("apiKeyId") Long apiKeyId);

    /**
     * Find flavors that can be added to a group.
     * Returns: unassigned flavors + flavors in public groups only.
     * Does NOT include flavors already in the target group or in other private groups.
     *
     * @param groupId the group to add flavors to
     * @return list of available flavors
     */
    @Query("""
        SELECT f FROM Flavor f
        WHERE f.isActive = true
        AND NOT EXISTS (
            SELECT 1 FROM GroupFlavor gf WHERE gf.flavor = f AND gf.group.id = :groupId
        )
        AND (
            NOT EXISTS (SELECT 1 FROM GroupFlavor gf WHERE gf.flavor = f)
            OR
            NOT EXISTS (
                SELECT 1 FROM GroupFlavor gf
                JOIN gf.group fg
                WHERE gf.flavor = f
                AND (EXISTS (SELECT 1 FROM GroupUserMember gum WHERE gum.group = fg)
                     OR EXISTS (SELECT 1 FROM GroupApiKeyMember gam WHERE gam.group = fg))
            )
        )
        ORDER BY f.displayName ASC
        """)
    List<Flavor> findFlavorsAvailableForGroup(@Param("groupId") Long groupId);

    /**
     * Find all active group unique names for a flavor.
     *
     * @param flavorId the flavor ID
     * @return list of group unique names
     */
    @Query("""
        SELECT gf.group.uniqueName FROM GroupFlavor gf
        WHERE gf.flavor.id = :flavorId
        AND gf.group.isActive = true
        ORDER BY gf.group.displayName ASC
        """)
    List<String> findActiveGroupUniqueNamesForFlavor(@Param("flavorId") Long flavorId);
}
