package com.spring.mcp.config;

import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.model.enums.McpToolGroup;
import com.spring.mcp.repository.McpToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for McpToolsInitializer.
 * Tests tool initialization on startup.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolsInitializer Tests")
class McpToolsInitializerTest {

    @Mock
    private McpToolRepository mcpToolRepository;

    private McpToolsInitializer initializer;

    @Captor
    private ArgumentCaptor<McpTool> toolCaptor;

    @BeforeEach
    void setUp() {
        initializer = new McpToolsInitializer(mcpToolRepository);
    }

    // ==================== Initialization Tests ====================

    @Nested
    @DisplayName("run (ApplicationRunner)")
    class InitializeTests {

        @Test
        @DisplayName("Should populate tools when table is empty")
        void shouldPopulateToolsWhenTableIsEmpty() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then - saves 44 individual tools
            verify(mcpToolRepository, times(44)).save(any(McpTool.class));
        }

        @Test
        @DisplayName("Should skip initialization when tools already exist")
        void shouldSkipInitializationWhenToolsExist() {
            // Given
            when(mcpToolRepository.count()).thenReturn(44L);

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, never()).save(any(McpTool.class));
        }

        @Test
        @DisplayName("Should create all tool groups")
        void shouldCreateAllToolGroups() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            // Check all groups are represented
            long docCount = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.DOCUMENTATION).count();
            long migrationCount = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.MIGRATION).count();
            long languageCount = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.LANGUAGE).count();
            long flavorsCount = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.FLAVORS).count();
            long flavorGroupsCount = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.FLAVOR_GROUPS).count();
            long initializrCount = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.INITIALIZR).count();
            long javadocCount = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.JAVADOC).count();

            assertThat(docCount).isEqualTo(10);
            assertThat(migrationCount).isEqualTo(7);
            assertThat(languageCount).isEqualTo(7);
            assertThat(flavorsCount).isEqualTo(8);
            assertThat(flavorGroupsCount).isEqualTo(3);
            assertThat(initializrCount).isEqualTo(5);
            assertThat(javadocCount).isEqualTo(4);
        }

        @Test
        @DisplayName("Should set all tools as enabled by default")
        void shouldSetAllToolsAsEnabledByDefault() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            assertThat(savedTools).allMatch(McpTool::getEnabled);
        }

        @Test
        @DisplayName("Should set original description same as description")
        void shouldSetOriginalDescriptionSameAsDescription() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            assertThat(savedTools).allMatch(t ->
                    t.getDescription().equals(t.getOriginalDescription()));
        }

        @Test
        @DisplayName("Should include expected documentation tools")
        void shouldIncludeExpectedDocumentationTools() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            List<String> docToolNames = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.DOCUMENTATION)
                    .map(McpTool::getToolName)
                    .toList();

            assertThat(docToolNames).contains(
                    "searchSpringDocs",
                    "getSpringVersions",
                    "listSpringProjects",
                    "getDocumentationByVersion",
                    "getCodeExamples",
                    "listSpringBootVersions",
                    "getLatestSpringBootVersion",
                    "filterSpringBootVersionsBySupport",
                    "listProjectsBySpringBootVersion",
                    "findProjectsByUseCase"
            );
        }

        @Test
        @DisplayName("Should include expected migration tools")
        void shouldIncludeExpectedMigrationTools() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            List<String> migrationToolNames = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.MIGRATION)
                    .map(McpTool::getToolName)
                    .toList();

            assertThat(migrationToolNames).contains(
                    "getSpringMigrationGuide",
                    "getBreakingChanges",
                    "searchMigrationKnowledge",
                    "getAvailableMigrationPaths",
                    "getTransformationsByType",
                    "getDeprecationReplacement",
                    "checkVersionCompatibility"
            );
        }

        @Test
        @DisplayName("Should include expected javadoc tools")
        void shouldIncludeExpectedJavadocTools() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            List<String> javadocToolNames = savedTools.stream()
                    .filter(t -> t.getToolGroup() == McpToolGroup.JAVADOC)
                    .map(McpTool::getToolName)
                    .toList();

            assertThat(javadocToolNames).contains(
                    "getClassDoc",
                    "getPackageDoc",
                    "searchJavadocs",
                    "listJavadocLibraries"
            );
        }

        @Test
        @DisplayName("Should set non-null descriptions for all tools")
        void shouldSetNonNullDescriptionsForAllTools() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            assertThat(savedTools).allMatch(t ->
                    t.getDescription() != null && !t.getDescription().isBlank());
        }

        @Test
        @DisplayName("Should set non-null tool names for all tools")
        void shouldSetNonNullToolNamesForAllTools() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            assertThat(savedTools).allMatch(t ->
                    t.getToolName() != null && !t.getToolName().isBlank());
        }

        @Test
        @DisplayName("Should have unique tool names")
        void shouldHaveUniqueToolNames() {
            // Given
            when(mcpToolRepository.count()).thenReturn(0L);
            when(mcpToolRepository.save(any(McpTool.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            initializer.run(new DefaultApplicationArguments());

            // Then
            verify(mcpToolRepository, atLeast(44)).save(toolCaptor.capture());
            List<McpTool> savedTools = toolCaptor.getAllValues();

            List<String> toolNames = savedTools.stream()
                    .map(McpTool::getToolName)
                    .toList();

            assertThat(toolNames).doesNotHaveDuplicates();
        }
    }
}
