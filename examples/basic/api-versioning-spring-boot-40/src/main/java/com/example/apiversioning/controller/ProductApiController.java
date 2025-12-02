package com.example.apiversioning.controller;

import com.example.apiversioning.dto.ApiResponse;
import com.example.apiversioning.dto.ProductV1;
import com.example.apiversioning.dto.ProductV2;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product API Controller with Native Versioned Endpoints
 *
 * Demonstrates Spring Framework 7's first-class API versioning support
 * using the `version` attribute on @GetMapping annotations.
 *
 * The version is resolved via the "API-Version" header as configured
 * in ApiVersionConfig.
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/api-version.html">
 *      Spring Framework API Versioning Documentation</a>
 */
@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    // ========== VERSION 1.0 ENDPOINTS (DEPRECATED) ==========

    /**
     * Get all products - Version 1.0 (deprecated)
     */
    @GetMapping(version = "1.0")
    public ApiResponse<List<ProductV1>> getAllProductsV1(HttpServletResponse response) {
        addDeprecationHeaders(response);
        List<ProductV1> products = List.of(
            new ProductV1(1L, "Spring Boot in Action", 49.99),
            new ProductV1(2L, "Cloud Native Java", 54.99),
            new ProductV1(3L, "Reactive Spring", 44.99)
        );
        return ApiResponse.of("1.0", products);
    }

    /**
     * Get product by ID - Version 1.0 (deprecated)
     */
    @GetMapping(path = "/{id}", version = "1.0")
    public ApiResponse<ProductV1> getProductV1(@PathVariable Long id, HttpServletResponse response) {
        addDeprecationHeaders(response);
        ProductV1 product = new ProductV1(id, "Spring Boot in Action", 49.99);
        return ApiResponse.of("1.0", product);
    }

    // ========== VERSION 2.0 ENDPOINTS (CURRENT) ==========

    /**
     * Get all products - Version 2.0 (current)
     */
    @GetMapping(version = "2.0")
    public ApiResponse<List<ProductV2>> getAllProductsV2() {
        List<ProductV2> products = List.of(
            ProductV2.sample(),
            new ProductV2(
                2L,
                "Cloud Native Java",
                54.99,
                "Build scalable, resilient, and observable applications",
                "Books",
                List.of("cloud", "kubernetes", "microservices"),
                28,
                java.time.LocalDateTime.of(2024, 6, 20, 14, 0, 0),
                new ProductV2.Rating(4.5, 89)
            ),
            new ProductV2(
                3L,
                "Reactive Spring",
                44.99,
                "Master reactive programming with Spring WebFlux",
                "Books",
                List.of("reactive", "webflux", "r2dbc"),
                15,
                java.time.LocalDateTime.of(2024, 9, 10, 9, 15, 0),
                new ProductV2.Rating(4.7, 67)
            )
        );
        return ApiResponse.of("2.0", products);
    }

    /**
     * Get product by ID - Version 2.0 (current)
     */
    @GetMapping(path = "/{id}", version = "2.0")
    public ApiResponse<ProductV2> getProductV2(@PathVariable Long id) {
        ProductV2 product = ProductV2.sample();
        return ApiResponse.of("2.0", product);
    }

    // ========== DEPRECATION HANDLING ==========

    /**
     * Add RFC 9745 / RFC 8594 compliant deprecation headers
     * for deprecated API versions.
     */
    private void addDeprecationHeaders(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", "Sat, 01 Jun 2025 00:00:00 GMT");
        response.setHeader("Link", "</docs/migration/1.0>; rel=\"deprecation\"");
    }
}
