package com.example.library.catalog.api;

import com.example.library.catalog.internal.Book;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Book management with API Versioning.
 * Demonstrates Spring Framework 7's first-class API versioning support.
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "Book Catalog", description = "APIs for managing the book catalog")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ==================== VERSION 1.0 APIs ====================

    @GetMapping(version = "1.0")
    @Operation(summary = "List all books (v1.0)", description = "Returns basic book information")
    @ApiResponse(responseCode = "200", description = "List of books")
    public List<BookResponseV1> getAllBooksV1() {
        return bookService.findAll().stream()
            .map(this::toBookResponseV1)
            .toList();
    }

    @GetMapping(path = "/{isbn}", version = "1.0")
    @Operation(summary = "Get book by ISBN (v1.0)", description = "Returns basic book details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Book found",
            content = @Content(schema = @Schema(implementation = BookResponseV1.class))),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookResponseV1> getBookByIsbnV1(
            @Parameter(description = "Book ISBN") @PathVariable String isbn) {
        return bookService.findByIsbn(isbn)
            .map(book -> ResponseEntity.ok(toBookResponseV1(book)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(version = "1.0")
    @Operation(summary = "Add new book (v1.0)", description = "Creates a new book in the catalog")
    @ApiResponse(responseCode = "201", description = "Book created successfully")
    public ResponseEntity<BookResponseV1> createBookV1(@Valid @RequestBody CreateBookRequestV1 request) {
        Book book = bookService.createBook(new BookService.CreateBookRequest(
            request.isbn(),
            request.title(),
            request.authorId(),
            request.categoryId(),
            request.description(),
            request.publicationYear(),
            request.totalCopies() != null ? request.totalCopies() : 1
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toBookResponseV1(book));
    }

    @PutMapping(path = "/{isbn}", version = "1.0")
    @Operation(summary = "Update book (v1.0)", description = "Updates book details")
    public ResponseEntity<BookResponseV1> updateBookV1(
            @PathVariable String isbn,
            @Valid @RequestBody UpdateBookRequestV1 request) {
        Book book = bookService.updateBook(isbn, new BookService.UpdateBookRequest(
            request.title(),
            request.categoryId(),
            request.description()
        ));
        return ResponseEntity.ok(toBookResponseV1(book));
    }

    @DeleteMapping(path = "/{isbn}", version = "1.0")
    @Operation(summary = "Delete book (v1.0)", description = "Removes a book from the catalog")
    @ApiResponse(responseCode = "204", description = "Book deleted successfully")
    public ResponseEntity<Void> deleteBookV1(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
        return ResponseEntity.noContent().build();
    }

    // ==================== VERSION 2.0 APIs (Extended) ====================

    @GetMapping(version = "2.0+")
    @Operation(summary = "List all books (v2.0)", description = "Returns extended book information with author details")
    @ApiResponse(responseCode = "200", description = "List of books with full details")
    public List<BookResponseV2> getAllBooksV2() {
        return bookService.findAll().stream()
            .map(this::toBookResponseV2)
            .toList();
    }

    @GetMapping(path = "/{isbn}", version = "2.0+")
    @Operation(summary = "Get book by ISBN (v2.0)", description = "Returns extended book details with author and availability")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Book found",
            content = @Content(schema = @Schema(implementation = BookResponseV2.class))),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookResponseV2> getBookByIsbnV2(@PathVariable String isbn) {
        return bookService.findByIsbn(isbn)
            .map(book -> ResponseEntity.ok(toBookResponseV2(book)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/search", version = "2.0+")
    @Operation(summary = "Search books (v2.0)", description = "Search books by title or author name")
    public Page<BookResponseV2> searchBooksV2(
            @Parameter(description = "Search query") @RequestParam String query,
            Pageable pageable) {
        return bookService.searchBooks(query, pageable)
            .map(this::toBookResponseV2);
    }

    @GetMapping(path = "/available", version = "2.0+")
    @Operation(summary = "List available books (v2.0)", description = "Returns only books with available copies")
    public List<BookResponseV2> getAvailableBooksV2() {
        return bookService.findAvailableBooks().stream()
            .map(this::toBookResponseV2)
            .toList();
    }

    @PostMapping(version = "2.0+")
    @Operation(summary = "Add new book (v2.0)", description = "Creates a new book with extended fields")
    @ApiResponse(responseCode = "201", description = "Book created successfully")
    public ResponseEntity<BookResponseV2> createBookV2(@Valid @RequestBody CreateBookRequestV2 request) {
        Book book = bookService.createBook(new BookService.CreateBookRequest(
            request.isbn(),
            request.title(),
            request.authorId(),
            request.categoryId(),
            request.description(),
            request.publicationYear(),
            request.totalCopies()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toBookResponseV2(book));
    }

    @PutMapping(path = "/{isbn}", version = "2.0+")
    @Operation(summary = "Update book (v2.0)", description = "Updates book with extended fields")
    public ResponseEntity<BookResponseV2> updateBookV2(
            @PathVariable String isbn,
            @Valid @RequestBody UpdateBookRequestV2 request) {
        Book book = bookService.updateBook(isbn, new BookService.UpdateBookRequest(
            request.title(),
            request.categoryId(),
            request.description()
        ));
        return ResponseEntity.ok(toBookResponseV2(book));
    }

    @DeleteMapping(path = "/{isbn}", version = "2.0+")
    @Operation(summary = "Delete book (v2.0)", description = "Removes a book from the catalog")
    @ApiResponse(responseCode = "204", description = "Book deleted successfully")
    public ResponseEntity<Void> deleteBookV2(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
        return ResponseEntity.noContent().build();
    }

    // ==================== DTO Mappers ====================

    private BookResponseV1 toBookResponseV1(Book book) {
        return new BookResponseV1(
            book.getIsbn(),
            book.getTitle(),
            book.getAuthor().getName(),
            book.getAvailableCopies()
        );
    }

    private BookResponseV2 toBookResponseV2(Book book) {
        return new BookResponseV2(
            book.getIsbn(),
            book.getTitle(),
            new AuthorInfo(
                book.getAuthor().getId(),
                book.getAuthor().getName(),
                book.getAuthor().getBirthYear()
            ),
            book.getCategory() != null
                ? new CategoryInfo(book.getCategory().getId(), book.getCategory().getName())
                : null,
            book.getDescription(),
            book.getPublicationYear(),
            new AvailabilityInfo(
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.isAvailable()
            )
        );
    }

    // ==================== Request/Response DTOs ====================

    // V1 DTOs
    @Schema(description = "Book response (Version 1.0)")
    public record BookResponseV1(
        @Schema(description = "ISBN", example = "978-0-13-468599-1") String isbn,
        @Schema(description = "Book title", example = "Clean Code") String title,
        @Schema(description = "Author name", example = "Robert C. Martin") String authorName,
        @Schema(description = "Available copies", example = "5") Integer availableCopies
    ) {}

    @Schema(description = "Create book request (Version 1.0)")
    public record CreateBookRequestV1(
        @NotBlank @Schema(description = "ISBN", example = "978-0-13-468599-1") String isbn,
        @NotBlank @Schema(description = "Book title", example = "Clean Code") String title,
        @NotNull @Schema(description = "Author ID", example = "1") Long authorId,
        @Schema(description = "Category ID", example = "4") Long categoryId,
        @Schema(description = "Book description") String description,
        @Schema(description = "Publication year", example = "2008") Integer publicationYear,
        @Positive @Schema(description = "Total copies", example = "5") Integer totalCopies
    ) {}

    @Schema(description = "Update book request (Version 1.0)")
    public record UpdateBookRequestV1(
        @Schema(description = "Book title") String title,
        @Schema(description = "Category ID") Long categoryId,
        @Schema(description = "Book description") String description
    ) {}

    // V2 DTOs (Extended)
    @Schema(description = "Book response (Version 2.0) - Extended with full details")
    public record BookResponseV2(
        @Schema(description = "ISBN", example = "978-0-13-468599-1") String isbn,
        @Schema(description = "Book title", example = "Clean Code") String title,
        @Schema(description = "Author details") AuthorInfo author,
        @Schema(description = "Category details") CategoryInfo category,
        @Schema(description = "Book description") String description,
        @Schema(description = "Publication year", example = "2008") Integer publicationYear,
        @Schema(description = "Availability information") AvailabilityInfo availability
    ) {}

    @Schema(description = "Author information")
    public record AuthorInfo(
        @Schema(description = "Author ID", example = "1") Long id,
        @Schema(description = "Author name", example = "Robert C. Martin") String name,
        @Schema(description = "Birth year", example = "1952") Integer birthYear
    ) {}

    @Schema(description = "Category information")
    public record CategoryInfo(
        @Schema(description = "Category ID", example = "4") Long id,
        @Schema(description = "Category name", example = "Technology") String name
    ) {}

    @Schema(description = "Book availability information")
    public record AvailabilityInfo(
        @Schema(description = "Total copies", example = "5") Integer totalCopies,
        @Schema(description = "Available copies", example = "3") Integer availableCopies,
        @Schema(description = "Is available", example = "true") Boolean isAvailable
    ) {}

    @Schema(description = "Create book request (Version 2.0)")
    public record CreateBookRequestV2(
        @NotBlank @Schema(description = "ISBN", example = "978-0-13-468599-1") String isbn,
        @NotBlank @Schema(description = "Book title", example = "Clean Code") String title,
        @NotNull @Schema(description = "Author ID", example = "1") Long authorId,
        @Schema(description = "Category ID", example = "4") Long categoryId,
        @Schema(description = "Book description") String description,
        @Schema(description = "Publication year", example = "2008") Integer publicationYear,
        @NotNull @Positive @Schema(description = "Total copies", example = "5") Integer totalCopies
    ) {}

    @Schema(description = "Update book request (Version 2.0)")
    public record UpdateBookRequestV2(
        @Schema(description = "Book title") String title,
        @Schema(description = "Category ID") Long categoryId,
        @Schema(description = "Book description") String description
    ) {}
}
