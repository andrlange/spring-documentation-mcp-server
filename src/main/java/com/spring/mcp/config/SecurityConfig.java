package com.spring.mcp.config;

import com.spring.mcp.security.ApiKeyAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the application
 * Uses JdbcUserDetailsManager for JDBC-based authentication (modern approach)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final DataSource dataSource;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Add API Key authentication filter before UsernamePasswordAuthenticationFilter
            // This filter processes /mcp/**, /sse, and /message endpoints
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                // Public resources (including vendor assets for login page)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/vendor/**", "/favicon.ico").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                // Spring AI MCP SSE endpoints - permitAll but protected by ApiKeyAuthenticationFilter
                .requestMatchers("/sse", "/message").permitAll()
                .requestMatchers("/mcp/spring/sse", "/mcp/spring/messages").permitAll()

                // Custom MCP endpoints - require API key authentication (handled by ApiKeyAuthenticationFilter)
                .requestMatchers("/mcp/**").authenticated()

                // API endpoints - require authentication
                .requestMatchers("/api/**").authenticated()

                // Admin-only pages
                .requestMatchers("/users/**", "/settings/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Disable HTTP Basic Auth - use form login for web UI and API keys for MCP endpoints
            .httpBasic(basic -> basic.disable())
            .csrf(csrf -> csrf
                // Disable CSRF for MCP endpoints as they use API key authentication
                .ignoringRequestMatchers("/mcp/**", "/sse", "/message", "/mcp/spring/sse", "/mcp/spring/messages",
                    "/api/mcp/**", "/sync/**", "/settings/api-keys/**")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(10)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use DelegatingPasswordEncoder which supports {bcrypt}, {noop}, etc. prefixes
        return org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * JdbcUserDetailsManager for JDBC-based user authentication
     * Configured with custom SQL queries for our database schema
     */
    @Bean
    public JdbcUserDetailsManager userDetailsManager() {
        var manager = new JdbcUserDetailsManager(dataSource);

        // Custom query for loading user by username
        // Returns: username, password, enabled
        // Only active users (is_active = true) can login
        manager.setUsersByUsernameQuery(
            "SELECT username, password, is_active as enabled FROM users WHERE username = ?"
        );

        // Custom query for loading authorities by username
        // Returns: username, authority
        // Note: Spring Security expects "ROLE_" prefix for roles
        manager.setAuthoritiesByUsernameQuery(
            "SELECT username, CONCAT('ROLE_', role) as authority FROM users WHERE username = ?"
        );

        // Optional: Custom queries for groups (not used in this project)
        // manager.setGroupAuthoritiesByUsernameQuery(...);
        return manager;
    }

    /**
     * CORS configuration for MCP endpoints
     * Allows cross-origin requests from MCP clients (like Claude Code)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins for MCP endpoints (MCP clients can be from any origin)
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Allow all standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

        // Allow necessary headers including API key headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "X-API-Key",
            "Content-Type",
            "Accept",
            "Origin",
            "Cache-Control",
            "X-Requested-With"
        ));

        // Expose headers that clients may need to read
        configuration.setExposedHeaders(Arrays.asList(
            "Content-Type",
            "X-Content-Type-Options"
        ));

        // Allow credentials (for session-based auth on web UI)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply CORS configuration to MCP endpoints
        source.registerCorsConfiguration("/mcp/**", configuration);
        source.registerCorsConfiguration("/sse", configuration);
        source.registerCorsConfiguration("/message", configuration);

        return source;
    }
}
