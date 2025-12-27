package com.example.library.service;

import com.example.library.data.DataInitializer;
import com.example.library.model.Book;
import com.example.library.model.Loan;
import com.example.library.model.Member;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service for loan operations with observability.
 */
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);
    private final DataInitializer dataInitializer;
    private final BookService bookService;
    private final MemberService memberService;

    public LoanService(DataInitializer dataInitializer, BookService bookService, MemberService memberService) {
        this.dataInitializer = dataInitializer;
        this.bookService = bookService;
        this.memberService = memberService;
    }

    @Observed(name = "library.loan.findAll", contextualName = "find-all-loans")
    public Collection<Loan> findAll() {
        log.info("Finding all loans");
        return dataInitializer.getLoans().values();
    }

    @Observed(name = "library.loan.findById", contextualName = "find-loan-by-id")
    public Optional<Loan> findById(Long id) {
        log.info("Finding loan by id: {}", id);
        return Optional.ofNullable(dataInitializer.getLoans().get(id));
    }

    @Observed(name = "library.loan.findActive", contextualName = "find-active-loans")
    public List<Loan> findActive() {
        log.info("Finding active loans");
        return dataInitializer.getLoans().values().stream()
            .filter(Loan::isActive)
            .toList();
    }

    @Observed(name = "library.loan.findOverdue", contextualName = "find-overdue-loans")
    public List<Loan> findOverdue() {
        log.info("Finding overdue loans");
        return dataInitializer.getLoans().values().stream()
            .filter(Loan::isOverdue)
            .toList();
    }

    @Observed(name = "library.loan.findByMember", contextualName = "find-loans-by-member")
    public List<Loan> findByMember(Long memberId) {
        log.info("Finding loans for member: {}", memberId);
        return dataInitializer.getLoans().values().stream()
            .filter(loan -> loan.memberId().equals(memberId))
            .toList();
    }

    @Observed(name = "library.loan.findByBook", contextualName = "find-loans-by-book")
    public List<Loan> findByBook(String bookIsbn) {
        log.info("Finding loans for book: {}", bookIsbn);
        return dataInitializer.getLoans().values().stream()
            .filter(loan -> loan.bookIsbn().equals(bookIsbn))
            .toList();
    }

    @Observed(name = "library.loan.create", contextualName = "create-loan")
    public Loan createLoan(String bookIsbn, Long memberId) {
        log.info("Creating loan for book {} and member {}", bookIsbn, memberId);

        // Validate book exists and is available
        Book book = bookService.findByIsbn(bookIsbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookIsbn));

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is not available: " + bookIsbn);
        }

        // Validate member exists and is active
        Member member = memberService.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        if (!member.active()) {
            throw new IllegalStateException("Member is not active: " + memberId);
        }

        // Create loan
        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = loanDate.plusDays(member.membershipType().getLoanDays());

        Loan loan = new Loan(
            dataInitializer.generateLoanId(),
            bookIsbn,
            memberId,
            loanDate,
            dueDate,
            null
        );

        // Update book availability
        bookService.updateBook(book.withAvailableCopies(book.availableCopies() - 1));

        // Store loan
        dataInitializer.getLoans().put(loan.id(), loan);

        log.info("Created loan {} for book {} and member {}", loan.id(), bookIsbn, memberId);
        return loan;
    }

    @Observed(name = "library.loan.return", contextualName = "return-loan")
    public Loan returnLoan(Long loanId) {
        log.info("Returning loan: {}", loanId);

        Loan loan = findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (!loan.isActive()) {
            throw new IllegalStateException("Loan is already returned: " + loanId);
        }

        // Update loan with return date
        Loan returnedLoan = loan.withReturnDate(LocalDate.now());
        dataInitializer.getLoans().put(loanId, returnedLoan);

        // Update book availability
        bookService.findByIsbn(loan.bookIsbn()).ifPresent(book ->
            bookService.updateBook(book.withAvailableCopies(book.availableCopies() + 1))
        );

        log.info("Returned loan: {}", loanId);
        return returnedLoan;
    }

    public long count() {
        return dataInitializer.getLoans().size();
    }

    public long countActive() {
        return dataInitializer.getLoans().values().stream()
            .filter(Loan::isActive)
            .count();
    }

    public long countOverdue() {
        return dataInitializer.getLoans().values().stream()
            .filter(Loan::isOverdue)
            .count();
    }

    public List<Loan> findRecent(int limit) {
        return dataInitializer.getLoans().values().stream()
            .sorted((a, b) -> b.loanDate().compareTo(a.loanDate()))
            .limit(limit)
            .toList();
    }
}
