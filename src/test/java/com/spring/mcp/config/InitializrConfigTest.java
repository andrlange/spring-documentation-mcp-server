package com.spring.mcp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Initializr configuration classes.
 * Tests property loading, URL generation, and bean creation.
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@DisplayName("Initializr Configuration Tests")
class InitializrConfigTest {

    @Nested
    @DisplayName("InitializrProperties Tests")
    class InitializrPropertiesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            InitializrProperties properties = new InitializrProperties();

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getBaseUrl()).isEqualTo("https://start.spring.io");
            assertThat(properties.getCache().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have correct default project settings")
        void shouldHaveCorrectDefaultProjectSettings() {
            InitializrProperties properties = new InitializrProperties();
            var defaults = properties.getDefaults();

            assertThat(defaults.getBootVersion()).isEqualTo("3.5.9");
            assertThat(defaults.getJavaVersion()).isEqualTo("25");
            assertThat(defaults.getLanguage()).isEqualTo("java");
            assertThat(defaults.getPackaging()).isEqualTo("jar");
            assertThat(defaults.getBuildType()).isEqualTo("gradle-project");
            assertThat(defaults.getGroupId()).isEqualTo("com.example");
            assertThat(defaults.getArtifactId()).isEqualTo("demo");
        }

        @Test
        @DisplayName("Should have correct default API settings")
        void shouldHaveCorrectDefaultApiSettings() {
            InitializrProperties properties = new InitializrProperties();
            var api = properties.getApi();

            assertThat(api.getTimeout()).isEqualTo(30000);
            assertThat(api.getMaxRetries()).isEqualTo(3);
            assertThat(api.getRetryDelay()).isEqualTo(1000);
            assertThat(api.getUserAgent()).isEqualTo("Spring-MCP-Server/1.4.0");
        }

        @Test
        @DisplayName("Should generate correct metadata URL")
        void shouldGenerateCorrectMetadataUrl() {
            InitializrProperties properties = new InitializrProperties();

            assertThat(properties.getMetadataUrl())
                .isEqualTo("https://start.spring.io/metadata/client");
        }

        @Test
        @DisplayName("Should generate correct dependencies URL without version")
        void shouldGenerateCorrectDependenciesUrlWithoutVersion() {
            InitializrProperties properties = new InitializrProperties();

            assertThat(properties.getDependenciesUrl(null))
                .isEqualTo("https://start.spring.io/dependencies");
            assertThat(properties.getDependenciesUrl(""))
                .isEqualTo("https://start.spring.io/dependencies");
            assertThat(properties.getDependenciesUrl("   "))
                .isEqualTo("https://start.spring.io/dependencies");
        }

        @Test
        @DisplayName("Should generate correct dependencies URL with version")
        void shouldGenerateCorrectDependenciesUrlWithVersion() {
            InitializrProperties properties = new InitializrProperties();

            assertThat(properties.getDependenciesUrl("3.5.8"))
                .isEqualTo("https://start.spring.io/dependencies?bootVersion=3.5.8");
            assertThat(properties.getDependenciesUrl("4.0.0"))
                .isEqualTo("https://start.spring.io/dependencies?bootVersion=4.0.0");
        }

        @Test
        @DisplayName("Should generate correct pom.xml URL")
        void shouldGenerateCorrectPomUrl() {
            InitializrProperties properties = new InitializrProperties();

            assertThat(properties.getPomUrl())
                .isEqualTo("https://start.spring.io/pom.xml");
        }

        @Test
        @DisplayName("Should generate correct build.gradle URL")
        void shouldGenerateCorrectBuildGradleUrl() {
            InitializrProperties properties = new InitializrProperties();

            assertThat(properties.getBuildGradleUrl())
                .isEqualTo("https://start.spring.io/build.gradle");
        }

        @Test
        @DisplayName("Should generate correct starter.zip URL")
        void shouldGenerateCorrectStarterZipUrl() {
            InitializrProperties properties = new InitializrProperties();

            assertThat(properties.getStarterZipUrl())
                .isEqualTo("https://start.spring.io/starter.zip");
        }

        @Test
        @DisplayName("Should support custom base URL")
        void shouldSupportCustomBaseUrl() {
            InitializrProperties properties = new InitializrProperties();
            properties.setBaseUrl("https://initializr.example.com");

            assertThat(properties.getMetadataUrl())
                .isEqualTo("https://initializr.example.com/metadata/client");
            assertThat(properties.getPomUrl())
                .isEqualTo("https://initializr.example.com/pom.xml");
        }
    }

    @Nested
    @DisplayName("InitializrConfig Tests")
    class InitializrConfigTests {

        @Test
        @DisplayName("Should create WebClient with properties")
        void shouldCreateWebClientWithProperties() {
            InitializrProperties properties = new InitializrProperties();
            InitializrConfig config = new InitializrConfig(properties);

            WebClient webClient = config.initializrWebClient();

            assertThat(webClient).isNotNull();
        }

        @Test
        @DisplayName("Should create WebClient with custom timeout")
        void shouldCreateWebClientWithCustomTimeout() {
            InitializrProperties properties = new InitializrProperties();
            properties.getApi().setTimeout(60000); // 60 seconds
            InitializrConfig config = new InitializrConfig(properties);

            WebClient webClient = config.initializrWebClient();

            assertThat(webClient).isNotNull();
        }

        @Test
        @DisplayName("Should create WebClient with custom base URL")
        void shouldCreateWebClientWithCustomBaseUrl() {
            InitializrProperties properties = new InitializrProperties();
            properties.setBaseUrl("https://custom-initializr.example.com");
            InitializrConfig config = new InitializrConfig(properties);

            WebClient webClient = config.initializrWebClient();

            assertThat(webClient).isNotNull();
        }

        @Test
        @DisplayName("Should create config logger")
        void shouldCreateConfigLogger() {
            InitializrProperties properties = new InitializrProperties();
            InitializrConfig config = new InitializrConfig(properties);

            var logger = config.initializrConfigLogger();

            assertThat(logger).isNotNull();
        }
    }

    @Nested
    @DisplayName("Property Modification Tests")
    class PropertyModificationTests {

        @Test
        @DisplayName("Should allow enabling/disabling feature")
        void shouldAllowEnablingDisablingFeature() {
            InitializrProperties properties = new InitializrProperties();

            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();

            properties.setEnabled(true);
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should allow modifying cache settings")
        void shouldAllowModifyingCacheSettings() {
            InitializrProperties properties = new InitializrProperties();

            properties.getCache().setEnabled(false);
            assertThat(properties.getCache().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should allow modifying default values")
        void shouldAllowModifyingDefaultValues() {
            InitializrProperties properties = new InitializrProperties();
            var defaults = properties.getDefaults();

            defaults.setBootVersion("4.0.0");
            defaults.setJavaVersion("25");
            defaults.setLanguage("kotlin");
            defaults.setBuildType("maven-project");

            assertThat(defaults.getBootVersion()).isEqualTo("4.0.0");
            assertThat(defaults.getJavaVersion()).isEqualTo("25");
            assertThat(defaults.getLanguage()).isEqualTo("kotlin");
            assertThat(defaults.getBuildType()).isEqualTo("maven-project");
        }

        @Test
        @DisplayName("Should allow modifying API settings")
        void shouldAllowModifyingApiSettings() {
            InitializrProperties properties = new InitializrProperties();
            var api = properties.getApi();

            api.setTimeout(60000);
            api.setMaxRetries(5);
            api.setUserAgent("Custom-Agent/2.0");

            assertThat(api.getTimeout()).isEqualTo(60000);
            assertThat(api.getMaxRetries()).isEqualTo(5);
            assertThat(api.getUserAgent()).isEqualTo("Custom-Agent/2.0");
        }
    }
}
