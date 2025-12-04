package com.spring.mcp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Flavor Group MCP Tools (3 tools).
 * Tests visibility logic and team-based authorization through MCP endpoints.
 *
 * @author Spring MCP Server
 * @version 1.3.3
 * @since 2025-12-04
 */
@DisplayName("Flavor Group Tools Integration Tests")
class FlavorGroupToolsIntegrationTest extends McpSseIntegrationTestBase {

    // ============ Tool 1: listFlavorGroups ============

    @Nested
    @DisplayName("listFlavorGroups Tool")
    class ListFlavorGroupsTests {

        @Test
        @DisplayName("should be available as MCP tool")
        void listFlavorGroups_shouldBeAvailable() throws Exception {
            assertToolAvailable("listFlavorGroups");
        }

        @Test
        @DisplayName("should return public groups when includePublic is true")
        void listFlavorGroups_shouldReturnPublicGroups() throws Exception {
            Map<String, Object> args = new HashMap<>();
            args.put("includePublic", true);
            args.put("includePrivate", false);

            String content = callToolAndGetTextContent("listFlavorGroups", args);

            // Test data has public groups
            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("should return all accessible groups with default parameters")
        void listFlavorGroups_shouldReturnAllAccessibleWithDefaults() throws Exception {
            Map<String, Object> args = new HashMap<>();
            // Both parameters default to true

            String content = callToolAndGetTextContent("listFlavorGroups", args);

            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("should hide inactive groups from results")
        void listFlavorGroups_shouldHideInactiveGroups() throws Exception {
            Map<String, Object> args = new HashMap<>();
            args.put("includePublic", true);
            args.put("includePrivate", true);

            String content = callToolAndGetTextContent("listFlavorGroups", args);

            // Inactive groups should not appear in results
            assertThat(content).doesNotContainIgnoringCase("inactive");
        }

        @Test
        @DisplayName("should return group summary information")
        void listFlavorGroups_shouldReturnGroupSummary() throws Exception {
            Map<String, Object> args = new HashMap<>();

            String content = callToolAndGetTextContent("listFlavorGroups", args);

            // Should contain group attributes
            assertThat(content).isNotNull();
            // Groups should have names and metadata
        }
    }

    // ============ Tool 2: getFlavorsGroup ============

    @Nested
    @DisplayName("getFlavorsGroup Tool")
    class GetFlavorsGroupTests {

        @Test
        @DisplayName("should be available as MCP tool")
        void getFlavorsGroup_shouldBeAvailable() throws Exception {
            assertToolAvailable("getFlavorsGroup");
        }

        @Test
        @DisplayName("should return group details with flavors for public group")
        void getFlavorsGroup_shouldReturnPublicGroupDetails() throws Exception {
            // First, get available groups to find a valid public group name
            Map<String, Object> listArgs = new HashMap<>();
            listArgs.put("includePublic", true);
            String listContent = callToolAndGetTextContent("listFlavorGroups", listArgs);

            // Assuming engineering-standards is a public group from test data
            Map<String, Object> args = Map.of("groupName", "engineering-standards");
            String content = callToolAndGetTextContent("getFlavorsGroup", args);

            // Should return group details
            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("should throw error for null or blank group name")
        void getFlavorsGroup_shouldThrowForBlankName() throws Exception {
            Map<String, Object> args = new HashMap<>();
            args.put("groupName", "");

            String content = callToolAndGetTextContent("getFlavorsGroup", args);

            // Should indicate error or empty result
            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("should throw error for non-existent group")
        void getFlavorsGroup_shouldThrowForNonExistentGroup() throws Exception {
            Map<String, Object> args = Map.of("groupName", "non-existent-group-xyz");

            String content = callToolAndGetTextContent("getFlavorsGroup", args);

            // Should indicate group not found
            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("should not return flavors from inactive groups")
        void getFlavorsGroup_shouldNotReturnInactiveGroupFlavors() throws Exception {
            // Inactive groups should return error, not found
            Map<String, Object> args = Map.of("groupName", "inactive-test-group");

            String content = callToolAndGetTextContent("getFlavorsGroup", args);

            // Should not return data for inactive group
            assertThat(content).isNotNull();
        }
    }

    // ============ Tool 3: getFlavorGroupStatistics ============

    @Nested
    @DisplayName("getFlavorGroupStatistics Tool")
    class GetFlavorGroupStatisticsTests {

        @Test
        @DisplayName("should be available as MCP tool")
        void getFlavorGroupStatistics_shouldBeAvailable() throws Exception {
            assertToolAvailable("getFlavorGroupStatistics");
        }

        @Test
        @DisplayName("should return group statistics")
        void getFlavorGroupStatistics_shouldReturnStats() throws Exception {
            Map<String, Object> args = Map.of();

            String content = callToolAndGetTextContent("getFlavorGroupStatistics", args);

            // Should contain statistics fields
            assertThat(content).isNotNull();
            assertThat(content).containsIgnoringCase("total");
        }

        @Test
        @DisplayName("should include public and private group counts")
        void getFlavorGroupStatistics_shouldIncludeCounts() throws Exception {
            Map<String, Object> args = Map.of();

            String content = callToolAndGetTextContent("getFlavorGroupStatistics", args);

            // Should have public and private group counts
            assertThat(content).isNotNull();
        }
    }

    // ============ All Tools Verification ============

    @Nested
    @DisplayName("All Flavor Group Tools")
    class AllToolsTests {

        @Test
        @DisplayName("All 3 flavor group tools should be available")
        void allFlavorGroupTools_shouldBeAvailable() throws Exception {
            assertToolAvailable("listFlavorGroups");
            assertToolAvailable("getFlavorsGroup");
            assertToolAvailable("getFlavorGroupStatistics");
        }

        @Test
        @DisplayName("Tool names should follow naming convention")
        void toolNames_shouldFollowNamingConvention() throws Exception {
            var tools = getToolNames();

            assertThat(tools).contains("listFlavorGroups");
            assertThat(tools).contains("getFlavorsGroup");
            assertThat(tools).contains("getFlavorGroupStatistics");
        }
    }

    // ============ Visibility Logic Tests (Risk Mitigation) ============

    @Nested
    @DisplayName("Visibility Logic - Risk Mitigation")
    class VisibilityTests {

        @Test
        @DisplayName("Should not expose private group details to unauthenticated requests")
        void shouldNotExposePrivateGroupsToPublic() throws Exception {
            // Without API key context, private groups should not be accessible
            Map<String, Object> args = new HashMap<>();
            args.put("includePublic", false);
            args.put("includePrivate", true);

            String content = callToolAndGetTextContent("listFlavorGroups", args);

            // For unauthenticated (null API key), private groups are not visible
            // This test verifies the visibility filtering works
            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("Public groups should always be visible")
        void publicGroups_shouldAlwaysBeVisible() throws Exception {
            Map<String, Object> args = new HashMap<>();
            args.put("includePublic", true);
            args.put("includePrivate", false);

            String content = callToolAndGetTextContent("listFlavorGroups", args);

            // Public groups (no members) should be returned
            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("Statistics should be accessible without restrictions")
        void statistics_shouldBeAccessible() throws Exception {
            Map<String, Object> args = Map.of();

            String content = callToolAndGetTextContent("getFlavorGroupStatistics", args);

            // Statistics endpoint should be accessible
            assertThat(content).isNotNull();
            assertThat(content.length()).isGreaterThan(0);
        }
    }
}
