package com.spring.mcp.service;

import com.spring.mcp.model.entity.McpTool;
import com.spring.mcp.model.enums.McpToolGroup;
import com.spring.mcp.repository.McpToolRepository;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.util.ReflectionTestUtils;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for McpToolMasqueradingService.
 * Tests dynamic MCP tool visibility and description management.
 *
 * <p>The service now supports runtime tool modifications by caching
 * SyncToolSpecification objects at startup and using them for add/remove operations.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolMasqueradingService Tests")
class McpToolMasqueradingServiceTest {

    @Mock
    private McpToolRepository mcpToolRepository;

    @Mock
    private McpSyncServer mcpSyncServer;

    @Mock
    private ContextRefreshedEvent contextRefreshedEvent;

    private McpToolMasqueradingService masqueradingService;

    private McpTool enabledTool;
    private McpTool disabledTool;
    private SyncToolSpecification testToolSpec;

    @BeforeEach
    void setUp() {
        // Create service with constructor injection
        masqueradingService = new McpToolMasqueradingService(mcpToolRepository);
        // Inject the mock server via reflection (since it's @Autowired)
        ReflectionTestUtils.setField(masqueradingService, "mcpSyncServer", mcpSyncServer);

        enabledTool = McpTool.builder()
                .id(1L)
                .toolName("searchSpringDocs")
                .toolGroup(McpToolGroup.DOCUMENTATION)
                .enabled(true)
                .description("Search Spring documentation")
                .originalDescription("Search Spring documentation")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        disabledTool = McpTool.builder()
                .id(2L)
                .toolName("getBreakingChanges")
                .toolGroup(McpToolGroup.MIGRATION)
                .enabled(false)
                .description("Get breaking changes")
                .originalDescription("Get breaking changes")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create a mock tool specification with handler
        Tool tool = Tool.builder()
                .name("searchSpringDocs")
                .description("Original description")
                .build();
        BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> mockHandler = (exchange, request) -> null;
        testToolSpec = SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(mockHandler)
                .build();
    }

    // ==================== Initialize Tests ====================

    @Nested
    @DisplayName("onContextRefreshed")
    class InitializeTests {

        @Test
        @DisplayName("Should set serverAvailable to true when McpSyncServer is present")
        void shouldSetServerAvailableWhenMcpSyncServerPresent() {
            // Given - inject empty tool specs
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));

            // When
            masqueradingService.onContextRefreshed();

            // Then
            assertThat(masqueradingService.isServerAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should set serverAvailable to false when McpSyncServer is null")
        void shouldSetServerAvailableToFalseWhenMcpSyncServerNull() {
            // Given
            McpToolMasqueradingService serviceWithoutServer =
                    new McpToolMasqueradingService(mcpToolRepository);
            // Don't inject the server

            // When
            serviceWithoutServer.onContextRefreshed();

            // Then
            assertThat(serviceWithoutServer.isServerAvailable()).isFalse();
        }

        @Test
        @DisplayName("Should cache tool specifications on context refresh")
        void shouldCacheToolSpecificationsOnContextRefresh() {
            // Given
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));

            // When
            masqueradingService.onContextRefreshed();

