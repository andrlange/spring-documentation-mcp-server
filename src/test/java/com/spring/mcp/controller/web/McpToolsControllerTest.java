package com.spring.mcp.controller.web;

import com.spring.mcp.model.dto.McpToolDto;
import com.spring.mcp.model.dto.McpToolGroupDto;
import com.spring.mcp.model.dto.McpToolStatisticsDto;
import com.spring.mcp.model.enums.McpToolGroup;
import com.spring.mcp.service.McpToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for McpToolsController.
 * Tests all endpoints for MCP tool masquerading functionality.
 *
 * @author Spring MCP Server
 * @version 1.6.2
 * @since 2026-01-03
 */
@DisplayName("McpToolsController Tests")
@ExtendWith(MockitoExtension.class)
class McpToolsControllerTest {

    @Mock
    private McpToolService mcpToolService;

    private McpToolsController controller;

    private McpToolDto sampleTool;
    private McpToolGroupDto sampleGroup;
    private McpToolStatisticsDto sampleStats;

    @BeforeEach
    void setUp() {
        controller = new McpToolsController(mcpToolService);

        sampleTool = McpToolDto.builder()
                .id(1L)
                .toolName("searchSpringDocs")
                .toolGroup(McpToolGroup.DOCUMENTATION)
                .enabled(true)
                .description("Search Spring documentation")
                .originalDescription("Search Spring documentation")
                .modified(false)
                .build();

        sampleGroup = McpToolGroupDto.fromEnum(McpToolGroup.DOCUMENTATION);
        sampleGroup.addTool(sampleTool);

        sampleStats = McpToolStatisticsDto.builder()
                .totalTools(44L)
                .enabledTools(40L)
                .disabledTools(4L)
                .modifiedTools(2L)
                .groupCount(7)
                .build();
    }

    // ==================== Index Page Tests ====================

    @Nested
    @DisplayName("GET /mcp-tools")
    class IndexTests {

        @Test
        @DisplayName("Should render index page with all model attributes")
        void shouldRenderIndexPageWithModelAttributes() {
            // Given
            when(mcpToolService.getAllGroupsWithTools())
                    .thenReturn(List.of(sampleGroup));
            when(mcpToolService.getStatistics())
                    .thenReturn(sampleStats);

            Model model = new ConcurrentModel();

            // When
            String viewName = controller.index(model);

            // Then
            assertThat(viewName).isEqualTo("mcp-tools/index");
            assertThat(model.getAttribute("activePage")).isEqualTo("mcp-tools");
            assertThat(model.getAttribute("pageTitle")).isEqualTo("MCP Masquerading");
            assertThat(model.getAttribute("toolGroups")).isEqualTo(List.of(sampleGroup));
            assertThat(model.getAttribute("stats")).isEqualTo(sampleStats);
        }

        @Test
        @DisplayName("Should call service methods once")
        void shouldCallServiceMethodsOnce() {
            // Given
            when(mcpToolService.getAllGroupsWithTools()).thenReturn(List.of());
            when(mcpToolService.getStatistics()).thenReturn(sampleStats);

            Model model = new ConcurrentModel();

            // When
            controller.index(model);

            // Then
            verify(mcpToolService, times(1)).getAllGroupsWithTools();
            verify(mcpToolService, times(1)).getStatistics();
        }
    }

    // ==================== Get Tool Tests ====================

    @Nested
    @DisplayName("GET /mcp-tools/{toolName}")
    class GetToolTests {

        @Test
        @DisplayName("Should return tool when found")
        void shouldReturnToolWhenFound() {
            // Given
            when(mcpToolService.getTool("searchSpringDocs"))
                    .thenReturn(sampleTool);

            // When
            ResponseEntity<McpToolDto> response = controller.getTool("searchSpringDocs");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(sampleTool);
        }

