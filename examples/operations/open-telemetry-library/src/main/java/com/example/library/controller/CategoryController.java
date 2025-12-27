package com.example.library.controller;

import com.example.library.model.Category;
import com.example.library.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * REST API controller for category operations.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Collection<Category> getAllCategories() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