            // Then
            assertThat(masqueradingService.getCachedSpecCount()).isEqualTo(1);
            assertThat(masqueradingService.hasToolSpec("searchSpringDocs")).isTrue();
        }

        @Test
        @DisplayName("Should apply initial masquerading for disabled tools")
        void shouldApplyInitialMasqueradingForDisabledTools() {
            // Given
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(List.of(disabledTool));
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));

            // When
            masqueradingService.onContextRefreshed();

            // Then
            verify(mcpSyncServer).removeTool("getBreakingChanges");
            verify(mcpSyncServer).notifyToolsListChanged();
        }
    }

    // ==================== Apply Tool Visibility Tests ====================

    @Nested
    @DisplayName("applyToolVisibility")
    class ApplyToolVisibilityTests {

        @BeforeEach
        void init() {
            // Initialize service with tool specs
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));
            masqueradingService.onContextRefreshed();
        }

        @Test
        @DisplayName("Should remove tool from MCP server when disabling")
        void shouldRemoveToolWhenDisabling() {
            // When
            masqueradingService.applyToolVisibility(disabledTool);

            // Then
            verify(mcpSyncServer).removeTool("getBreakingChanges");
            verify(mcpSyncServer).notifyToolsListChanged();
        }

        @Test
        @DisplayName("Should add tool to MCP server when enabling")
        void shouldAddToolWhenEnabling() {
            // When
            masqueradingService.applyToolVisibility(enabledTool);

            // Then
            verify(mcpSyncServer).addTool(testToolSpec);
            verify(mcpSyncServer).notifyToolsListChanged();
        }

        @Test
        @DisplayName("Should notify clients after visibility change")
        void shouldNotifyClientsAfterVisibilityChange() {
            // When
            masqueradingService.applyToolVisibility(disabledTool);

            // Then
            verify(mcpSyncServer).notifyToolsListChanged();
        }

        @Test
        @DisplayName("Should skip update when server not available")
        void shouldSkipUpdateWhenServerNotAvailable() {
            // Given
            McpToolMasqueradingService serviceWithoutServer =
                    new McpToolMasqueradingService(mcpToolRepository);
            serviceWithoutServer.onContextRefreshed(); // Will set serverAvailable = false

            // When
            serviceWithoutServer.applyToolVisibility(enabledTool);

            // Then - no interaction with mcpSyncServer (it's null anyway)
            verifyNoInteractions(mcpSyncServer);
        }

        @Test
        @DisplayName("Should handle missing tool specification gracefully")
        void shouldHandleMissingToolSpecGracefully() {
            // Given - tool not in cache
            McpTool unknownTool = McpTool.builder()
                    .toolName("unknownTool")
                    .enabled(true)
                    .description("Unknown tool")
                    .originalDescription("Unknown tool")
                    .build();

            // When
            masqueradingService.applyToolVisibility(unknownTool);

            // Then - should not throw, just log warning
            verify(mcpSyncServer, never()).addTool(any());
            verify(mcpSyncServer).notifyToolsListChanged();
        }
    }

    // ==================== Update Tool Description Tests ====================

    @Nested
    @DisplayName("updateToolDescription")
    class UpdateToolDescriptionTests {

        @BeforeEach
        void init() {
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));
            masqueradingService.onContextRefreshed();
        }

        @Test
        @DisplayName("Should update tool description in MCP server")
        void shouldUpdateToolDescriptionInMcpServer() {
            // When
            masqueradingService.updateToolDescription(enabledTool);

            // Then
            verify(mcpSyncServer).removeTool("searchSpringDocs");
            verify(mcpSyncServer).addTool(any(SyncToolSpecification.class));
            verify(mcpSyncServer).notifyToolsListChanged();
        }

        @Test
        @DisplayName("Should skip update for disabled tool")
        void shouldSkipUpdateForDisabledTool() {
            // Given - disabled tool
            McpTool disabled = McpTool.builder()
                    .toolName("searchSpringDocs")
                    .enabled(false)
                    .description("Updated description")
                    .originalDescription("Original description")
                    .build();

            // When
            masqueradingService.updateToolDescription(disabled);

            // Then - no MCP server interactions for description update
            verify(mcpSyncServer, never()).removeTool(anyString());
            verify(mcpSyncServer, never()).addTool(any());
        }

        @Test
        @DisplayName("Should skip update when server not available")
        void shouldSkipUpdateWhenServerNotAvailable() {
            // Given
            McpToolMasqueradingService serviceWithoutServer =
                    new McpToolMasqueradingService(mcpToolRepository);
            serviceWithoutServer.onContextRefreshed();

            // When
            serviceWithoutServer.updateToolDescription(enabledTool);

            // Then
            verifyNoInteractions(mcpSyncServer);
        }

        @Test
        @DisplayName("Should update cache with new specification")
        void shouldUpdateCacheWithNewSpecification() {
            // Given - initial cache has testToolSpec
            assertThat(masqueradingService.hasToolSpec("searchSpringDocs")).isTrue();

            // When
            masqueradingService.updateToolDescription(enabledTool);

            // Then - cache should still have the spec (updated)
            assertThat(masqueradingService.hasToolSpec("searchSpringDocs")).isTrue();
        }
    }

    // ==================== Apply All Masquerading Tests ====================

    @Nested
    @DisplayName("applyAllMasquerading")
    class ApplyAllMasqueradingTests {

        @BeforeEach
        void init() {
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));
            masqueradingService.onContextRefreshed();
            reset(mcpSyncServer); // Reset interactions from initialization
        }

        @Test
        @DisplayName("Should apply masquerading for all tools")
        void shouldApplyMasqueradingForAllTools() {
            // Given
            when(mcpToolRepository.findAllOrdered())
                    .thenReturn(List.of(enabledTool, disabledTool));

            // When
            masqueradingService.applyAllMasquerading();

            // Then
            verify(mcpToolRepository).findAllOrdered();
            verify(mcpSyncServer).addTool(any(SyncToolSpecification.class));
            verify(mcpSyncServer).removeTool("getBreakingChanges");
            verify(mcpSyncServer).notifyToolsListChanged();
        }

        @Test
        @DisplayName("Should skip when server not available")
        void shouldSkipWhenServerNotAvailable() {
            // Given
            McpToolMasqueradingService serviceWithoutServer =
                    new McpToolMasqueradingService(mcpToolRepository);
            serviceWithoutServer.onContextRefreshed();

            // When
            serviceWithoutServer.applyAllMasquerading();

            // Then - should not call repository (server not available)
            verify(mcpToolRepository, never()).findAllOrdered();
        }

        @Test
        @DisplayName("Should notify clients once after all changes")
        void shouldNotifyClientsOnceAfterAllChanges() {
            // Given
            when(mcpToolRepository.findAllOrdered())
                    .thenReturn(List.of(enabledTool, disabledTool));

            // When
            masqueradingService.applyAllMasquerading();

            // Then - only one notification
            verify(mcpSyncServer, times(1)).notifyToolsListChanged();
        }
    }

    // ==================== Tool Count Tests ====================

    @Nested
    @DisplayName("Tool Counts")
    class ToolCountTests {

        @Test
        @DisplayName("Should return enabled tool count")
        void shouldReturnEnabledToolCount() {
            // Given
            when(mcpToolRepository.countByEnabledTrue()).thenReturn(40L);

            // When
            long count = masqueradingService.getEnabledToolCount();

            // Then
            assertThat(count).isEqualTo(40L);
        }

        @Test
        @DisplayName("Should return disabled tool count")
        void shouldReturnDisabledToolCount() {
            // Given
            when(mcpToolRepository.countByEnabledFalse()).thenReturn(4L);

            // When
            long count = masqueradingService.getDisabledToolCount();

            // Then
            assertThat(count).isEqualTo(4L);
        }
    }

    // ==================== Server Available Tests ====================

    @Nested
    @DisplayName("isServerAvailable")
    class ServerAvailableTests {

        @Test
        @DisplayName("Should return false before initialization")
        void shouldReturnFalseBeforeInitialization() {
            // Given
            McpToolMasqueradingService freshService =
                    new McpToolMasqueradingService(mcpToolRepository);

            // Then
            assertThat(freshService.isServerAvailable()).isFalse();
        }

        @Test
        @DisplayName("Should return true after initialization with server")
        void shouldReturnTrueAfterInitializationWithServer() {
            // Given
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));

            // When
            masqueradingService.onContextRefreshed();

            // Then
            assertThat(masqueradingService.isServerAvailable()).isTrue();
        }
    }

    // ==================== Cache Tests ====================

    @Nested
    @DisplayName("Tool Specification Cache")
    class ToolSpecCacheTests {

        @Test
        @DisplayName("Should return correct cached spec count")
        void shouldReturnCorrectCachedSpecCount() {
            // Given
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            Tool tool2 = Tool.builder().name("tool2").description("Tool 2").build();
            BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> handler = (exchange, request) -> null;
            SyncToolSpecification spec2 = SyncToolSpecification.builder()
                    .tool(tool2)
                    .callHandler(handler)
                    .build();

            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec, spec2));

            // When
            masqueradingService.onContextRefreshed();

            // Then
            assertThat(masqueradingService.getCachedSpecCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should correctly identify cached tools")
        void shouldCorrectlyIdentifyCachedTools() {
            // Given
            when(mcpToolRepository.findByEnabledFalse()).thenReturn(Collections.emptyList());
            ReflectionTestUtils.setField(masqueradingService, "toolSpecifications", List.of(testToolSpec));

            // When
            masqueradingService.onContextRefreshed();

            // Then
            assertThat(masqueradingService.hasToolSpec("searchSpringDocs")).isTrue();
            assertThat(masqueradingService.hasToolSpec("nonExistentTool")).isFalse();
        }
    }
}