        @Test
        @DisplayName("Should return 404 when tool not found")
        void shouldReturn404WhenToolNotFound() {
            // Given
            when(mcpToolService.getTool("nonExistent"))
                    .thenThrow(new IllegalArgumentException("Tool not found"));

            // When
            ResponseEntity<McpToolDto> response = controller.getTool("nonExistent");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ==================== Toggle Tool Tests ====================

    @Nested
    @DisplayName("POST /mcp-tools/{toolName}/toggle")
    class ToggleToolTests {

        @Test
        @DisplayName("Should toggle tool successfully")
        void shouldToggleToolSuccessfully() {
            // Given
            McpToolDto toggledTool = McpToolDto.builder()
                    .toolName("searchSpringDocs")
                    .enabled(false)
                    .build();

            when(mcpToolService.toggleTool("searchSpringDocs", false))
                    .thenReturn(toggledTool);
            when(mcpToolService.getStatistics())
                    .thenReturn(sampleStats);

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.toggleTool("searchSpringDocs", false);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("toolName")).isEqualTo("searchSpringDocs");
            assertThat(response.getBody().get("enabled")).isEqualTo(false);
            assertThat(response.getBody().get("stats")).isNotNull();
        }

        @Test
        @DisplayName("Should return 400 for invalid tool")
        void shouldReturn400ForInvalidTool() {
            // Given
            when(mcpToolService.toggleTool("nonExistent", true))
                    .thenThrow(new IllegalArgumentException("Tool not found"));

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.toggleTool("nonExistent", true);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("Tool not found");
        }

        @Test
        @DisplayName("Should return 500 for unexpected error")
        void shouldReturn500ForUnexpectedError() {
            // Given
            when(mcpToolService.toggleTool(anyString(), anyBoolean()))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.toggleTool("searchSpringDocs", true);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("Internal server error");
        }
    }

    // ==================== Toggle Group Tests ====================

    @Nested
    @DisplayName("POST /mcp-tools/groups/{group}/toggle")
    class ToggleGroupTests {

        @Test
        @DisplayName("Should toggle group successfully")
        void shouldToggleGroupSuccessfully() {
            // Given
            when(mcpToolService.toggleGroup(McpToolGroup.DOCUMENTATION, false))
                    .thenReturn(10);

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.toggleGroup(McpToolGroup.DOCUMENTATION, false);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("group")).isEqualTo("DOCUMENTATION");
            assertThat(response.getBody().get("enabled")).isEqualTo(false);
            assertThat(response.getBody().get("updatedCount")).isEqualTo(10);
        }

