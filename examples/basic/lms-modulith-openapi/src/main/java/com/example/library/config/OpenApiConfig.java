package com.example.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger Configuration for springdoc-openapi 3.0
 * Demonstrates module-based API grouping and versioned documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Library Management System API")
                .description("""
                    REST API for the Library Management System demonstrating:
                    - Spring Boot 4.0
                    - Spring Modulith 2.0
                    - Spring Framework 7 API Versioning
                    - OpenAPI 3.1 Documentation

                    **API Versioning:**
                    Use the `API-Version` header to specify the API version.
                    - Version 1.0: Basic API (default)
                    - Version 2.0: Extended API with additional fields

                    **Modules:**
                    - Catalog: Book, Author, Category management
                    - Members: Member registration and profiles
                    - Loans: Book loans and returns
                    - Notifications: Alerts and reminders
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Library API Support")
                    .email("support@library.example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development Server")
            ));
    }

    @Bean
    public GroupedOpenApi catalogApi() {
        return GroupedOpenApi.builder()
            .group("catalog-api")
            .displayName("Catalog Module")
            .pathsToMatch("/api/books/**", "/api/authors/**", "/api/categories/**")
            .build();
    }

    @Bean
    public GroupedOpenApi membersApi() {
        return GroupedOpenApi.builder()
            .group("members-api")
            .displayName("Members Module")
            .pathsToMatch("/api/members/**")
            .build();
    }

    @Bean
    public GroupedOpenApi loansApi() {
        return GroupedOpenApi.builder()
            .group("loans-api")
            .displayName("Loans Module")
            .pathsToMatch("/api/loans/**")
            .build();
    }

    @Bean
    public GroupedOpenApi notificationsApi() {
        return GroupedOpenApi.builder()
            .group("notifications-api")
            .displayName("Notifications Module")
            .pathsToMatch("/api/notifications/**")
            .build();
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
            .group("all-apis")
            .displayName("All APIs")
            .pathsToMatch("/api/**")
            .build();
    }
}
