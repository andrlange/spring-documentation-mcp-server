package com.example.api.controller;

import com.example.api.model.Book;
import com.example.api.model.CreateBookRequest;
import com.example.api.model.UpdateBookRequest;
import com.example.api.service.BookService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Book API.
 *
 * Endpoints:
 * - GET    /api/books          - List all books
 * - GET    /api/books/{id}     - Get book by ID
 * - GET    /api/books/search   - Search books
 * - POST   /api/books          - Create new book
 * - PUT    /api/books/{id}     - Update existing book
 * - DELETE /api/books/{id}     - Delete book
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * GET /api/books - List all books
     */
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        log.debug("GET /api/books - Fetching all books");
        List<Book> books = bookService.findAll();
        return ResponseEntity.ok(books);
    }

    /**
     * GET /api/books/{id} - Get book by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable String id) {
        log.debug("GET /api/books/{} - Fetching book by ID", id);
        return bookService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/books/search - Search books
     * Query params: title, author
     */
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author) {
        log.debug("GET /api/books/search - title={}, author={}", title, author);

        List<Book> results;
        if (title != null && !title.isBlank()) {
            results = bookService.searchByTitle(title);
        } else if (author != null && !author.isBlank()) {
            results = bookService.searchByAuthor(author);
        } else {
            results = bookService.findAll();
        }

        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/books - Create new book
     */
    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody CreateBookRequest request) {
        log.debug("POST /api/books - Creating book: {}", request.title());
        Book created = bookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/books/{id} - Update existing book
     */
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookRequest request) {
        log.debug("PUT /api/books/{} - Updating book", id);
        return bookService.update(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/books/{id} - Delete book
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable String id) {
        log.debug("DELETE /api/books/{} - Deleting book", id);
        if (bookService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/books/stats - Get statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "totalBooks", bookService.count(),
            "springBootVersion", "4.0.0",
            "springFrameworkVersion", "7.0.x",
            "javaVersion", Runtime.version().toString()
        ));
    }
}
