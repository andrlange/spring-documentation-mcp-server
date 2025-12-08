package com.spring.mcp.integration;

import com.spring.mcp.config.JavadocsFeatureConfig;
import com.spring.mcp.config.TestContainersConfig;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.javadoc.*;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for JavadocCrawlService.
 * Uses mocked HTTP fetcher with real parser and storage services.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JavadocCrawlServiceIntegrationTest.MockFetcherConfig.class})
@DisplayName("Javadoc Crawl Service Integration Tests")
@Transactional
class JavadocCrawlServiceIntegrationTest {

    @Autowired
    private JavadocCrawlService crawlService;

    @Autowired
    private JavadocFetcherService mockFetcherService;

    @Autowired
    private JavadocPackageRepository packageRepository;

    @Autowired
    private JavadocClassRepository classRepository;

    @Autowired
    private JavadocStorageService storageService;

    private static final AtomicLong testIdCounter = new AtomicLong(System.currentTimeMillis());
    private String testId;
    private String library;
    private String version;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        testId = String.valueOf(testIdCounter.incrementAndGet());
        library = "test-crawl-" + testId;
        version = "1.0.0";
        baseUrl = "https://test.example.com/api/";

        // Reset mock
        Mockito.reset(mockFetcherService);
    }

    @TestConfiguration
    static class MockFetcherConfig {
        @Bean
        @Primary
        public JavadocFetcherService mockJavadocFetcherService() {
            return Mockito.mock(JavadocFetcherService.class);
        }
    }

    @Test
    @DisplayName("Should return empty result when package list is empty")
    void shouldReturnEmptyResultWhenNoPackages() {
        // Given - empty package list
        when(mockFetcherService.fetchPackageList(anyString()))
                .thenReturn(Mono.just(""));

        // When
        JavadocCrawlService.CrawlResult result = crawlService.crawlJavadoc(baseUrl, library, version);

        // Then
        assertThat(result.packagesProcessed).isZero();
        assertThat(result.classesProcessed).isZero();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors).anyMatch(e -> e.contains("No packages found"));
    }

    @Test
    @DisplayName("Should handle package list fetch failure gracefully")
    void shouldHandlePackageListFetchFailure() {
        // Given - fetch fails
        when(mockFetcherService.fetchPackageList(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Network error")));

        // When
        JavadocCrawlService.CrawlResult result = crawlService.crawlJavadoc(baseUrl, library, version);

        // Then
        assertThat(result.packagesProcessed).isZero();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Should process packages and classes from valid HTML")
    void shouldProcessPackagesAndClasses() throws IOException {
        // Given - mock package list and HTML responses
        String packageList = "org.springframework.web.client";
        String packageSummaryHtml = loadSampleHtml("package-summary.html");
        String classSummaryHtml = loadSampleHtml("class-page.html");

        when(mockFetcherService.fetchPackageList(anyString()))
                .thenReturn(Mono.just(packageList));
        when(mockFetcherService.fetchPageBlocking(contains("package-summary.html")))
                .thenReturn(Optional.of(packageSummaryHtml));
        when(mockFetcherService.fetchPageBlocking(contains("RestTemplate.html")))
                .thenReturn(Optional.of(classSummaryHtml));
        when(mockFetcherService.fetchPageBlocking(contains("RestClient.html")))
                .thenReturn(Optional.empty()); // Not found
        when(mockFetcherService.fetchPageBlocking(contains("HttpClientErrorException.html")))
                .thenReturn(Optional.empty()); // Not found

        // When
        JavadocCrawlService.CrawlResult result = crawlService.crawlJavadoc(baseUrl, library, version);

        // Then
        assertThat(result.packagesProcessed).isEqualTo(1);
        assertThat(result.classesProcessed).isGreaterThanOrEqualTo(1);
        assertThat(result.isSuccessful()).isTrue();

        // Verify data was stored
        assertThat(storageService.existsForVersion(library, version)).isTrue();
    }

    @Test
    @DisplayName("Should handle missing package summary gracefully")
    void shouldHandleMissingPackageSummary() {
        // Given
        String packageList = "com.missing.package";

        when(mockFetcherService.fetchPackageList(anyString()))
                .thenReturn(Mono.just(packageList));
        when(mockFetcherService.fetchPageBlocking(contains("package-summary.html")))
                .thenReturn(Optional.empty());

        // When
        JavadocCrawlService.CrawlResult result = crawlService.crawlJavadoc(baseUrl, library, version);

        // Then
        assertThat(result.totalPackages).isEqualTo(1);
        assertThat(result.packagesProcessed).isZero();
    }

    @Test
    @DisplayName("Should normalize URL with trailing slash")
    void shouldNormalizeUrl() throws IOException {
        // Given - URL without trailing slash
        String urlWithoutSlash = "https://test.example.com/api";
        String packageList = "com.test.pkg";
        String packageSummaryHtml = loadSampleHtml("package-summary.html");

        when(mockFetcherService.fetchPackageList(eq(urlWithoutSlash + "/")))
                .thenReturn(Mono.just(packageList));
        when(mockFetcherService.fetchPageBlocking(anyString()))
                .thenReturn(Optional.of(packageSummaryHtml));

        // When
        crawlService.crawlJavadoc(urlWithoutSlash, library, version);

        // Then - should have added trailing slash
        verify(mockFetcherService).fetchPackageList(eq(urlWithoutSlash + "/"));
    }

    @Test
    @DisplayName("Should skip module declarations in element-list format")
    void shouldSkipModuleDeclarations() throws IOException {
        // Given - element-list format with module declarations
        String packageList = """
                module:spring.web
                org.springframework.web.client
                module:spring.core
                org.springframework.core
                """;
        String packageSummaryHtml = loadSampleHtml("package-summary.html");

        when(mockFetcherService.fetchPackageList(anyString()))
                .thenReturn(Mono.just(packageList));
        when(mockFetcherService.fetchPageBlocking(anyString()))
                .thenReturn(Optional.of(packageSummaryHtml));

        // When
        JavadocCrawlService.CrawlResult result = crawlService.crawlJavadoc(baseUrl, library, version);

        // Then - should have 2 packages (module declarations skipped)
        assertThat(result.totalPackages).isEqualTo(2);
    }

    @Test
    @DisplayName("CrawlResult should provide correct summary")
    void crawlResultShouldProvideSummary() {
        // Given
        when(mockFetcherService.fetchPackageList(anyString()))
                .thenReturn(Mono.just(""));

        // When
        JavadocCrawlService.CrawlResult result = crawlService.crawlJavadoc(baseUrl, library, version);

        // Then
        assertThat(result.libraryName).isEqualTo(library);
        assertThat(result.version).isEqualTo(version);
        assertThat(result.toString()).contains(library);
        assertThat(result.toString()).contains(version);
    }

    private String loadSampleHtml(String filename) throws IOException {
        var resource = new ClassPathResource("javadoc-samples/" + filename);
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }
}
