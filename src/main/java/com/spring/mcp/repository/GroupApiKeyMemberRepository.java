package com.spring.mcp.repository;

import com.spring.mcp.model.entity.GroupApiKeyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GroupApiKeyMember entities.
 * Manages API key memberships in flavor groups.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Repository
public interface GroupApiKeyMemberRepository extends JpaRepository<GroupApiKeyMember, Long> {

    /**
     * Find all memberships for a specific group.
     *
     * @param groupId the group ID
     * @return list of API key memberships
     */
    List<GroupApiKeyMember> findByGroupId(Long groupId);

    /**
     * Find all memberships for a specific API key.
     *
     * @param apiKeyId the API key ID
     * @return list of group memberships
     */
    List<GroupApiKeyMember> findByApiKeyId(Long apiKeyId);

    /**
     * Find a specific membership.
     *
     * @param groupId the group ID
     * @param apiKeyId the API key ID
     * @return the membership if exists
     */
    Optional<GroupApiKeyMember> findByGroupIdAndApiKeyId(Long groupId, Long apiKeyId);

    /**
     * Check if an API key is a member of a group.
     *
     * @param groupId the group ID
     * @param apiKeyId the API key ID
     * @return true if the API key is a member
     */
    boolean existsByGroupIdAndApiKeyId(Long groupId, Long apiKeyId);

    /**
     * Count API key members in a group.
     *
     * @param groupId the group ID
     * @return number of API key members
     */
    long countByGroupId(Long groupId);

    /**
     * Delete a specific membership.
     *
     * @param groupId the group ID
     * @param apiKeyId the API key ID
     */
    @Modifying
    @Query("DELETE FROM GroupApiKeyMember gam WHERE gam.group.id = :groupId AND gam.apiKey.id = :apiKeyId")
    void deleteByGroupIdAndApiKeyId(@Param("groupId") Long groupId, @Param("apiKeyId") Long apiKeyId);

    /**
     * Delete all memberships for a group.
     *
     * @param groupId the group ID
     */
    @Modifying
    void deleteByGroupId(Long groupId);

    /**
     * Delete all memberships for an API key.
     *
     * @param apiKeyId the API key ID
     */
    @Modifying
    void deleteByApiKeyId(Long apiKeyId);

    /**
     * Find all active group memberships for an API key.
     * Only includes memberships where the group is active.
     *
     * @param apiKeyId the API key ID
     * @return list of active group memberships
     */
    @Query("""
        SELECT gam FROM GroupApiKeyMember gam
        JOIN FETCH gam.group fg
        WHERE gam.apiKey.id = :apiKeyId
        AND fg.isActive = true
        ORDER BY fg.displayName ASC
        """)
    List<GroupApiKeyMember> findActiveGroupMembershipsForApiKey(@Param("apiKeyId") Long apiKeyId);

    /**
     * Find all group IDs where an API key is a member (active groups only).
     *
     * @param apiKeyId the API key ID
     * @return list of group IDs
     */
    @Query("""
        SELECT gam.group.id FROM GroupApiKeyMember gam
        WHERE gam.apiKey.id = :apiKeyId
        AND gam.group.isActive = true
        """)
    List<Long> findActiveGroupIdsForApiKey(@Param("apiKeyId") Long apiKeyId);
}
