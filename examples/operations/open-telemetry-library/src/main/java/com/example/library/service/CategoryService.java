package com.example.library.service;

import com.example.library.data.DataInitializer;
import com.example.library.model.Category;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

/**
 * Service for category operations with observability.
 */
@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);
    private final DataInitializer dataInitializer;

    public CategoryService(DataInitializer dataInitializer) {
        this.dataInitializer = dataInitializer;
    }

    @Observed(name = "library.category.findAll", contextualName = "find-all-categories")
    public Collection<Category> findAll() {
        log.info("Finding all categories");
        return dataInitializer.getCategories().values();
    }

    @Observed(name = "library.category.findById", contextualName = "find-category-by-id")
    public Optional<Category> findById(Long id) {
        log.info("Finding category by id: {}", id);
        return Optional.ofNullable(dataInitializer.getCategories().get(id));
    }

    public long count() {
        return dataInitializer.getCategories().size();
    }
}
