package com.spring.mcp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.mcp.config.TestContainersConfig;
import com.spring.mcp.config.TestDataBootstrapConfig;
import com.spring.mcp.config.TestFlavorSearchVectorConfig;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for MCP Streamable-HTTP integration tests.
 * Uses Spring AI MCP Client with Streamable-HTTP transport (MCP Protocol 2025-11-25).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestContainersConfig.class, TestFlavorSearchVectorConfig.class, TestDataBootstrapConfig.class})
public abstract class McpSseIntegrationTestBase {

    private static final Logger log = LoggerFactory.getLogger(McpSseIntegrationTestBase.class);

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    protected McpSyncClient mcpClient;
    protected String baseUrl;

    protected static final String API_KEY = TestDataBootstrapConfig.TEST_API_KEY;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port;

        // Create WebClient.Builder with API key authentication
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", API_KEY);

        // Create Streamable-HTTP transport using WebFlux (MCP Protocol 2025-11-25)
        WebClientStreamableHttpTransport transport = WebClientStreamableHttpTransport.builder(webClientBuilder)
                .endpoint("/mcp/spring")
                .build();

        // Create and initialize the MCP client
        mcpClient = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

        // Initialize the connection
        McpSchema.InitializeResult initResult = mcpClient.initialize();
        log.info("MCP client initialized: server={}, version={}",
                initResult.serverInfo().name(),
                initResult.serverInfo().version());
    }

    @AfterEach
    void tearDown() {
        if (mcpClient != null) {
            try {
                mcpClient.close();
            } catch (Exception e) {
                log.warn("Error closing MCP client", e);
            }
        }
    }

    /**
     * List all available MCP tools.
     */
    protected List<McpSchema.Tool> listTools() {
        McpSchema.ListToolsResult result = mcpClient.listTools();
        return result.tools();
    }

    /**
     * Call an MCP tool and return the result.
     */
    protected McpSchema.CallToolResult callTool(String toolName, Map<String, Object> arguments) {
        return mcpClient.callTool(new McpSchema.CallToolRequest(toolName, arguments));
    }

    /**
     * Call an MCP tool and extract the text content from the result.
     */
    protected String callToolAndGetTextContent(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = callTool(toolName, arguments);

        if (result.content() != null && !result.content().isEmpty()) {
            McpSchema.Content content = result.content().get(0);
            if (content instanceof McpSchema.TextContent textContent) {
                return textContent.text();
            }
        }

        return "";
    }

    /**
     * Call an MCP tool and parse the JSON result.
     */
    protected JsonNode callToolAndGetJson(String toolName, Map<String, Object> arguments) throws Exception {
        String text = callToolAndGetTextContent(toolName, arguments);
        if (text != null && !text.isEmpty()) {
            return objectMapper.readTree(text);
        }
        return objectMapper.createObjectNode();
    }

    /**
     * Assert that a specific tool is available.
     */
    protected void assertToolAvailable(String toolName) {
        List<McpSchema.Tool> tools = listTools();
        boolean found = tools.stream()
                .anyMatch(tool -> toolName.equals(tool.name()));
        assertThat(found)
                .as("Tool '%s' should be available", toolName)
                .isTrue();
    }

    /**
     * Assert that the text content contains expected value.
     */
    protected void assertContentContains(String toolName, Map<String, Object> arguments, String expected) {
        String content = callToolAndGetTextContent(toolName, arguments);
        assertThat(content)
                .as("Tool '%s' content should contain '%s'", toolName, expected)
                .contains(expected);
    }

    /**
     * Get tool names as a list of strings.
     */
    protected List<String> getToolNames() {
        return listTools().stream()
                .map(McpSchema.Tool::name)
                .toList();
    }
}
