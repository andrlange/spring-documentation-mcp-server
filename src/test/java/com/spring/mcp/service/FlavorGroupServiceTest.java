package com.spring.mcp.service;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FlavorGroupService.
 * Tests visibility logic and risk mitigation scenarios.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlavorGroupService Tests")
class FlavorGroupServiceTest {

    @Mock
    private FlavorGroupRepository groupRepository;

    @Mock
    private GroupUserMemberRepository userMemberRepository;

    @Mock
    private GroupApiKeyMemberRepository apiKeyMemberRepository;

    @Mock
    private GroupFlavorRepository groupFlavorRepository;

    @Mock
    private FlavorRepository flavorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private FlavorGroupService service;

    private FlavorGroup publicGroup;
    private FlavorGroup privateGroup;
    private FlavorGroup inactiveGroup;
    private Flavor unassignedFlavor;
    private Flavor publicGroupFlavor;
    private Flavor privateGroupFlavor;
    private Flavor inactiveGroupFlavor;

    @BeforeEach
    void setUp() {
        // Public group (no members)
        publicGroup = FlavorGroup.builder()
                .id(1L)
                .uniqueName("public-group")
                .displayName("Public Group")
                .isActive(true)
                .build();

        // Private group (has members)
        privateGroup = FlavorGroup.builder()
                .id(2L)
                .uniqueName("private-group")
                .displayName("Private Group")
                .isActive(true)
                .build();

        // Inactive group
        inactiveGroup = FlavorGroup.builder()
                .id(3L)
                .uniqueName("inactive-group")
                .displayName("Inactive Group")
                .isActive(false)
                .build();

        // Flavors
        unassignedFlavor = Flavor.builder().id(1L).uniqueName("unassigned").isActive(true).build();
        publicGroupFlavor = Flavor.builder().id(2L).uniqueName("public-flavor").isActive(true).build();
        privateGroupFlavor = Flavor.builder().id(3L).uniqueName("private-flavor").isActive(true).build();
        inactiveGroupFlavor = Flavor.builder().id(4L).uniqueName("inactive-flavor").isActive(true).build();
    }

    // ==================== Group CRUD Tests ====================

    @Nested
    @DisplayName("Group CRUD Operations")
    class GroupCrudTests {

        @Test
        @DisplayName("Should create new group successfully")
        void createGroup_success() {
            when(groupRepository.existsByUniqueName("new-group")).thenReturn(false);
            when(groupRepository.save(any(FlavorGroup.class))).thenAnswer(inv -> inv.getArgument(0));

            FlavorGroup newGroup = FlavorGroup.builder()
                    .uniqueName("new-group")
                    .displayName("New Group")
                    .isActive(true)
                    .build();

            FlavorGroup result = service.createGroup(newGroup);

            assertThat(result.getUniqueName()).isEqualTo("new-group");
            verify(groupRepository).save(newGroup);
        }

