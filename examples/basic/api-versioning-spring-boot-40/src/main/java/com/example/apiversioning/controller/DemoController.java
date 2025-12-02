package com.example.apiversioning.controller;

import com.example.apiversioning.dto.ApiResponse;
import com.example.apiversioning.dto.ProductV1;
import com.example.apiversioning.dto.ProductV2;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Demo Web Controller
 *
 * Serves the Thymeleaf UI for demonstrating API versioning.
 */
@Controller
public class DemoController {

    private final ObjectMapper objectMapper;

    public DemoController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index(Model model) throws JsonProcessingException {
        // Prepare sample data for display
        List<ProductV1> productsV1 = List.of(
            new ProductV1(1L, "Spring Boot in Action", 49.99),
            new ProductV1(2L, "Cloud Native Java", 54.99),
            new ProductV1(3L, "Reactive Spring", 44.99)
        );

        List<ProductV2> productsV2 = List.of(
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

        // Create API response wrappers
        ApiResponse<List<ProductV1>> responseV1 = ApiResponse.of("1.0", productsV1);
        ApiResponse<List<ProductV2>> responseV2 = ApiResponse.of("2.0", productsV2);

        // Convert to pretty-printed JSON for display
        model.addAttribute("jsonV1", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseV1));
        model.addAttribute("jsonV2", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseV2));
        model.addAttribute("productsV1", productsV1);
        model.addAttribute("productsV2", productsV2);

        return "index";
    }
}
