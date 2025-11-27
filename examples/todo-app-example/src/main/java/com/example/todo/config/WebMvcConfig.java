package com.example.todo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice to add common model attributes to all views.
 *
 * Note: In Thymeleaf 3.1+, #request is no longer available by default.
 * This advice exposes the request URI as a model attribute for navigation highlighting.
 */
@ControllerAdvice
public class WebMvcConfig {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