        @Test
        @DisplayName("Should throw exception when creating group with duplicate name")
        void createGroup_duplicateName_throws() {
            when(groupRepository.existsByUniqueName("existing-group")).thenReturn(true);

            FlavorGroup newGroup = FlavorGroup.builder()
                    .uniqueName("existing-group")
                    .displayName("Existing Group")
                    .build();

            assertThatThrownBy(() -> service.createGroup(newGroup))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should update group successfully")
        void updateGroup_success() {
            when(groupRepository.findById(1L)).thenReturn(Optional.of(publicGroup));
            when(groupRepository.save(any(FlavorGroup.class))).thenAnswer(inv -> inv.getArgument(0));

            FlavorGroup updates = FlavorGroup.builder()
                    .displayName("Updated Name")
                    .description("Updated description")
                    .isActive(true)
                    .build();

            FlavorGroup result = service.updateGroup(1L, updates);

            assertThat(result.getDisplayName()).isEqualTo("Updated Name");
            assertThat(result.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent group")
        void updateGroup_notFound_throws() {
            when(groupRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateGroup(999L, new FlavorGroup()))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should delete group with cascade")
        void deleteGroup_success() {
            when(groupRepository.findById(1L)).thenReturn(Optional.of(publicGroup));
            doNothing().when(groupRepository).delete(publicGroup);

            service.deleteGroup(1L);

            verify(groupRepository).delete(publicGroup);
        }
    }

    // ==================== Visibility Logic Tests (Risk Mitigation) ====================

    @Nested
    @DisplayName("Visibility Logic - Risk Mitigation")
    class VisibilityTests {

        @Test
        @DisplayName("Should return unassigned flavors as accessible to everyone")
        void unassignedFlavors_accessibleToAll() {
            when(groupFlavorRepository.findUnassignedFlavors()).thenReturn(List.of(unassignedFlavor));

            // With null API key (unauthenticated)
            List<Flavor> unassigned = service.getUnassignedFlavors();

            assertThat(unassigned).contains(unassignedFlavor);
        }

        @Test
        @DisplayName("Should return public groups for any API key (including null)")
        void publicGroups_accessibleToAll() {
            when(groupRepository.findAllPublicActiveGroups()).thenReturn(List.of(publicGroup));

            // With null API key (unauthenticated)
            List<FlavorGroup> groups = service.findAccessibleGroupsForApiKey(null);

            assertThat(groups).contains(publicGroup);
        }

        @Test
        @DisplayName("Should return public + private member groups for authenticated API key")
        void authenticatedApiKey_seesPublicAndMemberGroups() {
            when(groupRepository.findAccessibleGroupsForApiKey(100L))
                    .thenReturn(List.of(publicGroup, privateGroup));

            List<FlavorGroup> groups = service.findAccessibleGroupsForApiKey(100L);

            assertThat(groups).hasSize(2);
            assertThat(groups).contains(publicGroup, privateGroup);
        }

        @Test
        @DisplayName("Should hide inactive groups completely")
        void inactiveGroups_completelyHidden() {
            // findAccessibleGroupsForApiKey should not return inactive groups
            when(groupRepository.findAccessibleGroupsForApiKey(100L))
                    .thenReturn(List.of(publicGroup)); // Only active groups

            List<FlavorGroup> groups = service.findAccessibleGroupsForApiKey(100L);

            assertThat(groups).doesNotContain(inactiveGroup);
        }

        @Test
        @DisplayName("Should detect public group correctly (no members)")
        void detectPublicGroup_noMembers() {
            // Public group has empty member lists
            assertThat(publicGroup.isPublic()).isTrue();

            // Private group would have members
            privateGroup.setUserMembers(List.of(new GroupUserMember()));
            assertThat(privateGroup.isPublic()).isFalse();
        }

        @Test
        @DisplayName("Should check if API key can access flavor correctly")
        void apiKeyCanAccessFlavor_correctCheck() {
            when(groupFlavorRepository.canApiKeyAccessFlavor(3L, 100L)).thenReturn(true);
            when(groupFlavorRepository.canApiKeyAccessFlavor(3L, 200L)).thenReturn(false);

            // Member can access
            assertThat(service.canApiKeyAccessFlavor(3L, 100L)).isTrue();

            // Non-member cannot access
            assertThat(service.canApiKeyAccessFlavor(3L, 200L)).isFalse();
        }

        @Test
        @DisplayName("Should check if flavor is publicly accessible")
        void isFlavorPubliclyAccessible_unassigned() {
            when(flavorRepository.findById(1L)).thenReturn(Optional.of(unassignedFlavor));
            when(groupFlavorRepository.existsByFlavorId(1L)).thenReturn(false); // Not in any group

            boolean accessible = service.isFlavorPubliclyAccessible(1L);

            assertThat(accessible).isTrue();
        }

        @Test
        @DisplayName("Should return false for inactive flavor")
        void isFlavorPubliclyAccessible_inactiveFlavor() {
            Flavor inactiveFlavor = Flavor.builder().id(5L).isActive(false).build();
            when(flavorRepository.findById(5L)).thenReturn(Optional.of(inactiveFlavor));

            boolean accessible = service.isFlavorPubliclyAccessible(5L);

            assertThat(accessible).isFalse();
        }

        @Test
        @DisplayName("Should return accessible flavor IDs for API key")
        void getAccessibleFlavorIdsForApiKey() {
            when(groupFlavorRepository.findAccessibleFlavorIdsForApiKey(100L))
                    .thenReturn(List.of(1L, 2L, 3L));

            Set<Long> ids = service.getAccessibleFlavorIdsForApiKey(100L);

            assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("Should return only public flavors for null API key")
        void getAccessibleFlavorIdsForApiKey_nullApiKey() {
            when(groupFlavorRepository.findUnassignedFlavors()).thenReturn(List.of(unassignedFlavor));
            when(groupRepository.findAllPublicActiveGroups()).thenReturn(List.of());

            Set<Long> ids = service.getAccessibleFlavorIdsForApiKey(null);

            assertThat(ids).contains(1L); // Unassigned flavor ID
        }
    }

    // ==================== Membership Management Tests ====================

    @Nested
    @DisplayName("Membership Management")
    class MembershipTests {

        @Test
        @DisplayName("Should add user to group")
        void addUserToGroup_success() {
            User user = User.builder().id(1L).username("testuser").build();
            when(groupRepository.findById(1L)).thenReturn(Optional.of(publicGroup));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMemberRepository.existsByGroupIdAndUserId(1L, 1L)).thenReturn(false);
            when(userMemberRepository.save(any(GroupUserMember.class))).thenAnswer(inv -> inv.getArgument(0));

            service.addUserToGroup(1L, 1L, "admin");

            verify(userMemberRepository).save(any(GroupUserMember.class));
        }

        @Test
        @DisplayName("Should skip adding duplicate user membership")
        void addUserToGroup_duplicate_skips() {
            User user = User.builder().id(1L).username("testuser").build();
            when(groupRepository.findById(1L)).thenReturn(Optional.of(publicGroup));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMemberRepository.existsByGroupIdAndUserId(1L, 1L)).thenReturn(true);

            service.addUserToGroup(1L, 1L, "admin");

            verify(userMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should add API key to group")
        void addApiKeyToGroup_success() {
            ApiKey apiKey = new ApiKey();
            apiKey.setId(1L);
            apiKey.setName("test-key");
            when(groupRepository.findById(1L)).thenReturn(Optional.of(publicGroup));
            when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(apiKey));
            when(apiKeyMemberRepository.existsByGroupIdAndApiKeyId(1L, 1L)).thenReturn(false);
            when(apiKeyMemberRepository.save(any(GroupApiKeyMember.class))).thenAnswer(inv -> inv.getArgument(0));

            service.addApiKeyToGroup(1L, 1L, "admin");

            verify(apiKeyMemberRepository).save(any(GroupApiKeyMember.class));
        }

        @Test
        @DisplayName("Should remove user from group")
        void removeUserFromGroup_success() {
            doNothing().when(userMemberRepository).deleteByGroupIdAndUserId(1L, 1L);

            service.removeUserFromGroup(1L, 1L);

            verify(userMemberRepository).deleteByGroupIdAndUserId(1L, 1L);
        }
    }

    // ==================== Flavor-Group Association Tests ====================

    @Nested
    @DisplayName("Flavor-Group Association")
    class FlavorAssociationTests {

        @Test
        @DisplayName("Should add flavor to group")
        void addFlavorToGroup_success() {
            when(groupRepository.findById(1L)).thenReturn(Optional.of(publicGroup));
            when(flavorRepository.findById(1L)).thenReturn(Optional.of(unassignedFlavor));
            when(groupFlavorRepository.existsByGroupIdAndFlavorId(1L, 1L)).thenReturn(false);
            when(groupFlavorRepository.save(any(GroupFlavor.class))).thenAnswer(inv -> inv.getArgument(0));

            service.addFlavorToGroup(1L, 1L, "admin");

            verify(groupFlavorRepository).save(any(GroupFlavor.class));
        }

        @Test
        @DisplayName("Should remove flavor from group")
        void removeFlavorFromGroup_success() {
            doNothing().when(groupFlavorRepository).deleteByGroupIdAndFlavorId(1L, 1L);

            service.removeFlavorFromGroup(1L, 1L);

            verify(groupFlavorRepository).deleteByGroupIdAndFlavorId(1L, 1L);
        }

        @Test
        @DisplayName("Should get flavors in group for authorized API key")
        void getFlavorsInGroup_authorized() {
            when(groupRepository.findByUniqueNameAndIsActiveTrue("public-group"))
                    .thenReturn(Optional.of(publicGroup));
            when(groupRepository.canApiKeyAccessGroup(1L, 100L)).thenReturn(true);
            when(groupFlavorRepository.findActiveFlavorsInGroupByUniqueName("public-group"))
                    .thenReturn(List.of(publicGroupFlavor));

            List<Flavor> flavors = service.getFlavorsInGroup("public-group", 100L);

            assertThat(flavors).contains(publicGroupFlavor);
        }

        @Test
        @DisplayName("Should throw SecurityException for unauthorized access")
        void getFlavorsInGroup_unauthorized() {
            when(groupRepository.findByUniqueNameAndIsActiveTrue("private-group"))
                    .thenReturn(Optional.of(privateGroup));
            when(groupRepository.canApiKeyAccessGroup(2L, 999L)).thenReturn(false);

            assertThatThrownBy(() -> service.getFlavorsInGroup("private-group", 999L))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("does not have access");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException for non-existent group")
        void getFlavorsInGroup_groupNotFound() {
            when(groupRepository.findByUniqueNameAndIsActiveTrue("non-existent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getFlavorsInGroup("non-existent", 100L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should return correct group statistics")
        void getGroupStatistics() {
            when(groupRepository.count()).thenReturn(10L);
            when(groupRepository.countByIsActive(true)).thenReturn(8L);
            when(groupRepository.countByIsActive(false)).thenReturn(2L);
            when(groupRepository.countPublicGroups()).thenReturn(5L);
            when(groupRepository.countPrivateGroups()).thenReturn(3L);

            FlavorGroupService.GroupStatistics stats = service.getGroupStatistics();

            assertThat(stats.totalGroups()).isEqualTo(10L);
            assertThat(stats.activeGroups()).isEqualTo(8L);
            assertThat(stats.inactiveGroups()).isEqualTo(2L);
            assertThat(stats.publicGroups()).isEqualTo(5L);
            assertThat(stats.privateGroups()).isEqualTo(3L);
        }
    }
}
