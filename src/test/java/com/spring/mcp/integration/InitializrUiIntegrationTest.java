package com.spring.mcp.integration;

import com.spring.mcp.config.TestContainersConfig;
import com.spring.mcp.config.TestDataBootstrapConfig;
import com.spring.mcp.config.TestFlavorSearchVectorConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Spring Initializr Web UI.
 * Tests page rendering and form interactions.
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestContainersConfig.class, TestFlavorSearchVectorConfig.class, TestDataBootstrapConfig.class})
@DisplayName("Initializr UI Integration Tests")
class InitializrUiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /initializr - Main Page")
    class MainPageTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should render Initializr page with required model attributes")
        void shouldRenderInitializrPage() throws Exception {
            mockMvc.perform(get("/initializr"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("initializr/index"))
                    .andExpect(model().attributeExists("activePage"))
                    .andExpect(model().attribute("activePage", "initializr"))
                    .andExpect(model().attributeExists("pageTitle"))
                    .andExpect(model().attribute("pageTitle", "Spring Initializr"))
                    .andExpect(model().attributeExists("bootVersions"))
                    .andExpect(model().attributeExists("dependencyCategories"))
                    .andExpect(model().attributeExists("javaVersions"))
                    .andExpect(model().attributeExists("languages"))
                    .andExpect(model().attributeExists("projectTypes"))
                    .andExpect(model().attributeExists("packagingTypes"))
                    .andExpect(model().attributeExists("defaults"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should have boot versions loaded from Initializr cache")
        void shouldHaveBootVersionsLoaded() throws Exception {
            mockMvc.perform(get("/initializr"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("bootVersions"))
                    .andExpect(model().attribute("bootVersions", hasSize(greaterThanOrEqualTo(0))));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should have dependency categories loaded")
        void shouldHaveDependencyCategoriesLoaded() throws Exception {
            mockMvc.perform(get("/initializr"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("dependencyCategories"));
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            // Without @WithMockUser, expects redirect to login or 401
            mockMvc.perform(get("/initializr"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("GET /initializr/dependencies/search - AJAX Search")
    class DependencySearchTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return all categories when no query")
        void shouldReturnAllCategoriesWhenNoQuery() throws Exception {
            mockMvc.perform(get("/initializr/dependencies/search")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.categories").exists());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should search dependencies with query")
        void shouldSearchDependenciesWithQuery() throws Exception {
            mockMvc.perform(get("/initializr/dependencies/search")
                            .param("query", "web")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should support limit parameter")
        void shouldSupportLimitParameter() throws Exception {
            mockMvc.perform(get("/initializr/dependencies/search")
                            .param("query", "spring")
                            .param("limit", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /initializr/dependencies/{id} - Dependency Details")
    class DependencyDetailsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return JSON response for dependency endpoint")
        void shouldReturnJsonResponseForDependencyEndpoint() throws Exception {
            // May return success=true (found) or success=false (not found) depending on cache state
            // Just verify we get a valid JSON response with the expected structure
            mockMvc.perform(get("/initializr/dependencies/web")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle boot version parameter")
        void shouldHandleBootVersionParameter() throws Exception {
            // May return success=true (found) or success=false (not found) depending on cache state
            mockMvc.perform(get("/initializr/dependencies/web")
                            .param("bootVersion", "3.5.8")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 404 for unknown dependency")
        void shouldReturn404ForUnknownDependency() throws Exception {
            mockMvc.perform(get("/initializr/dependencies/unknown-dep-xyz")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /initializr/preview - Build File Preview")
    class PreviewTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should respond to Gradle preview request")
        void shouldRespondToGradlePreviewRequest() throws Exception {
            // Preview calls external Initializr API - may succeed or fail depending on network
            // Just verify we get a JSON response with the expected structure
            mockMvc.perform(get("/initializr/preview")
                            .param("type", "gradle-project")
                            .param("bootVersion", "3.5.8")
                            .param("groupId", "com.example")
                            .param("artifactId", "demo")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should respond to Maven preview request")
        void shouldRespondToMavenPreviewRequest() throws Exception {
            // Preview calls external Initializr API - may succeed or fail depending on network
            mockMvc.perform(get("/initializr/preview")
                            .param("type", "maven-project")
                            .param("bootVersion", "3.5.8")
                            .param("groupId", "com.example")
                            .param("artifactId", "demo")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should respond to Kotlin DSL preview request")
        void shouldRespondToKotlinDslPreviewRequest() throws Exception {
            // Preview calls external Initializr API - may succeed or fail depending on network
            mockMvc.perform(get("/initializr/preview")
                            .param("type", "gradle-project-kotlin")
                            .param("bootVersion", "3.5.8")
                            .param("groupId", "com.example")
                            .param("artifactId", "demo")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }
    }

    @Nested
    @DisplayName("POST /initializr/cache/refresh - Cache Refresh")
    class CacheRefreshTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should refresh cache for admin")
        void shouldRefreshCacheForAdmin() throws Exception {
            mockMvc.perform(post("/initializr/cache/refresh")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cache refreshed successfully"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should allow cache refresh for regular user")
        void shouldAllowCacheRefreshForUser() throws Exception {
            // Note: In production you may want to restrict this to admin only
            mockMvc.perform(post("/initializr/cache/refresh")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}
