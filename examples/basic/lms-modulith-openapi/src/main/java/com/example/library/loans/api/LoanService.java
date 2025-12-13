package com.example.library.loans.api;

import com.example.library.catalog.api.BookService;
import com.example.library.loans.events.BookLoanedEvent;
import com.example.library.loans.events.BookReturnedEvent;
import com.example.library.loans.events.LoanOverdueEvent;
import com.example.library.loans.internal.*;
import com.example.library.members.api.MemberService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Public API for the Loans module.
 * This service is exposed to other modules via the Named Interface.
 */
@Service
@Transactional(readOnly = true)
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanPolicyRepository policyRepository;
    private final BookService bookService;
    private final MemberService memberService;
    private final ApplicationEventPublisher eventPublisher;

    public LoanService(LoanRepository loanRepository,
                       LoanPolicyRepository policyRepository,
                       BookService bookService,
                       MemberService memberService,
                       ApplicationEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.policyRepository = policyRepository;
        this.bookService = bookService;
        this.memberService = memberService;
        this.eventPublisher = eventPublisher;
    }

    public List<Loan> findAll() {
        return loanRepository.findAll();
    }

    public Optional<Loan> findById(Long id) {
        return loanRepository.findById(id);
    }

    public List<Loan> findByMemberId(Long memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    public List<Loan> findByBookIsbn(String isbn) {
        return loanRepository.findByBookIsbn(isbn);
    }

    public List<Loan> findActiveLoans() {
        return loanRepository.findByStatus(Loan.LoanStatus.ACTIVE);
    }

    public List<Loan> findOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now());
    }

    public List<Loan> findLoansDueSoon(int daysAhead) {
        return loanRepository.findLoansDueSoon(
            LocalDate.now(),
            LocalDate.now().plusDays(daysAhead)
        );
    }

    public Page<Loan> getMemberLoanHistory(Long memberId, Pageable pageable) {
        return loanRepository.findByMemberIdOrderByLoanDateDesc(memberId, pageable);
    }

    @Transactional
    public Loan loanBook(LoanBookRequest request) {
        // Check if member can borrow
        if (!memberService.canBorrow(request.memberId())) {
            throw new IllegalStateException("Member cannot borrow books (inactive or expired card)");
        }

        // Check if book is available
        var book = bookService.findByIsbn(request.bookIsbn())
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + request.bookIsbn()));

        if (!book.isAvailable()) {
            throw new IllegalStateException("Book is not available for loan: " + request.bookIsbn());
        }

        // Check if book is already loaned to another member
        Optional<Loan> existingLoan = loanRepository.findActiveByBookIsbn(request.bookIsbn());
        if (existingLoan.isPresent()) {
            throw new IllegalStateException("Book is already on loan");
        }

        // Get loan policy for member
        var member = memberService.findById(request.memberId())
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + request.memberId()));

        LoanPolicy policy = policyRepository.findByMembershipType(member.getMembershipType().name())
            .orElseGet(() -> getDefaultPolicy());

        // Check max books limit
        long activeLoans = loanRepository.countActiveByMemberId(request.memberId());
        if (activeLoans >= policy.getMaxBooks()) {
            throw new IllegalStateException("Member has reached maximum loan limit of " + policy.getMaxBooks());
        }

        // Create loan
        LocalDate dueDate = LocalDate.now().plusDays(policy.getLoanDurationDays());
        Loan loan = new Loan(request.bookIsbn(), request.memberId(), dueDate);
        loan.setNotes(request.notes());

        Loan savedLoan = loanRepository.save(loan);

        // Decrement book availability
        bookService.decrementAvailability(request.bookIsbn());

        // Publish event
        eventPublisher.publishEvent(new BookLoanedEvent(
            savedLoan.getId(),
            savedLoan.getBookIsbn(),
            savedLoan.getMemberId(),
            savedLoan.getDueDate()
        ));

        return savedLoan;
    }

    @Transactional
    public Loan returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.isReturned()) {
            throw new IllegalStateException("Book has already been returned");
        }

        // Get policy for fine calculation
        var member = memberService.findById(loan.getMemberId())
            .orElseThrow(() -> new IllegalStateException("Member not found"));

        LoanPolicy policy = policyRepository.findByMembershipType(member.getMembershipType().name())
            .orElseGet(() -> getDefaultPolicy());

        boolean wasOverdue = loan.isOverdue();
        loan.returnBook();
        loan.calculateFine(policy.getDailyFineRate());

        Loan savedLoan = loanRepository.save(loan);

        // Increment book availability
        bookService.incrementAvailability(loan.getBookIsbn());

        // Publish event
        eventPublisher.publishEvent(new BookReturnedEvent(
            savedLoan.getId(),
            savedLoan.getBookIsbn(),
            savedLoan.getMemberId(),
            savedLoan.getReturnDate(),
            wasOverdue,
            savedLoan.getFineAmount()
        ));

        return savedLoan;
    }

    @Transactional
    public Loan renewLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        var member = memberService.findById(loan.getMemberId())
            .orElseThrow(() -> new IllegalStateException("Member not found"));

        LoanPolicy policy = policyRepository.findByMembershipType(member.getMembershipType().name())
            .orElseGet(() -> getDefaultPolicy());

        if (!loan.canRenew(policy.getMaxRenewals())) {
            throw new IllegalStateException("Loan cannot be renewed (max renewals reached or overdue)");
        }

        loan.renew(policy.getRenewalDurationDays());
        return loanRepository.save(loan);
    }

    @Transactional
    public void payFine(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.getFineAmount() <= 0) {
            throw new IllegalStateException("No fine to pay");
        }

        loan.markFinePaid();
        loanRepository.save(loan);
    }

    @Transactional
    public void markAsLost(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.isReturned()) {
            throw new IllegalStateException("Book has already been returned");
        }

        loan.setStatus(Loan.LoanStatus.LOST);
        // Apply maximum fine for lost book
        loan.calculateFine(50.0); // $50 replacement fee
        loanRepository.save(loan);
    }

    /**
     * Scheduled task to check for overdue loans and publish events.
     */
    @Scheduled(cron = "0 0 9 * * ?") // Every day at 9 AM
    @Transactional
    public void checkOverdueLoans() {
        List<Loan> overdueLoans = findOverdueLoans();
        for (Loan loan : overdueLoans) {
            int daysOverdue = (int) (LocalDate.now().toEpochDay() - loan.getDueDate().toEpochDay());
            eventPublisher.publishEvent(new LoanOverdueEvent(
                loan.getId(),
                loan.getBookIsbn(),
                loan.getMemberId(),
                loan.getDueDate(),
                daysOverdue
            ));
        }
    }

    // Policy management
    public List<LoanPolicy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public Optional<LoanPolicy> getPolicyByMembershipType(String membershipType) {
        return policyRepository.findByMembershipType(membershipType);
    }

    @Transactional
    public LoanPolicy createOrUpdatePolicy(CreatePolicyRequest request) {
        LoanPolicy policy = policyRepository.findByMembershipType(request.membershipType())
            .orElse(new LoanPolicy(
                request.membershipType(),
                request.maxBooks(),
                request.loanDurationDays(),
                request.maxRenewals(),
                request.renewalDurationDays(),
                request.dailyFineRate()
            ));

        policy.setMaxBooks(request.maxBooks());
        policy.setLoanDurationDays(request.loanDurationDays());
        policy.setMaxRenewals(request.maxRenewals());
        policy.setRenewalDurationDays(request.renewalDurationDays());
        policy.setDailyFineRate(request.dailyFineRate());

        return policyRepository.save(policy);
    }

    // Statistics
    public long countActiveLoans() {
        return loanRepository.countByStatus(Loan.LoanStatus.ACTIVE);
    }

    public long countOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now()).size();
    }

    public Double getTotalUnpaidFines() {
        Double total = loanRepository.getTotalUnpaidFines();
        return total != null ? total : 0.0;
    }

    public List<Loan> getLoansWithUnpaidFines() {
        return loanRepository.findLoansWithUnpaidFines();
    }

    private LoanPolicy getDefaultPolicy() {
        return new LoanPolicy("STANDARD", 5, 14, 2, 7, 0.50);
    }

    // Request DTOs
    public record LoanBookRequest(
        String bookIsbn,
        Long memberId,
        String notes
    ) {}

    public record CreatePolicyRequest(
        String membershipType,
        Integer maxBooks,
        Integer loanDurationDays,
        Integer maxRenewals,
        Integer renewalDurationDays,
        Double dailyFineRate
    ) {}
}
