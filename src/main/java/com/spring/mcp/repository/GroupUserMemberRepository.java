package com.spring.mcp.repository;

import com.spring.mcp.model.entity.GroupUserMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GroupUserMember entities.
 * Manages user memberships in flavor groups.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Repository
public interface GroupUserMemberRepository extends JpaRepository<GroupUserMember, Long> {

    /**
     * Find all memberships for a specific group.
     *
     * @param groupId the group ID
     * @return list of user memberships
     */
    List<GroupUserMember> findByGroupId(Long groupId);

    /**
     * Find all memberships for a specific user.
     *
     * @param userId the user ID
     * @return list of group memberships
     */
    List<GroupUserMember> findByUserId(Long userId);

    /**
     * Find a specific membership.
     *
     * @param groupId the group ID
     * @param userId the user ID
     * @return the membership if exists
     */
    Optional<GroupUserMember> findByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * Check if a user is a member of a group.
     *
     * @param groupId the group ID
     * @param userId the user ID
     * @return true if the user is a member
     */
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    /**
     * Count members in a group.
     *
     * @param groupId the group ID
     * @return number of user members
     */
    long countByGroupId(Long groupId);

    /**
     * Delete a specific membership.
     *
     * @param groupId the group ID
     * @param userId the user ID
     */
    @Modifying
    @Query("DELETE FROM GroupUserMember gum WHERE gum.group.id = :groupId AND gum.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    /**
     * Delete all memberships for a group.
     *
     * @param groupId the group ID
     */
    @Modifying
    void deleteByGroupId(Long groupId);

    /**
     * Delete all memberships for a user.
     *
     * @param userId the user ID
     */
    @Modifying
    void deleteByUserId(Long userId);

    /**
     * Find all active group memberships for a user.
     * Only includes memberships where the group is active.
     *
     * @param userId the user ID
     * @return list of active group memberships
     */
    @Query("""
        SELECT gum FROM GroupUserMember gum
        JOIN FETCH gum.group fg
        WHERE gum.user.id = :userId
        AND fg.isActive = true
        ORDER BY fg.displayName ASC
        """)
    List<GroupUserMember> findActiveGroupMembershipsForUser(@Param("userId") Long userId);
}
