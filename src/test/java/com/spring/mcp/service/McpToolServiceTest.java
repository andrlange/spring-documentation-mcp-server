package com.spring.mcp.service;

import com.spring.mcp.model.dto.McpToolDto;
import com.spring.mcp.model.dto.McpToolGroupDto;
import com.spring.mcp.model.dto.McpToolStatisticsDto;
import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.model.enums.McpToolGroup;
import com.spring.mcp.repository.McpToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for McpToolService.
 * Tests CRUD operations and group management for MCP tool masquerading.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolService Tests")
class McpToolServiceTest {

    @Mock
    private McpToolRepository mcpToolRepository;

    @Mock
    private McpToolMasqueradingService masqueradingService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private McpToolService mcpToolService;

    private McpTool documentationTool;
    private McpTool migrationTool;
    private McpTool disabledTool;

    @BeforeEach
    void setUp() {
        // Set up security context for tests
        SecurityContextHolder.setContext(securityContext);

        // Create test tools
        documentationTool = McpTool.builder()
                .id(1L)
                .toolName("searchSpringDocs")
                .toolGroup(McpToolGroup.DOCUMENTATION)
                .enabled(true)
                .description("Search across all Spring documentation")
                .originalDescription("Search across all Spring documentation")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        migrationTool = McpTool.builder()
                .id(2L)
                .toolName("getBreakingChanges")
                .toolGroup(McpToolGroup.MIGRATION)
                .enabled(true)
                .description("Get breaking changes for a project version")
                .originalDescription("Get breaking changes for a project version")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        disabledTool = McpTool.builder()
                .id(3L)
                .toolName("getClassDoc")
                .toolGroup(McpToolGroup.JAVADOC)
                .enabled(false)
                .description("Get class documentation")
                .originalDescription("Get class documentation")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Get All Groups With Tools Tests ====================

    @Nested
    @DisplayName("getAllGroupsWithTools")
    class GetAllGroupsWithToolsTests {

        @Test
        @DisplayName("Should return all groups with their tools")
        void shouldReturnAllGroupsWithTools() {
            // Given
            when(mcpToolRepository.findAllOrdered())
                    .thenReturn(List.of(documentationTool, migrationTool, disabledTool));

            // When
            List<McpToolGroupDto> result = mcpToolService.getAllGroupsWithTools();

            // Then
            assertThat(result).hasSize(McpToolGroup.values().length);

            // Check DOCUMENTATION group has its tool
            McpToolGroupDto docGroup = result.stream()
                    .filter(g -> g.getGroup() == McpToolGroup.DOCUMENTATION)
                    .findFirst()
                    .orElseThrow();
            assertThat(docGroup.getTools()).hasSize(1);
            assertThat(docGroup.getTools().get(0).getToolName()).isEqualTo("searchSpringDocs");
        }

        @Test
        @DisplayName("Should return empty groups for groups without tools")
        void shouldReturnEmptyGroupsForGroupsWithoutTools() {
            // Given
            when(mcpToolRepository.findAllOrdered()).thenReturn(List.of(documentationTool));

            // When
            List<McpToolGroupDto> result = mcpToolService.getAllGroupsWithTools();

            // Then
            McpToolGroupDto langGroup = result.stream()
                    .filter(g -> g.getGroup() == McpToolGroup.LANGUAGE)
                    .findFirst()
                    .orElseThrow();
            assertThat(langGroup.getTools()).isEmpty();
        }
    }

    // ==================== Get Statistics Tests ====================

    @Nested
    @DisplayName("getStatistics")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should return correct statistics")
        void shouldReturnCorrectStatistics() {
            // Given
            when(mcpToolRepository.count()).thenReturn(44L);
            when(mcpToolRepository.countByEnabledTrue()).thenReturn(40L);
            when(mcpToolRepository.countModifiedDescriptions()).thenReturn(5L);

            // When
            McpToolStatisticsDto stats = mcpToolService.getStatistics();

            // Then
            assertThat(stats.getTotalTools()).isEqualTo(44L);
            assertThat(stats.getEnabledTools()).isEqualTo(40L);
            assertThat(stats.getDisabledTools()).isEqualTo(4L);
            assertThat(stats.getModifiedTools()).isEqualTo(5L);
            assertThat(stats.getGroupCount()).isEqualTo(McpToolGroup.values().length);
        }

        @Test
        @DisplayName("Should handle zero tools")
        void shouldHandleZeroTools() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.countByEnabledTrue()).thenReturn(0L);
            when(mcpToolRepository.countModifiedDescriptions()).thenReturn(0L);