        @Test
        @DisplayName("Should return 500 for unexpected error")
        void shouldReturn500ForUnexpectedError() {
            // Given
            when(mcpToolService.toggleGroup(any(), anyBoolean()))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.toggleGroup(McpToolGroup.MIGRATION, true);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().get("success")).isEqualTo(false);
        }
    }

    // ==================== Update Description Tests ====================

    @Nested
    @DisplayName("POST /mcp-tools/{toolName}/description")
    class UpdateDescriptionTests {

        @Test
        @DisplayName("Should update description successfully")
        void shouldUpdateDescriptionSuccessfully() {
            // Given
            McpToolDto updatedTool = McpToolDto.builder()
                    .toolName("searchSpringDocs")
                    .modified(true)
                    .build();

            when(mcpToolService.updateDescription("searchSpringDocs", "New description"))
                    .thenReturn(updatedTool);

            Map<String, String> request = Map.of("description", "New description");

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.updateDescription("searchSpringDocs", request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("toolName")).isEqualTo("searchSpringDocs");
            assertThat(response.getBody().get("modified")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should return 400 for empty description")
        void shouldReturn400ForEmptyDescription() {
            // Given
            when(mcpToolService.updateDescription("searchSpringDocs", ""))
                    .thenThrow(new IllegalArgumentException("Description cannot be empty"));

            Map<String, String> request = Map.of("description", "");

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.updateDescription("searchSpringDocs", request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("Description cannot be empty");
        }

        @Test
        @DisplayName("Should return 400 for non-existent tool")
        void shouldReturn400ForNonExistentTool() {
            // Given
            when(mcpToolService.updateDescription(eq("nonExistent"), anyString()))
                    .thenThrow(new IllegalArgumentException("Tool not found"));

            Map<String, String> request = Map.of("description", "New description");

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.updateDescription("nonExistent", request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("success")).isEqualTo(false);
        }

        @Test
        @DisplayName("Should return 500 for unexpected error")
        void shouldReturn500ForUnexpectedError() {
            // Given
            when(mcpToolService.updateDescription(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Database error"));

            Map<String, String> request = Map.of("description", "New description");

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.updateDescription("searchSpringDocs", request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("Internal server error");
        }
    }

    // ==================== Reset Description Tests ====================

    @Nested
    @DisplayName("POST /mcp-tools/{toolName}/reset")
    class ResetDescriptionTests {

        @Test
        @DisplayName("Should reset description successfully")
        void shouldResetDescriptionSuccessfully() {
            // Given
            McpToolDto resetTool = McpToolDto.builder()
                    .toolName("searchSpringDocs")
                    .description("Original description")
                    .modified(false)
                    .build();

            when(mcpToolService.resetToOriginal("searchSpringDocs"))
                    .thenReturn(resetTool);

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.resetDescription("searchSpringDocs");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("toolName")).isEqualTo("searchSpringDocs");
            assertThat(response.getBody().get("modified")).isEqualTo(false);
            assertThat(response.getBody().get("description")).isEqualTo("Original description");
        }

        @Test
        @DisplayName("Should return 400 for non-existent tool")
        void shouldReturn400ForNonExistentTool() {
            // Given
            when(mcpToolService.resetToOriginal("nonExistent"))
                    .thenThrow(new IllegalArgumentException("Tool not found"));

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.resetDescription("nonExistent");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("success")).isEqualTo(false);
        }

        @Test
        @DisplayName("Should return 500 for unexpected error")
        void shouldReturn500ForUnexpectedError() {
            // Given
            when(mcpToolService.resetToOriginal(anyString()))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.resetDescription("searchSpringDocs");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().get("success")).isEqualTo(false);
            assertThat(response.getBody().get("error")).isEqualTo("Internal server error");
        }
    }

    // ==================== Is Modified Tests ====================

    @Nested
    @DisplayName("GET /mcp-tools/{toolName}/modified")
    class IsModifiedTests {

        @Test
        @DisplayName("Should return modified status true")
        void shouldReturnModifiedStatusTrue() {
            // Given
            when(mcpToolService.isDescriptionModified("searchSpringDocs"))
                    .thenReturn(true);

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.isModified("searchSpringDocs");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("success")).isEqualTo(true);
            assertThat(response.getBody().get("toolName")).isEqualTo("searchSpringDocs");
            assertThat(response.getBody().get("modified")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should return modified status false")
        void shouldReturnModifiedStatusFalse() {
            // Given
            when(mcpToolService.isDescriptionModified("searchSpringDocs"))
                    .thenReturn(false);

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.isModified("searchSpringDocs");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("modified")).isEqualTo(false);
        }

        @Test
        @DisplayName("Should return 404 for non-existent tool")
        void shouldReturn404ForNonExistentTool() {
            // Given
            when(mcpToolService.isDescriptionModified("nonExistent"))
                    .thenThrow(new IllegalArgumentException("Tool not found"));

            // When
            ResponseEntity<Map<String, Object>> response =
                    controller.isModified("nonExistent");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ==================== Get Modified Tools Tests ====================

    @Nested
    @DisplayName("GET /mcp-tools/modified")
    class GetModifiedToolsTests {

        @Test
        @DisplayName("Should return list of modified tools")
        void shouldReturnListOfModifiedTools() {
            // Given
            McpToolDto modifiedTool1 = McpToolDto.builder()
                    .toolName("tool1")
                    .modified(true)
                    .build();

            McpToolDto modifiedTool2 = McpToolDto.builder()
                    .toolName("tool2")
                    .modified(true)
                    .build();

            when(mcpToolService.getModifiedTools())
                    .thenReturn(List.of(modifiedTool1, modifiedTool2));

            // When
            ResponseEntity<List<McpToolDto>> response = controller.getModifiedTools();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getToolName()).isEqualTo("tool1");
            assertThat(response.getBody().get(1).getToolName()).isEqualTo("tool2");
        }

        @Test
        @DisplayName("Should return empty list when no tools are modified")
        void shouldReturnEmptyListWhenNoToolsAreModified() {
            // Given
            when(mcpToolService.getModifiedTools()).thenReturn(List.of());

            // When
            ResponseEntity<List<McpToolDto>> response = controller.getModifiedTools();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }
}
