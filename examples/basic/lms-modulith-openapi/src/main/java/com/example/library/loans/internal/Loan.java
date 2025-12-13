package com.example.library.loans.internal;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_isbn", nullable = false, length = 20)
    private String bookIsbn;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "renewal_count")
    private Integer renewalCount = 0;

    @Column(name = "fine_amount")
    private Double fineAmount = 0.0;

    @Column(name = "fine_paid")
    private Boolean finePaid = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Loan() {}

    public Loan(String bookIsbn, Long memberId, LocalDate dueDate) {
        this.bookIsbn = bookIsbn;
        this.memberId = memberId;
        this.loanDate = LocalDate.now();
        this.dueDate = dueDate;
        this.status = LoanStatus.ACTIVE;
        this.renewalCount = 0;
        this.fineAmount = 0.0;
        this.finePaid = false;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (loanDate == null) {
            loanDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE && LocalDate.now().isAfter(dueDate);
    }

    public boolean isReturned() {
        return status == LoanStatus.RETURNED;
    }

    public boolean canRenew(int maxRenewals) {
        return status == LoanStatus.ACTIVE &&
               renewalCount < maxRenewals &&
               !isOverdue();
    }

    public void renew(int additionalDays) {
        this.dueDate = this.dueDate.plusDays(additionalDays);
        this.renewalCount++;
    }

    public void returnBook() {
        this.returnDate = LocalDate.now();
        this.status = LoanStatus.RETURNED;
        if (isOverdue()) {
            this.status = LoanStatus.RETURNED_LATE;
        }
    }

    public void calculateFine(double dailyFineRate) {
        if (returnDate != null && returnDate.isAfter(dueDate)) {
            long daysLate = returnDate.toEpochDay() - dueDate.toEpochDay();
            this.fineAmount = daysLate * dailyFineRate;
        } else if (returnDate == null && isOverdue()) {
            long daysLate = LocalDate.now().toEpochDay() - dueDate.toEpochDay();
            this.fineAmount = daysLate * dailyFineRate;
        }
    }

    public void markFinePaid() {
        this.finePaid = true;
    }

    // Getters
    public Long getId() { return id; }
    public String getBookIsbn() { return bookIsbn; }
    public Long getMemberId() { return memberId; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public LoanStatus getStatus() { return status; }
    public Integer getRenewalCount() { return renewalCount; }
    public Double getFineAmount() { return fineAmount; }
    public Boolean getFinePaid() { return finePaid; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public enum LoanStatus {
        ACTIVE, RETURNED, RETURNED_LATE, LOST
    }
}
