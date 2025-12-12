package com.spring.mcp.service;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing Flavor Groups with visibility logic.
 * <p>
 * Visibility Rules:
 * <ul>
 *   <li>Unassigned flavors (not in any group) → visible to everyone</li>
 *   <li>Public groups (no members) → visible to everyone</li>
 *   <li>Private groups (has members) → visible only to members</li>
 *   <li>Inactive groups → completely hidden (group AND flavors)</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FlavorGroupService {

    private final FlavorGroupRepository groupRepository;
    private final GroupUserMemberRepository userMemberRepository;
    private final GroupApiKeyMemberRepository apiKeyMemberRepository;
    private final GroupFlavorRepository groupFlavorRepository;
    private final FlavorRepository flavorRepository;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;

    // ==================== Group CRUD Operations ====================

    /**
     * Creates a new flavor group.
     *
     * @param group the group to create
     * @return the created group
     * @throws IllegalArgumentException if unique name already exists
     */
    @Transactional
    public FlavorGroup createGroup(FlavorGroup group) {
        if (groupRepository.existsByUniqueName(group.getUniqueName())) {
            throw new IllegalArgumentException("Group with unique name '" + group.getUniqueName() + "' already exists");
        }
        log.info("Creating new flavor group: {}", group.getUniqueName());
        return groupRepository.save(group);
    }

    /**
     * Updates an existing flavor group.
     * Note: unique_name cannot be changed.
     * Only non-null fields in updates are applied.
     *
     * @param id the group ID
     * @param updates the updated fields (only non-null values are applied)
     * @return the updated group
     * @throws EntityNotFoundException if group not found
     */
    @Transactional
    public FlavorGroup updateGroup(Long id, FlavorGroup updates) {
        FlavorGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));

        // Update only non-null fields (unique_name is immutable)
        if (updates.getDisplayName() != null) {
            group.setDisplayName(updates.getDisplayName());
        }
        if (updates.getDescription() != null) {
            group.setDescription(updates.getDescription());
        }
        if (updates.getIsActive() != null) {
            group.setIsActive(updates.getIsActive());
        }
        if (updates.getUpdatedBy() != null) {
            group.setUpdatedBy(updates.getUpdatedBy());
        }

        log.info("Updated flavor group: {} (id={})", group.getUniqueName(), id);
        return groupRepository.save(group);
    }

    /**
     * Deletes a flavor group and all its associations.
     *
     * @param id the group ID
     * @throws EntityNotFoundException if group not found
     */
    @Transactional
    public void deleteGroup(Long id) {
        FlavorGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
        log.info("Deleting flavor group: {} (id={})", group.getUniqueName(), id);
        groupRepository.delete(group); // Cascade deletes all memberships and flavor associations
    }

    /**
     * Finds a group by ID.
     *
     * @param id the group ID
     * @return the group if found
     */
    public Optional<FlavorGroup> findById(Long id) {
        return groupRepository.findById(id);
    }

    /**
     * Finds a group by unique name.
     *
     * @param uniqueName the unique name
     * @return the group if found
     */
    public Optional<FlavorGroup> findByUniqueName(String uniqueName) {
        return groupRepository.findByUniqueName(uniqueName);
    }

    /**
     * Finds an active group by unique name.
     *
     * @param uniqueName the unique name
     * @return the active group if found
     */
    public Optional<FlavorGroup> findActiveByUniqueName(String uniqueName) {
        return groupRepository.findByUniqueNameAndIsActiveTrue(uniqueName);
    }

    /**
     * Gets all groups (for admin management).
     *
     * @return list of all groups
     */
    public List<FlavorGroup> findAllGroups() {
        return groupRepository.findAllByOrderByDisplayNameAsc();
    }

    /**
     * Gets all active groups.
     *
     * @return list of active groups
     */
    public List<FlavorGroup> findAllActiveGroups() {
        return groupRepository.findByIsActiveTrueOrderByDisplayNameAsc();
    }

    // ==================== Visibility Logic ====================

    /**
     * Gets all accessible groups for an API key.
     * Returns: public groups + private groups where API key is member.
     * Only includes active groups.
     *
     * @param apiKeyId the API key ID (null for unauthenticated access)
     * @return list of accessible groups
     */
    public List<FlavorGroup> findAccessibleGroupsForApiKey(Long apiKeyId) {
        if (apiKeyId == null) {
            // Only public groups for unauthenticated access
            return groupRepository.findAllPublicActiveGroups();
        }
        return groupRepository.findAccessibleGroupsForApiKey(apiKeyId);
    }

    /**
     * Gets all accessible groups for a user.
     * Returns: public groups + private groups where user is member.
     * Only includes active groups.
     *
     * @param userId the user ID (null for public-only access)
     * @return list of accessible groups
     */
    public List<FlavorGroup> findAccessibleGroupsForUser(Long userId) {
        if (userId == null) {
            // Only public groups
            return groupRepository.findAllPublicActiveGroups();
        }
        return groupRepository.findAccessibleGroupsForUser(userId);
    }

    /**
     * Gets all accessible flavor IDs for an API key.
     * Returns IDs of: unassigned flavors + public group flavors + member private group flavors.
     *
     * @param apiKeyId the API key ID (null returns unassigned + public only)
     * @return set of accessible flavor IDs
     */
    public Set<Long> getAccessibleFlavorIdsForApiKey(Long apiKeyId) {
        if (apiKeyId == null) {
            // Unassigned + public groups only
            List<Long> unassignedIds = groupFlavorRepository.findUnassignedFlavors().stream()
                    .map(Flavor::getId)
                    .toList();
            List<Long> publicIds = getPublicGroupFlavorIds();
            return combineLists(unassignedIds, publicIds);
        }
        return groupFlavorRepository.findAccessibleFlavorIdsForApiKey(apiKeyId).stream()
                .collect(Collectors.toSet());
    }

    /**
     * Gets all accessible flavor IDs for a user.
     * Returns IDs of: unassigned flavors + public group flavors + member private group flavors.
     *
     * @param userId the user ID (null returns unassigned + public only)
     * @return set of accessible flavor IDs
     */
    public Set<Long> getAccessibleFlavorIdsForUser(Long userId) {
        if (userId == null) {
            // Unassigned + public groups only
            List<Long> unassignedIds = groupFlavorRepository.findUnassignedFlavors().stream()
                    .map(Flavor::getId)
                    .toList();
            List<Long> publicIds = getPublicGroupFlavorIds();
            return combineLists(unassignedIds, publicIds);
        }
        return groupFlavorRepository.findAccessibleFlavorIdsForUser(userId).stream()
                .collect(Collectors.toSet());
    }

    /**
     * Checks if an API key can access a specific flavor.
     *
     * @param flavorId the flavor ID
     * @param apiKeyId the API key ID (null for public access check)
     * @return true if accessible
     */
    public boolean canApiKeyAccessFlavor(Long flavorId, Long apiKeyId) {
        if (flavorId == null) {
            return false;
        }
        // If no API key, check if flavor is unassigned or in public group
        if (apiKeyId == null) {
            return isFlavorPubliclyAccessible(flavorId);
        }
        return groupFlavorRepository.canApiKeyAccessFlavor(flavorId, apiKeyId);
    }

    /**
     * Checks if a flavor is publicly accessible (unassigned or in public group).
     *
     * @param flavorId the flavor ID
     * @return true if publicly accessible
     */
    public boolean isFlavorPubliclyAccessible(Long flavorId) {
        if (flavorId == null) {
            return false;
        }
        // Check if flavor exists and is active
        Optional<Flavor> flavor = flavorRepository.findById(flavorId);
        if (flavor.isEmpty() || !Boolean.TRUE.equals(flavor.get().getIsActive())) {
            return false;
        }
        // Check if unassigned (not in any group)
        if (!groupFlavorRepository.existsByFlavorId(flavorId)) {
            return true;
        }
        // Check if in any public active group
        List<String> groupNames = groupFlavorRepository.findActiveGroupUniqueNamesForFlavor(flavorId);
        for (String groupName : groupNames) {
            Optional<FlavorGroup> group = groupRepository.findByUniqueNameAndIsActiveTrue(groupName);
            if (group.isPresent() && group.get().isPublic()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an API key can access a specific group.
     *
     * @param groupId the group ID
     * @param apiKeyId the API key ID
     * @return true if accessible
     */
    public boolean canApiKeyAccessGroup(Long groupId, Long apiKeyId) {
        if (groupId == null) {
            return false;
        }
        if (apiKeyId == null) {
            // Check if group is public
            return groupRepository.isGroupPublic(groupId);
        }
        return groupRepository.canApiKeyAccessGroup(groupId, apiKeyId);
    }

    /**
     * Gets flavors in a specific group (if accessible).
     *
     * @param groupUniqueName the group unique name
     * @param apiKeyId the API key ID (null for public access)
     * @return list of flavors in the group
     * @throws EntityNotFoundException if group not found
     * @throws SecurityException if API key cannot access the group
     */
    public List<Flavor> getFlavorsInGroup(String groupUniqueName, Long apiKeyId) {
        FlavorGroup group = groupRepository.findByUniqueNameAndIsActiveTrue(groupUniqueName)
                .orElseThrow(() -> new EntityNotFoundException("Group not found or inactive: " + groupUniqueName));

        // Check access
        if (!canApiKeyAccessGroup(group.getId(), apiKeyId)) {
            throw new SecurityException("API key does not have access to group: " + groupUniqueName);
        }

        return groupFlavorRepository.findActiveFlavorsInGroupByUniqueName(groupUniqueName);
    }

    /**
     * Gets all flavors in a group by group ID (for admin/UI use).
     * No access check - should only be used in web UI context where access is already verified.
     *
     * @param groupId the group ID
     * @return list of flavors in the group
     */
    public List<Flavor> getFlavorsInGroupById(Long groupId) {
        return groupFlavorRepository.findActiveFlavorsInGroup(groupId);
    }

    /**
     * Gets all unassigned flavors (not in any group).
     *
     * @return list of unassigned active flavors
     */
    public List<Flavor> getUnassignedFlavors() {
        return groupFlavorRepository.findUnassignedFlavors();
    }

    // ==================== Membership Management ====================

    /**
     * Adds a user to a group.
     *
     * @param groupId the group ID
     * @param userId the user ID
     * @param addedBy who is adding this membership
     * @throws EntityNotFoundException if group or user not found
     */
    @Transactional
    public void addUserToGroup(Long groupId, Long userId, String addedBy) {
        FlavorGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (userMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            log.debug("User {} is already a member of group {}", userId, groupId);
            return;
        }

        GroupUserMember membership = GroupUserMember.create(group, user, addedBy);
        userMemberRepository.save(membership);
        log.info("Added user {} to group {} (by {})", userId, group.getUniqueName(), addedBy);
    }

    /**
     * Removes a user from a group.
     *
     * @param groupId the group ID
     * @param userId the user ID
     */
    @Transactional
    public void removeUserFromGroup(Long groupId, Long userId) {
        userMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("Removed user {} from group {}", userId, groupId);
    }

    /**
     * Adds an API key to a group.
     *
     * @param groupId the group ID
     * @param apiKeyId the API key ID
     * @param addedBy who is adding this membership
     * @throws EntityNotFoundException if group or API key not found
     */
    @Transactional
    public void addApiKeyToGroup(Long groupId, Long apiKeyId, String addedBy) {
        FlavorGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new EntityNotFoundException("API key not found with id: " + apiKeyId));

        if (apiKeyMemberRepository.existsByGroupIdAndApiKeyId(groupId, apiKeyId)) {
            log.debug("API key {} is already a member of group {}", apiKeyId, groupId);
            return;
        }

        GroupApiKeyMember membership = GroupApiKeyMember.create(group, apiKey, addedBy);
        apiKeyMemberRepository.save(membership);
        log.info("Added API key {} to group {} (by {})", apiKeyId, group.getUniqueName(), addedBy);
    }

    /**
     * Removes an API key from a group.
     *
     * @param groupId the group ID
     * @param apiKeyId the API key ID
     */
    @Transactional
    public void removeApiKeyFromGroup(Long groupId, Long apiKeyId) {
        apiKeyMemberRepository.deleteByGroupIdAndApiKeyId(groupId, apiKeyId);
        log.info("Removed API key {} from group {}", apiKeyId, groupId);
    }

    // ==================== Flavor-Group Association ====================

    /**
     * Adds a flavor to a group.
     *
     * @param groupId the group ID
     * @param flavorId the flavor ID
     * @param addedBy who is adding this association
     * @throws EntityNotFoundException if group or flavor not found
     */
    @Transactional
    public void addFlavorToGroup(Long groupId, Long flavorId, String addedBy) {
        FlavorGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));
        Flavor flavor = flavorRepository.findById(flavorId)
                .orElseThrow(() -> new EntityNotFoundException("Flavor not found with id: " + flavorId));

        if (groupFlavorRepository.existsByGroupIdAndFlavorId(groupId, flavorId)) {
            log.debug("Flavor {} is already in group {}", flavorId, groupId);
            return;
        }

        GroupFlavor association = GroupFlavor.create(group, flavor, addedBy);
        groupFlavorRepository.save(association);
        log.info("Added flavor {} to group {} (by {})", flavor.getUniqueName(), group.getUniqueName(), addedBy);
    }

    /**
     * Removes a flavor from a group.
     *
     * @param groupId the group ID
     * @param flavorId the flavor ID
     */
    @Transactional
    public void removeFlavorFromGroup(Long groupId, Long flavorId) {
        groupFlavorRepository.deleteByGroupIdAndFlavorId(groupId, flavorId);
        log.info("Removed flavor {} from group {}", flavorId, groupId);
    }

    /**
     * Gets flavors available to add to a group.
     * Returns unassigned flavors + flavors in public groups only.
     *
     * @param groupId the group ID
     * @return list of available flavors
     */
    public List<Flavor> getFlavorsAvailableForGroup(Long groupId) {
        return groupFlavorRepository.findFlavorsAvailableForGroup(groupId);
    }

    // ==================== Statistics ====================

    /**
     * Gets group statistics.
     *
     * @return statistics map
     */
    public GroupStatistics getGroupStatistics() {
        return new GroupStatistics(
                groupRepository.count(),
                groupRepository.countByIsActive(true),
                groupRepository.countByIsActive(false),
                groupRepository.countPublicGroups(),
                groupRepository.countPrivateGroups()
        );
    }

    /**
     * Group statistics record.
     */
    public record GroupStatistics(
            long totalGroups,
            long activeGroups,
            long inactiveGroups,
            long publicGroups,
            long privateGroups
    ) {}

    // ==================== Helper Methods ====================

    private List<Long> getPublicGroupFlavorIds() {
        List<FlavorGroup> publicGroups = groupRepository.findAllPublicActiveGroups();
        List<Long> flavorIds = new ArrayList<>();
        for (FlavorGroup group : publicGroups) {
            List<Flavor> flavors = groupFlavorRepository.findActiveFlavorsInGroup(group.getId());
            flavorIds.addAll(flavors.stream().map(Flavor::getId).toList());
        }
        return flavorIds;
    }

    private Set<Long> combineLists(List<Long> list1, List<Long> list2) {
        return java.util.stream.Stream.concat(list1.stream(), list2.stream())
                .collect(Collectors.toSet());
    }
}
