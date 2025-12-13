package com.example.library.catalog.api;

import com.example.library.catalog.events.BookAddedEvent;
import com.example.library.catalog.events.BookAvailabilityChangedEvent;
import com.example.library.catalog.events.BookAvailabilityChangedEvent.ChangeType;
import com.example.library.catalog.internal.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Public API for the Catalog module - Book management.
 * This service is exposed to other modules via the Named Interface.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       CategoryRepository categoryRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Book> findAll() {
        return bookRepository.findAllWithDetails();
    }

    public Optional<Book> findByIsbn(String isbn) {
        return bookRepository.findByIsbnWithDetails(isbn);
    }

    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    public Page<Book> searchBooks(String query, Pageable pageable) {
        return bookRepository.searchBooks(query, pageable);
    }

    public List<Book> findAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }

    public List<Book> findByAuthor(Long authorId) {
        return bookRepository.findByAuthorId(authorId);
    }

    public List<Book> findByCategory(Long categoryId) {
        return bookRepository.findByCategoryId(categoryId);
    }

    @Transactional
    public Book createBook(CreateBookRequest request) {
        Author author = authorRepository.findById(request.authorId())
            .orElseThrow(() -> new IllegalArgumentException("Author not found: " + request.authorId()));

        Category category = request.categoryId() != null
            ? categoryRepository.findById(request.categoryId()).orElse(null)
            : null;

        Book book = new Book(
            request.isbn(),
            request.title(),
            author,
            category,
            request.description(),
            request.publicationYear(),
            request.totalCopies()
        );

        Book savedBook = bookRepository.save(book);

        // Publish event
        eventPublisher.publishEvent(new BookAddedEvent(
            savedBook.getIsbn(),
            savedBook.getTitle(),
            author.getName(),
            savedBook.getAvailableCopies()
        ));

        return savedBook;
    }

    @Transactional
    public Book updateBook(String isbn, UpdateBookRequest request) {
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));

        if (request.title() != null) {
            book.setTitle(request.title());
        }
        if (request.description() != null) {
            book.setDescription(request.description());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.categoryId()));
            book.setCategory(category);
        }

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(String isbn) {
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));
        bookRepository.delete(book);
    }

    @Transactional
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    /**
     * Called by Loans module when a book is loaned.
     */
    @Transactional
    public void decrementAvailability(String isbn) {
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));

        int previousAvailable = book.getAvailableCopies();
        book.decrementAvailableCopies();
        bookRepository.save(book);

        eventPublisher.publishEvent(new BookAvailabilityChangedEvent(
            book.getIsbn(),
            book.getTitle(),
            previousAvailable,
            book.getAvailableCopies(),
            ChangeType.LOANED
        ));
    }

    /**
     * Called by Loans module when a book is returned.
     */
    @Transactional
    public void incrementAvailability(String isbn) {
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));

        int previousAvailable = book.getAvailableCopies();
        book.incrementAvailableCopies();
        bookRepository.save(book);

        eventPublisher.publishEvent(new BookAvailabilityChangedEvent(
            book.getIsbn(),
            book.getTitle(),
            previousAvailable,
            book.getAvailableCopies(),
            ChangeType.RETURNED
        ));
    }

    public long countBooks() {
        return bookRepository.count();
    }

    public long countAvailableBooks() {
        return bookRepository.countByAvailableCopiesGreaterThan(0);
    }

    // Author methods
    public List<Author> findAllAuthors() {
        return authorRepository.findAll();
    }

    public Optional<Author> findAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    // Category methods
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // Request DTOs
    public record CreateBookRequest(
        String isbn,
        String title,
        Long authorId,
        Long categoryId,
        String description,
        Integer publicationYear,
        Integer totalCopies
    ) {}

    public record UpdateBookRequest(
        String title,
        Long categoryId,
        String description
    ) {}
}
