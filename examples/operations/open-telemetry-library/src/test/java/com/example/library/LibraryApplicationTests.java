package com.example.library;

import com.example.library.data.DataInitializer;
import com.example.library.model.Book;
import com.example.library.model.Member;
import com.example.library.service.BookService;
import com.example.library.service.CategoryService;
import com.example.library.service.LoanService;
import com.example.library.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Library Application.
 * Tests data initialization, services, and observability components.
 */
@SpringBootTest
class LibraryApplicationTests {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private BookService bookService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void contextLoads() {
        // Verify application context loads successfully
        assertThat(dataInitializer).isNotNull();
        assertThat(bookService).isNotNull();
        assertThat(memberService).isNotNull();
        assertThat(loanService).isNotNull();
        assertThat(categoryService).isNotNull();
    }

    @Test
    void dataInitializerCreatesExpectedData() {
        // Verify 200 books are created
        assertThat(dataInitializer.getBooks()).hasSize(200);

        // Verify 10 categories are created
        assertThat(dataInitializer.getCategories()).hasSize(10);

        // Verify 20 members are created
        assertThat(dataInitializer.getMembers()).hasSize(20);

        // Verify some loans exist
        assertThat(dataInitializer.getLoans()).isNotEmpty();
    }

    @Test
    void bookServiceFindsAllBooks() {
        var books = bookService.findAll();
        assertThat(books).hasSize(200);
    }

    @Test
    void bookServiceSearchWorks() {
        // Search for common words that should appear in our generated book titles
        var results = bookService.search("the");
        assertThat(results).isNotNull();
        // Search should work even if no results (no exception)
        var emptyResults = bookService.search("xyznonexistent");
        assertThat(emptyResults).isEmpty();
    }

    @Test
    void bookServiceFindsAvailableBooks() {
        var available = bookService.findAvailable();
        assertThat(available).isNotEmpty();
        available.forEach(book -> assertThat(book.isAvailable()).isTrue());
    }

    @Test
    void memberServiceFindsAllMembers() {
        var members = memberService.findAll();
        assertThat(members).hasSize(20);
    }

    @Test
    void memberServiceFindsActiveMembers() {
        var activeMembers = memberService.findActive();
        assertThat(activeMembers).isNotEmpty();
        activeMembers.forEach(member -> assertThat(member.active()).isTrue());
    }

    @Test
    void categoryServiceFindsAllCategories() {
        var categories = categoryService.findAll();
        assertThat(categories).hasSize(10);
    }

    @Test
    void loanServiceCreatesAndReturnsLoans() {
        // Find an available book and active member
        Book book = bookService.findAvailable().get(0);
        Member member = memberService.findActive().get(0);

        int initialAvailableCopies = book.availableCopies();
        long initialActiveLoans = loanService.countActive();

        // Create a loan
        var loan = loanService.createLoan(book.isbn(), member.id());
        assertThat(loan).isNotNull();
        assertThat(loan.bookIsbn()).isEqualTo(book.isbn());
        assertThat(loan.memberId()).isEqualTo(member.id());
        assertThat(loan.isActive()).isTrue();

        // Verify book availability decreased
        var updatedBook = bookService.findByIsbn(book.isbn()).orElseThrow();
        assertThat(updatedBook.availableCopies()).isEqualTo(initialAvailableCopies - 1);

        // Verify active loans increased
        assertThat(loanService.countActive()).isEqualTo(initialActiveLoans + 1);

        // Return the loan
        var returnedLoan = loanService.returnLoan(loan.id());
        assertThat(returnedLoan.isActive()).isFalse();
        assertThat(returnedLoan.returnDate()).isNotNull();

        // Verify book availability restored
        var restoredBook = bookService.findByIsbn(book.isbn()).orElseThrow();
        assertThat(restoredBook.availableCopies()).isEqualTo(initialAvailableCopies);
    }

    @Test
    void booksHaveValidCategories() {
        var books = bookService.findAll();
        var categories = categoryService.findAll();
        var categoryIds = categories.stream().map(c -> c.id()).toList();

        books.forEach(book ->
            assertThat(categoryIds).contains(book.categoryId())
        );
    }

    @Test
    void membersHaveValidMembershipTypes() {
        var members = memberService.findAll();
        members.forEach(member -> {
            assertThat(member.membershipType()).isNotNull();
            assertThat(member.membershipType().getLoanDays()).isGreaterThan(0);
        });
    }
}
