package com.spring.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used in external API calls.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Configuration
public class WebClientConfig {

    /**
     * Maximum buffer size for WebClient responses (16MB).
     * Large repositories like spring-boot can have tree responses exceeding the default 256KB limit.
     */
    private static final int MAX_BUFFER_SIZE = 16 * 1024 * 1024; // 16MB

    /**
     * Create a WebClient.Builder bean for injection with increased buffer size.
     * This is needed for GitHub API calls that return large responses (e.g., recursive tree for spring-boot).
     *
     * @return WebClient.Builder instance with custom exchange strategies
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(this::configureCodecs)
                .build();

        return WebClient.builder()
                .exchangeStrategies(strategies);
    }

    /**
     * Configure codecs with increased buffer size.
     */
    private void configureCodecs(ClientCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE);
    }
}
