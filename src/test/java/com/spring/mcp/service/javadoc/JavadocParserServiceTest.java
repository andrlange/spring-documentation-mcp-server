package com.spring.mcp.service.javadoc;

import com.spring.mcp.model.enums.JavadocClassKind;
import com.spring.mcp.service.javadoc.dto.ParsedClassDto;
import com.spring.mcp.service.javadoc.dto.ParsedPackageDto;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JavadocParserService.
 * Tests parsing of Javadoc HTML pages in both Java 8 and Java 11+ formats.
 */
@DisplayName("Javadoc Parser Service Tests")
class JavadocParserServiceTest {

    private JavadocParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new JavadocParserService();
    }

    private String loadSampleHtml(String filename) throws IOException {
        var resource = new ClassPathResource("javadoc-samples/" + filename);
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("Package Summary Parsing Tests")
    class PackageSummaryParsingTests {

        @Test
        @DisplayName("Should parse Java 11+ package-summary.html")
        void shouldParseJava11PackageSummary() throws IOException {
            // Given
            String html = loadSampleHtml("package-summary.html");
            String baseUrl = "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/package-summary.html";

            // When
            ParsedPackageDto result = parserService.parsePackageSummary(html, baseUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.packageName()).isEqualTo("org.springframework.web.client");
            assertThat(result.summary()).isNotBlank();
            assertThat(result.summary()).contains("client-side web support");
            assertThat(result.classes()).isNotEmpty();
            assertThat(result.classes()).hasSizeGreaterThanOrEqualTo(3);
            assertThat(result.sourceUrl()).isEqualTo(baseUrl);
        }

        @Test
        @DisplayName("Should parse Java 8 format package-summary.html")
        void shouldParseJava8PackageSummary() throws IOException {
            // Given
            String html = loadSampleHtml("package-summary-java8.html");
            String baseUrl = "https://docs.spring.io/spring-framework/docs/5.3.x/javadoc-api/org/springframework/jdbc/core/package-summary.html";

            // When
            ParsedPackageDto result = parserService.parsePackageSummary(html, baseUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.packageName()).isEqualTo("org.springframework.jdbc.core");
            assertThat(result.summary()).isNotBlank();
            assertThat(result.classes()).isNotEmpty();
            assertThat(result.classes()).hasSizeGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Should extract class links from package summary")
        void shouldExtractClassLinks() throws IOException {
            // Given
            String html = loadSampleHtml("package-summary.html");
            String baseUrl = "https://docs.spring.io/spring-framework/docs/current/javadoc-api/";

            // When
            ParsedPackageDto result = parserService.parsePackageSummary(html, baseUrl);

            // Then
            assertThat(result.classes())
                .anyMatch(c -> c.contains("RestTemplate"))
                .anyMatch(c -> c.contains("RestClient"));
        }
    }

    @Nested
    @DisplayName("Class Page Parsing Tests")
    class ClassPageParsingTests {

        @Test
        @DisplayName("Should parse class page with methods, fields, and constructors")
        void shouldParseClassPage() throws IOException {
            // Given
            String html = loadSampleHtml("class-page.html");
            String baseUrl = "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html";

            // When
            ParsedClassDto result = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.simpleName()).isEqualTo("RestTemplate");
            assertThat(result.kind()).isEqualTo(JavadocClassKind.CLASS);
            assertThat(result.modifiers()).contains("public");
            assertThat(result.summary()).isNotBlank();
            assertThat(result.sourceUrl()).isEqualTo(baseUrl);
        }

        @Test
        @DisplayName("Should detect deprecated class")
        void shouldDetectDeprecatedClass() throws IOException {
            // Given
            String html = loadSampleHtml("class-page.html");
            String baseUrl = "https://docs.spring.io/test/";

            // When
            ParsedClassDto result = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(result.deprecated()).isTrue();
            assertThat(result.deprecatedMessage()).contains("RestClient");
        }

        @Test
        @DisplayName("Should extract methods from class page")
        void shouldExtractMethods() throws IOException {
            // Given
            String html = loadSampleHtml("class-page.html");
            String baseUrl = "https://docs.spring.io/test/";

            // When
            ParsedClassDto result = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(result.methods()).isNotEmpty();
            assertThat(result.methods())
                .anyMatch(m -> m.name().equals("getForObject"))
                .anyMatch(m -> m.name().equals("delete"))
                .anyMatch(m -> m.name().equals("postForLocation"));
        }

        @Test
        @DisplayName("Should extract fields from class page")
        void shouldExtractFields() throws IOException {
            // Given
            String html = loadSampleHtml("class-page.html");
            String baseUrl = "https://docs.spring.io/test/";

            // When
            ParsedClassDto result = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(result.fields()).isNotEmpty();
            assertThat(result.fields())
                .anyMatch(f -> f.name().equals("logger"));
        }

        @Test
        @DisplayName("Should extract constructors from class page")
        void shouldExtractConstructors() throws IOException {
            // Given
            String html = loadSampleHtml("class-page.html");
            String baseUrl = "https://docs.spring.io/test/";

            // When
            ParsedClassDto result = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(result.constructors()).isNotEmpty();
            assertThat(result.constructors()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should parse interface page")
        void shouldParseInterfacePage() throws IOException {
            // Given
            String html = loadSampleHtml("interface-page.html");
            String baseUrl = "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestClient.html";

            // When
            ParsedClassDto result = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.simpleName()).isEqualTo("RestClient");
            assertThat(result.kind()).isEqualTo(JavadocClassKind.INTERFACE);
            assertThat(result.deprecated()).isFalse();
            assertThat(result.methods()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty HTML gracefully")
        void shouldHandleEmptyHtml() {
            // Given
            String html = "<html><body></body></html>";
            String baseUrl = "https://example.com/test.html";

            // When
            ParsedPackageDto packageResult = parserService.parsePackageSummary(html, baseUrl);
            ParsedClassDto classResult = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(packageResult).isNotNull();
            assertThat(packageResult.classes()).isEmpty();
            assertThat(classResult).isNotNull();
        }

        @Test
        @DisplayName("Should handle malformed HTML")
        void shouldHandleMalformedHtml() {
            // Given - Missing closing tags
            String html = "<html><body><div class='block'>Some content";
            String baseUrl = "https://example.com/test.html";

            // When / Then - Should not throw exception
            ParsedPackageDto result = parserService.parsePackageSummary(html, baseUrl);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should preserve source URL")
        void shouldPreserveSourceUrl() throws IOException {
            // Given
            String html = loadSampleHtml("class-page.html");
            String baseUrl = "https://docs.spring.io/specific/url/RestTemplate.html";

            // When
            ParsedClassDto result = parserService.parseClassPage(html, baseUrl);

            // Then
            assertThat(result.sourceUrl()).isEqualTo(baseUrl);
        }
    }

    @Nested
    @DisplayName("Class Kind Detection Tests")
    class ClassKindDetectionTests {

        @Test
        @DisplayName("Should detect CLASS kind")
        void shouldDetectClassKind() throws IOException {
            String html = loadSampleHtml("class-page.html");
            ParsedClassDto result = parserService.parseClassPage(html, "https://test.com/");
            assertThat(result.kind()).isEqualTo(JavadocClassKind.CLASS);
        }

        @Test
        @DisplayName("Should detect INTERFACE kind")
        void shouldDetectInterfaceKind() throws IOException {
            String html = loadSampleHtml("interface-page.html");
            ParsedClassDto result = parserService.parseClassPage(html, "https://test.com/");
            assertThat(result.kind()).isEqualTo(JavadocClassKind.INTERFACE);
        }
    }
}
