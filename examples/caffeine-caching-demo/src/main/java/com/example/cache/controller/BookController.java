package com.example.cache.controller;

import com.example.cache.model.Book;
import com.example.cache.service.BookService;
import com.example.cache.service.CacheStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Web controller for the book search and cache demo UI.
 */
@Controller
public class BookController {

    private final BookService bookService;
    private final CacheStatsService cacheStatsService;

    public BookController(BookService bookService, CacheStatsService cacheStatsService) {
        this.bookService = bookService;
        this.cacheStatsService = cacheStatsService;
    }

    /**
     * Main page - displays book search and cache statistics.
     *
     * @param query Optional search query
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping("/")
    public String index(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Boolean cleared,
            Model model
    ) {
        // Cache statistics for the header
        model.addAttribute("cacheStats", cacheStatsService.getAllCacheStats());
        model.addAttribute("aggregatedStats", cacheStatsService.getAggregatedStats());
        model.addAttribute("totalBooks", bookService.getBookCount());

        // Book search with timing
        List<Book> books;
        long startTime = System.currentTimeMillis();

        if (query.isBlank()) {
            // No search - show first 50 books
            books = bookService.findAll().stream().limit(50).toList();
        } else {
            // Search with caching
            books = bookService.searchBooks(query);
        }

        long duration = System.currentTimeMillis() - startTime;

        model.addAttribute("books", books);
        model.addAttribute("query", query);
        model.addAttribute("searchDuration", duration);
        model.addAttribute("resultCount", books.size());
        model.addAttribute("isCached", duration < 10);

        // Cache cleared notification
        if (Boolean.TRUE.equals(cleared)) {
            model.addAttribute("message", "Cache cleared successfully!");
            model.addAttribute("messageType", "success");
        }

        return "books";
    }

    /**
     * Clear all caches and redirect to home.
     *
     * @param redirectAttributes For flash messages
     * @return Redirect to home
     */
    @PostMapping("/cache/clear")
    public String clearCache(RedirectAttributes redirectAttributes) {
        bookService.clearCache();
        return "redirect:/?cleared=true";
    }
}
