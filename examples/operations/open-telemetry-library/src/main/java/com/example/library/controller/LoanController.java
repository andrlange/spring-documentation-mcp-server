package com.example.library.controller;

import com.example.library.model.Loan;
import com.example.library.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * REST API controller for loan operations.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public Collection<Loan> getAllLoans() {
        return loanService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        return loanService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public List<Loan> getActiveLoans() {
        return loanService.findActive();
    }

    @GetMapping("/overdue")
    public List<Loan> getOverdueLoans() {
        return loanService.findOverdue();
    }

    @GetMapping("/member/{memberId}")
    public List<Loan> getLoansByMember(@PathVariable Long memberId) {
        return loanService.findByMember(memberId);
    }

    @GetMapping("/book/{bookIsbn}")
    public List<Loan> getLoansByBook(@PathVariable String bookIsbn) {
        return loanService.findByBook(bookIsbn);
    }

    @PostMapping
    public ResponseEntity<Loan> createLoan(@RequestBody CreateLoanRequest request) {
        try {
            Loan loan = loanService.createLoan(request.bookIsbn(), request.memberId());
            return ResponseEntity.ok(loan);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<Loan> returnLoan(@PathVariable Long id) {
        try {
            Loan loan = loanService.returnLoan(id);
            return ResponseEntity.ok(loan);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record CreateLoanRequest(String bookIsbn, Long memberId) {}
}
