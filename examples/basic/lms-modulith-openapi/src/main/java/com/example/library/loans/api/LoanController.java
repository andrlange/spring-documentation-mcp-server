package com.example.library.loans.api;

import com.example.library.loans.internal.Loan;
import com.example.library.loans.internal.LoanPolicy;
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

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Loan management with API Versioning.
 * Demonstrates Spring Framework 7's first-class API versioning support.
 */
@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loan Management", description = "APIs for managing book loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // ==================== VERSION 1.0 APIs ====================

    @GetMapping(version = "1.0")
    @Operation(summary = "List all loans (v1.0)", description = "Returns basic loan information")
    @ApiResponse(responseCode = "200", description = "List of loans")
    public List<LoanResponseV1> getAllLoansV1() {
        return loanService.findAll().stream()
            .map(this::toLoanResponseV1)
            .toList();
    }

    @GetMapping(path = "/{id}", version = "1.0")
    @Operation(summary = "Get loan by ID (v1.0)", description = "Returns basic loan details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found",
            content = @Content(schema = @Schema(implementation = LoanResponseV1.class))),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public ResponseEntity<LoanResponseV1> getLoanByIdV1(
            @Parameter(description = "Loan ID") @PathVariable Long id) {
        return loanService.findById(id)
            .map(loan -> ResponseEntity.ok(toLoanResponseV1(loan)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(version = "1.0")
    @Operation(summary = "Loan a book (v1.0)", description = "Creates a new book loan")
    @ApiResponse(responseCode = "201", description = "Loan created successfully")
    public ResponseEntity<LoanResponseV1> loanBookV1(@Valid @RequestBody LoanBookRequestV1 request) {
        Loan loan = loanService.loanBook(new LoanService.LoanBookRequest(
            request.bookIsbn(),
            request.memberId(),
            null
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toLoanResponseV1(loan));
    }

    @PostMapping(path = "/{id}/return", version = "1.0")
    @Operation(summary = "Return a book (v1.0)", description = "Returns a loaned book")
    @ApiResponse(responseCode = "200", description = "Book returned successfully")
    public ResponseEntity<LoanResponseV1> returnBookV1(@PathVariable Long id) {
        Loan loan = loanService.returnBook(id);
        return ResponseEntity.ok(toLoanResponseV1(loan));
    }

    @PostMapping(path = "/{id}/renew", version = "1.0")
    @Operation(summary = "Renew loan (v1.0)", description = "Extends the loan duration")
    @ApiResponse(responseCode = "200", description = "Loan renewed successfully")
    public ResponseEntity<LoanResponseV1> renewLoanV1(@PathVariable Long id) {
        Loan loan = loanService.renewLoan(id);
        return ResponseEntity.ok(toLoanResponseV1(loan));
    }

    // ==================== VERSION 2.0 APIs (Extended) ====================

    @GetMapping(version = "2.0+")
    @Operation(summary = "List all loans (v2.0)", description = "Returns extended loan information")
    @ApiResponse(responseCode = "200", description = "List of loans with full details")
    public List<LoanResponseV2> getAllLoansV2() {
        return loanService.findAll().stream()
            .map(this::toLoanResponseV2)
            .toList();
    }

    @GetMapping(path = "/{id}", version = "2.0+")
    @Operation(summary = "Get loan by ID (v2.0)", description = "Returns extended loan details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found",
            content = @Content(schema = @Schema(implementation = LoanResponseV2.class))),
        @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    public ResponseEntity<LoanResponseV2> getLoanByIdV2(@PathVariable Long id) {
        return loanService.findById(id)
            .map(loan -> ResponseEntity.ok(toLoanResponseV2(loan)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/active", version = "2.0+")
    @Operation(summary = "List active loans (v2.0)", description = "Returns all currently active loans")
    public List<LoanResponseV2> getActiveLoansV2() {
        return loanService.findActiveLoans().stream()
            .map(this::toLoanResponseV2)
            .toList();
    }

    @GetMapping(path = "/overdue", version = "2.0+")
    @Operation(summary = "List overdue loans (v2.0)", description = "Returns all overdue loans")
    public List<LoanResponseV2> getOverdueLoansV2() {
        return loanService.findOverdueLoans().stream()
            .map(this::toLoanResponseV2)
            .toList();
    }

    @GetMapping(path = "/due-soon", version = "2.0+")
    @Operation(summary = "List loans due soon (v2.0)", description = "Returns loans due within specified days")
    public List<LoanResponseV2> getLoansDueSoonV2(
            @Parameter(description = "Days ahead") @RequestParam(defaultValue = "3") int days) {
        return loanService.findLoansDueSoon(days).stream()
            .map(this::toLoanResponseV2)
            .toList();
    }

    @GetMapping(path = "/member/{memberId}", version = "2.0+")
    @Operation(summary = "Get member's loans (v2.0)", description = "Returns loan history for a member")
    public Page<LoanResponseV2> getMemberLoansV2(
            @PathVariable Long memberId,
            Pageable pageable) {
        return loanService.getMemberLoanHistory(memberId, pageable)
            .map(this::toLoanResponseV2);
    }

    @GetMapping(path = "/book/{isbn}", version = "2.0+")
    @Operation(summary = "Get book's loan history (v2.0)", description = "Returns loan history for a book")
    public List<LoanResponseV2> getBookLoansV2(@PathVariable String isbn) {
        return loanService.findByBookIsbn(isbn).stream()
            .map(this::toLoanResponseV2)
            .toList();
    }

    @GetMapping(path = "/unpaid-fines", version = "2.0+")
    @Operation(summary = "Get loans with unpaid fines (v2.0)", description = "Returns all loans with unpaid fines")
    public List<LoanResponseV2> getLoansWithUnpaidFinesV2() {
        return loanService.getLoansWithUnpaidFines().stream()
            .map(this::toLoanResponseV2)
            .toList();
    }

    @PostMapping(version = "2.0+")
    @Operation(summary = "Loan a book (v2.0)", description = "Creates a new book loan with notes")
    @ApiResponse(responseCode = "201", description = "Loan created successfully")
    public ResponseEntity<LoanResponseV2> loanBookV2(@Valid @RequestBody LoanBookRequestV2 request) {
        Loan loan = loanService.loanBook(new LoanService.LoanBookRequest(
            request.bookIsbn(),
            request.memberId(),
            request.notes()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toLoanResponseV2(loan));
    }

    @PostMapping(path = "/{id}/return", version = "2.0+")
    @Operation(summary = "Return a book (v2.0)", description = "Returns a loaned book with fine details")
    @ApiResponse(responseCode = "200", description = "Book returned successfully")
    public ResponseEntity<LoanResponseV2> returnBookV2(@PathVariable Long id) {
        Loan loan = loanService.returnBook(id);
        return ResponseEntity.ok(toLoanResponseV2(loan));
    }

    @PostMapping(path = "/{id}/renew", version = "2.0+")
    @Operation(summary = "Renew loan (v2.0)", description = "Extends the loan duration with renewal count")
    @ApiResponse(responseCode = "200", description = "Loan renewed successfully")
    public ResponseEntity<LoanResponseV2> renewLoanV2(@PathVariable Long id) {
        Loan loan = loanService.renewLoan(id);
        return ResponseEntity.ok(toLoanResponseV2(loan));
    }

    @PostMapping(path = "/{id}/pay-fine", version = "2.0+")
    @Operation(summary = "Pay fine (v2.0)", description = "Marks fine as paid")
    @ApiResponse(responseCode = "204", description = "Fine paid successfully")
    public ResponseEntity<Void> payFineV2(@PathVariable Long id) {
        loanService.payFine(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/mark-lost", version = "2.0+")
    @Operation(summary = "Mark book as lost (v2.0)", description = "Marks a loaned book as lost")
    @ApiResponse(responseCode = "204", description = "Book marked as lost")
    public ResponseEntity<Void> markAsLostV2(@PathVariable Long id) {
        loanService.markAsLost(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Policy Management (v2.0+) ====================

    @GetMapping(path = "/policies", version = "2.0+")
    @Operation(summary = "List loan policies (v2.0)", description = "Returns all loan policies")
    public List<PolicyResponse> getAllPoliciesV2() {
        return loanService.getAllPolicies().stream()
            .map(this::toPolicyResponse)
            .toList();
    }

    @GetMapping(path = "/policies/{membershipType}", version = "2.0+")
    @Operation(summary = "Get policy by membership type (v2.0)", description = "Returns policy for a membership type")
    public ResponseEntity<PolicyResponse> getPolicyV2(@PathVariable String membershipType) {
        return loanService.getPolicyByMembershipType(membershipType)
            .map(policy -> ResponseEntity.ok(toPolicyResponse(policy)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(path = "/policies", version = "2.0+")
    @Operation(summary = "Create or update policy (v2.0)", description = "Creates or updates a loan policy")
    public ResponseEntity<PolicyResponse> createOrUpdatePolicyV2(
            @Valid @RequestBody CreatePolicyRequest request) {
        LoanPolicy policy = loanService.createOrUpdatePolicy(new LoanService.CreatePolicyRequest(
            request.membershipType(),
            request.maxBooks(),
            request.loanDurationDays(),
            request.maxRenewals(),
            request.renewalDurationDays(),
            request.dailyFineRate()
        ));
        return ResponseEntity.ok(toPolicyResponse(policy));
    }

    // ==================== Statistics (v2.0+) ====================

    @GetMapping(path = "/statistics", version = "2.0+")
    @Operation(summary = "Get loan statistics (v2.0)", description = "Returns loan statistics")
    public LoanStatistics getStatisticsV2() {
        return new LoanStatistics(
            loanService.countActiveLoans(),
            loanService.countOverdueLoans(),
            loanService.getTotalUnpaidFines()
        );
    }

    // ==================== DTO Mappers ====================

    private LoanResponseV1 toLoanResponseV1(Loan loan) {
        return new LoanResponseV1(
            loan.getId(),
            loan.getBookIsbn(),
            loan.getMemberId(),
            loan.getLoanDate(),
            loan.getDueDate(),
            loan.getStatus().name()
        );
    }

    private LoanResponseV2 toLoanResponseV2(Loan loan) {
        return new LoanResponseV2(
            loan.getId(),
            loan.getBookIsbn(),
            loan.getMemberId(),
            loan.getLoanDate(),
            loan.getDueDate(),
            loan.getReturnDate(),
            loan.getStatus().name(),
            loan.getRenewalCount(),
            loan.isOverdue(),
            new FineInfo(
                loan.getFineAmount(),
                loan.getFinePaid()
            ),
            loan.getNotes()
        );
    }

    private PolicyResponse toPolicyResponse(LoanPolicy policy) {
        return new PolicyResponse(
            policy.getId(),
            policy.getMembershipType(),
            policy.getMaxBooks(),
            policy.getLoanDurationDays(),
            policy.getMaxRenewals(),
            policy.getRenewalDurationDays(),
            policy.getDailyFineRate()
        );
    }

    // ==================== Request/Response DTOs ====================

    // V1 DTOs
    @Schema(description = "Loan response (Version 1.0)")
    public record LoanResponseV1(
        @Schema(description = "Loan ID", example = "1") Long id,
        @Schema(description = "Book ISBN", example = "978-0-13-468599-1") String bookIsbn,
        @Schema(description = "Member ID", example = "1") Long memberId,
        @Schema(description = "Loan date") LocalDate loanDate,
        @Schema(description = "Due date") LocalDate dueDate,
        @Schema(description = "Status", example = "ACTIVE") String status
    ) {}

    @Schema(description = "Loan book request (Version 1.0)")
    public record LoanBookRequestV1(
        @NotBlank @Schema(description = "Book ISBN", example = "978-0-13-468599-1") String bookIsbn,
        @NotNull @Schema(description = "Member ID", example = "1") Long memberId
    ) {}

    // V2 DTOs (Extended)
    @Schema(description = "Loan response (Version 2.0) - Extended with full details")
    public record LoanResponseV2(
        @Schema(description = "Loan ID", example = "1") Long id,
        @Schema(description = "Book ISBN", example = "978-0-13-468599-1") String bookIsbn,
        @Schema(description = "Member ID", example = "1") Long memberId,
        @Schema(description = "Loan date") LocalDate loanDate,
        @Schema(description = "Due date") LocalDate dueDate,
        @Schema(description = "Return date") LocalDate returnDate,
        @Schema(description = "Status", example = "ACTIVE") String status,
        @Schema(description = "Renewal count", example = "0") Integer renewalCount,
        @Schema(description = "Is overdue", example = "false") Boolean isOverdue,
        @Schema(description = "Fine information") FineInfo fine,
        @Schema(description = "Notes") String notes
    ) {}

    @Schema(description = "Fine information")
    public record FineInfo(
        @Schema(description = "Fine amount", example = "2.50") Double amount,
        @Schema(description = "Is paid", example = "false") Boolean isPaid
    ) {}

    @Schema(description = "Loan book request (Version 2.0)")
    public record LoanBookRequestV2(
        @NotBlank @Schema(description = "Book ISBN", example = "978-0-13-468599-1") String bookIsbn,
        @NotNull @Schema(description = "Member ID", example = "1") Long memberId,
        @Schema(description = "Notes") String notes
    ) {}

    @Schema(description = "Loan policy response")
    public record PolicyResponse(
        @Schema(description = "Policy ID", example = "1") Long id,
        @Schema(description = "Membership type", example = "PREMIUM") String membershipType,
        @Schema(description = "Max books", example = "10") Integer maxBooks,
        @Schema(description = "Loan duration days", example = "21") Integer loanDurationDays,
        @Schema(description = "Max renewals", example = "3") Integer maxRenewals,
        @Schema(description = "Renewal duration days", example = "14") Integer renewalDurationDays,
        @Schema(description = "Daily fine rate", example = "0.25") Double dailyFineRate
    ) {}

    @Schema(description = "Create/Update policy request")
    public record CreatePolicyRequest(
        @NotBlank @Schema(description = "Membership type", example = "PREMIUM") String membershipType,
        @NotNull @Positive @Schema(description = "Max books", example = "10") Integer maxBooks,
        @NotNull @Positive @Schema(description = "Loan duration days", example = "21") Integer loanDurationDays,
        @NotNull @Positive @Schema(description = "Max renewals", example = "3") Integer maxRenewals,
        @NotNull @Positive @Schema(description = "Renewal duration days", example = "14") Integer renewalDurationDays,
        @NotNull @Positive @Schema(description = "Daily fine rate", example = "0.25") Double dailyFineRate
    ) {}

    @Schema(description = "Loan statistics")
    public record LoanStatistics(
        @Schema(description = "Active loans", example = "42") long activeLoans,
        @Schema(description = "Overdue loans", example = "5") long overdueLoans,
        @Schema(description = "Total unpaid fines", example = "125.50") Double totalUnpaidFines
    ) {}
}
