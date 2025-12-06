package com.spring.mcp.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Spring Boot Initializr API integration.
 *
 * <p>Provides a configured WebClient for making requests to the Initializr API
 * with proper timeouts, headers, and retry behavior.</p>
 *
 * <p>This configuration is only active when the Initializr feature is enabled.</p>
 *
 * @author Spring MCP Server
 * @version 1.4.0
 * @since 2025-12-06
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.initializr", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializrConfig {

    private final InitializrProperties properties;

    /**
     * Create a WebClient configured specifically for Initializr API calls.
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Connection timeout from configuration</li>
     *   <li>Read/write timeouts</li>
     *   <li>User-Agent header for API identification</li>
     *   <li>Accept header for JSON responses</li>
     * </ul>
     *
     * @return configured WebClient for Initializr API
     */
    @Bean("initializrWebClient")
    public WebClient initializrWebClient() {
        int timeoutMs = properties.getApi().getTimeout();

        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
            .responseTimeout(Duration.ofMillis(timeoutMs))
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
            );

        log.info("Initializing Initializr WebClient with base URL: {}, timeout: {}ms",
            properties.getBaseUrl(), timeoutMs);

        return WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.USER_AGENT, properties.getApi().getUserAgent())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Create a RestTemplate for synchronous Initializr API calls.
     *
     * <p>Used primarily by the web controller for file generation
     * and download operations where synchronous behavior is preferred.</p>
     *
     * @param builder the RestTemplateBuilder provided by Spring Boot
     * @return configured RestTemplate for Initializr API
     */
    @Bean("initializrRestTemplate")
    public RestTemplate initializrRestTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofMillis(properties.getApi().getTimeout());

        log.info("Initializing Initializr RestTemplate with timeout: {}ms", timeout.toMillis());

        return builder
            .connectTimeout(timeout)
            .readTimeout(timeout)
            .build();
    }

    /**
     * Log the Initializr configuration on startup.
     */
    @Bean
    public InitializrConfigLogger initializrConfigLogger() {
        return new InitializrConfigLogger(properties);
    }

    /**
     * Simple logger to output Initializr configuration at startup.
     */
    @Slf4j
    @RequiredArgsConstructor
    public static class InitializrConfigLogger {
        private final InitializrProperties properties;

        @jakarta.annotation.PostConstruct
        public void logConfiguration() {
            log.info("=== Spring Boot Initializr Integration ===");
            log.info("  Enabled: {}", properties.isEnabled());
            log.info("  Base URL: {}", properties.getBaseUrl());
            log.info("  Cache Enabled: {}", properties.getCache().isEnabled());
            log.info("  Default Boot Version: {}", properties.getDefaults().getBootVersion());
            log.info("  Default Java Version: {}", properties.getDefaults().getJavaVersion());
            log.info("  Default Build Type: {}", properties.getDefaults().getBuildType());
            log.info("==========================================");
        }
    }
}