            // When
            McpToolStatisticsDto stats = mcpToolService.getStatistics();

            // Then
            assertThat(stats.getTotalTools()).isZero();
            assertThat(stats.getEnabledTools()).isZero();
            assertThat(stats.getDisabledTools()).isZero();
            assertThat(stats.getModifiedTools()).isZero();
        }
    }

    // ==================== Get Tool Tests ====================

    @Nested
    @DisplayName("getTool")
    class GetToolTests {

        @Test
        @DisplayName("Should return tool when found")
        void shouldReturnToolWhenFound() {
            // Given
            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));

            // When
            McpToolDto result = mcpToolService.getTool("searchSpringDocs");

            // Then
            assertThat(result.getToolName()).isEqualTo("searchSpringDocs");
            assertThat(result.getToolGroup()).isEqualTo(McpToolGroup.DOCUMENTATION);
            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when tool not found")
        void shouldThrowExceptionWhenToolNotFound() {
            // Given
            when(mcpToolRepository.findByToolName("nonExistent"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> mcpToolService.getTool("nonExistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tool not found");
        }
    }

    // ==================== Toggle Tool Tests ====================

    @Nested
    @DisplayName("toggleTool")
    class ToggleToolTests {

        @Test
        @DisplayName("Should enable a disabled tool")
        void shouldEnableDisabledTool() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            when(mcpToolRepository.findByToolName("getClassDoc"))
                    .thenReturn(Optional.of(disabledTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            McpToolDto result = mcpToolService.toggleTool("getClassDoc", true);

            // Then
            assertThat(result.isEnabled()).isTrue();
            verify(masqueradingService).applyToolVisibility(any(McpTool.class));
        }

        @Test
        @DisplayName("Should disable an enabled tool")
        void shouldDisableEnabledTool() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            McpToolDto result = mcpToolService.toggleTool("searchSpringDocs", false);

            // Then
            assertThat(result.isEnabled()).isFalse();
            verify(masqueradingService).applyToolVisibility(any(McpTool.class));
        }

        @Test
        @DisplayName("Should throw exception when toggling non-existent tool")
        void shouldThrowExceptionWhenTogglingNonExistentTool() {
            // Given
            when(mcpToolRepository.findByToolName("nonExistent"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> mcpToolService.toggleTool("nonExistent", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tool not found");
        }

        @Test
        @DisplayName("Should set updatedBy to current username")
        void shouldSetUpdatedByToCurrentUsername() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("testadmin");

            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> {
                        McpTool saved = inv.getArgument(0);
                        assertThat(saved.getUpdatedBy()).isEqualTo("testadmin");
                        return saved;
                    });

            // When
            mcpToolService.toggleTool("searchSpringDocs", false);

            // Then
            verify(mcpToolRepository).save(any(McpTool.class));
        }
    }

    // ==================== Toggle Group Tests ====================

    @Nested
    @DisplayName("toggleGroup")
    class ToggleGroupTests {

        @Test
        @DisplayName("Should toggle all tools in a group")
        void shouldToggleAllToolsInGroup() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            when(mcpToolRepository.updateGroupEnabled(
                    eq(McpToolGroup.DOCUMENTATION), eq(false), eq("admin")))
                    .thenReturn(10);
            when(mcpToolRepository.findByToolGroup(McpToolGroup.DOCUMENTATION))
                    .thenReturn(List.of(documentationTool));

            // When
            int updated = mcpToolService.toggleGroup(McpToolGroup.DOCUMENTATION, false);

            // Then
            assertThat(updated).isEqualTo(10);
            verify(masqueradingService).applyToolVisibility(documentationTool);
        }

        @Test
        @DisplayName("Should apply masquerading to all tools in group")
        void shouldApplyMasqueradingToAllToolsInGroup() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            McpTool tool1 = McpTool.builder().id(1L).toolName("tool1").build();
            McpTool tool2 = McpTool.builder().id(2L).toolName("tool2").build();
            McpTool tool3 = McpTool.builder().id(3L).toolName("tool3").build();

            when(mcpToolRepository.updateGroupEnabled(any(), anyBoolean(), anyString()))
                    .thenReturn(3);
            when(mcpToolRepository.findByToolGroup(McpToolGroup.MIGRATION))
                    .thenReturn(List.of(tool1, tool2, tool3));

            // When
            mcpToolService.toggleGroup(McpToolGroup.MIGRATION, true);

            // Then
            verify(masqueradingService, times(3)).applyToolVisibility(any(McpTool.class));
        }
    }

    // ==================== Update Description Tests ====================

    @Nested
    @DisplayName("updateDescription")
    class UpdateDescriptionTests {

        @Test
        @DisplayName("Should update tool description")
        void shouldUpdateToolDescription() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            McpToolDto result = mcpToolService.updateDescription(
                    "searchSpringDocs", "New description for searching docs");

            // Then
            assertThat(result.getDescription()).isEqualTo("New description for searching docs");
            verify(masqueradingService).updateToolDescription(any(McpTool.class));
        }

        @Test
        @DisplayName("Should not update MCP server for disabled tool")
        void shouldNotUpdateMcpServerForDisabledTool() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            when(mcpToolRepository.findByToolName("getClassDoc"))
                    .thenReturn(Optional.of(disabledTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            mcpToolService.updateDescription("getClassDoc", "New description");

            // Then
            verify(masqueradingService, never()).updateToolDescription(any());
        }

        @Test
        @DisplayName("Should throw exception for empty description")
        void shouldThrowExceptionForEmptyDescription() {
            // Given
            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));

            // When/Then
            assertThatThrownBy(() -> mcpToolService.updateDescription("searchSpringDocs", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be empty");
        }

        @Test
        @DisplayName("Should throw exception for null description")
        void shouldThrowExceptionForNullDescription() {
            // Given
            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));

            // When/Then
            assertThatThrownBy(() -> mcpToolService.updateDescription("searchSpringDocs", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be empty");
        }

        @Test
        @DisplayName("Should throw exception for blank description")
        void shouldThrowExceptionForBlankDescription() {
            // Given
            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));

            // When/Then
            assertThatThrownBy(() -> mcpToolService.updateDescription("searchSpringDocs", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be empty");
        }

        @Test
        @DisplayName("Should trim description before saving")
        void shouldTrimDescriptionBeforeSaving() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> {
                        McpTool saved = inv.getArgument(0);
                        assertThat(saved.getDescription()).isEqualTo("Trimmed description");
                        return saved;
                    });

            // When
            mcpToolService.updateDescription("searchSpringDocs", "  Trimmed description  ");

            // Then
            verify(mcpToolRepository).save(any(McpTool.class));
        }
    }

    // ==================== Reset To Original Tests ====================

    @Nested
    @DisplayName("resetToOriginal")
    class ResetToOriginalTests {

        @Test
        @DisplayName("Should reset modified description to original")
        void shouldResetModifiedDescriptionToOriginal() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");

            McpTool modifiedTool = McpTool.builder()
                    .id(1L)
                    .toolName("searchSpringDocs")
                    .toolGroup(McpToolGroup.DOCUMENTATION)
                    .enabled(true)
                    .description("Modified description")
                    .originalDescription("Original description")
                    .build();

            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(modifiedTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            McpToolDto result = mcpToolService.resetToOriginal("searchSpringDocs");

            // Then
            assertThat(result.getDescription()).isEqualTo("Original description");
            assertThat(result.isModified()).isFalse();
            verify(masqueradingService).updateToolDescription(any(McpTool.class));
        }

        @Test
        @DisplayName("Should not save or update if description was not modified")
        void shouldNotSaveIfNotModified() {
            // Given
            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool)); // original == description

            // When
            mcpToolService.resetToOriginal("searchSpringDocs");

            // Then
            verify(mcpToolRepository, never()).save(any());
            verify(masqueradingService, never()).updateToolDescription(any());
        }

        @Test
        @DisplayName("Should throw exception for non-existent tool")
        void shouldThrowExceptionForNonExistentTool() {
            // Given
            when(mcpToolRepository.findByToolName("nonExistent"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> mcpToolService.resetToOriginal("nonExistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tool not found");
        }
    }

    // ==================== Is Description Modified Tests ====================

    @Nested
    @DisplayName("isDescriptionModified")
    class IsDescriptionModifiedTests {

        @Test
        @DisplayName("Should return true when description is modified")
        void shouldReturnTrueWhenDescriptionIsModified() {
            // Given
            McpTool modifiedTool = McpTool.builder()
                    .toolName("tool")
                    .description("Modified")
                    .originalDescription("Original")
                    .build();

            when(mcpToolRepository.findByToolName("tool"))
                    .thenReturn(Optional.of(modifiedTool));

            // When
            boolean result = mcpToolService.isDescriptionModified("tool");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when description is not modified")
        void shouldReturnFalseWhenDescriptionIsNotModified() {
            // Given
            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));

            // When
            boolean result = mcpToolService.isDescriptionModified("searchSpringDocs");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw exception for non-existent tool")
        void shouldThrowExceptionForNonExistentTool() {
            // Given
            when(mcpToolRepository.findByToolName("nonExistent"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> mcpToolService.isDescriptionModified("nonExistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tool not found");
        }
    }

    // ==================== Get Modified Tools Tests ====================

    @Nested
    @DisplayName("getModifiedTools")
    class GetModifiedToolsTests {

        @Test
        @DisplayName("Should return list of modified tools")
        void shouldReturnListOfModifiedTools() {
            // Given
            McpTool modifiedTool1 = McpTool.builder()
                    .id(1L)
                    .toolName("tool1")
                    .toolGroup(McpToolGroup.DOCUMENTATION)
                    .description("Modified1")
                    .originalDescription("Original1")
                    .build();

            McpTool modifiedTool2 = McpTool.builder()
                    .id(2L)
                    .toolName("tool2")
                    .toolGroup(McpToolGroup.MIGRATION)
                    .description("Modified2")
                    .originalDescription("Original2")
                    .build();

            when(mcpToolRepository.findAllWithModifiedDescriptions())
                    .thenReturn(List.of(modifiedTool1, modifiedTool2));

            // When
            List<McpToolDto> result = mcpToolService.getModifiedTools();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getToolName()).isEqualTo("tool1");
            assertThat(result.get(1).getToolName()).isEqualTo("tool2");
        }

        @Test
        @DisplayName("Should return empty list when no tools are modified")
        void shouldReturnEmptyListWhenNoToolsAreModified() {
            // Given
            when(mcpToolRepository.findAllWithModifiedDescriptions())
                    .thenReturn(List.of());

            // When
            List<McpToolDto> result = mcpToolService.getModifiedTools();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== Current Username Tests ====================

    @Nested
    @DisplayName("getCurrentUsername")
    class GetCurrentUsernameTests {

        @Test
        @DisplayName("Should return 'system' when no authentication")
        void shouldReturnSystemWhenNoAuthentication() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(null);

            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> {
                        McpTool saved = inv.getArgument(0);
                        assertThat(saved.getUpdatedBy()).isEqualTo("system");
                        return saved;
                    });

            // When
            mcpToolService.toggleTool("searchSpringDocs", false);

            // Then
            verify(mcpToolRepository).save(any(McpTool.class));
        }

        @Test
        @DisplayName("Should return 'system' when not authenticated")
        void shouldReturnSystemWhenNotAuthenticated() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            when(mcpToolRepository.findByToolName("searchSpringDocs"))
                    .thenReturn(Optional.of(documentationTool));
            when(mcpToolRepository.save(any(McpTool.class)))
                    .thenAnswer(inv -> {
                        McpTool saved = inv.getArgument(0);
                        assertThat(saved.getUpdatedBy()).isEqualTo("system");
                        return saved;
                    });

            // When
            mcpToolService.toggleTool("searchSpringDocs", false);

            // Then
            verify(mcpToolRepository).save(any(McpTool.class));
        }
    }
}
