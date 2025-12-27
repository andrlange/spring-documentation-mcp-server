package com.example.library.controller;

import com.example.library.model.Book;
import com.example.library.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * REST API controller for book operations.
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public Collection<Book> getAllBooks() {
        return bookService.findAll();
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<Book> getBookByIsbn(@PathVariable String isbn) {
        return bookService.findByIsbn(isbn)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String query) {
        return bookService.search(query);
    }

    @GetMapping("/category/{categoryId}")
    public List<Book> getBooksByCategory(@PathVariable Long categoryId) {
        return bookService.findByCategory(categoryId);
    }

    @GetMapping("/available")
    public List<Book> getAvailableBooks() {
        return bookService.findAvailable();
    }
}
