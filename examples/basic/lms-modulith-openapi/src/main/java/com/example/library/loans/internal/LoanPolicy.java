package com.example.library.loans.internal;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_policies")
public class LoanPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "membership_type", nullable = false, unique = true, length = 20)
    private String membershipType;

    @Column(name = "max_books", nullable = false)
    private Integer maxBooks;

    @Column(name = "loan_duration_days", nullable = false)
    private Integer loanDurationDays;

    @Column(name = "max_renewals", nullable = false)
    private Integer maxRenewals;

    @Column(name = "renewal_duration_days", nullable = false)
    private Integer renewalDurationDays;

    @Column(name = "daily_fine_rate", nullable = false)
    private Double dailyFineRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LoanPolicy() {}

    public LoanPolicy(String membershipType, Integer maxBooks, Integer loanDurationDays,
                      Integer maxRenewals, Integer renewalDurationDays, Double dailyFineRate) {
        this.membershipType = membershipType;
        this.maxBooks = maxBooks;
        this.loanDurationDays = loanDurationDays;
        this.maxRenewals = maxRenewals;
        this.renewalDurationDays = renewalDurationDays;
        this.dailyFineRate = dailyFineRate;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getMembershipType() { return membershipType; }
    public Integer getMaxBooks() { return maxBooks; }
    public Integer getLoanDurationDays() { return loanDurationDays; }
    public Integer getMaxRenewals() { return maxRenewals; }
    public Integer getRenewalDurationDays() { return renewalDurationDays; }
    public Double getDailyFineRate() { return dailyFineRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setMaxBooks(Integer maxBooks) { this.maxBooks = maxBooks; }
    public void setLoanDurationDays(Integer loanDurationDays) { this.loanDurationDays = loanDurationDays; }
    public void setMaxRenewals(Integer maxRenewals) { this.maxRenewals = maxRenewals; }
    public void setRenewalDurationDays(Integer renewalDurationDays) { this.renewalDurationDays = renewalDurationDays; }
    public void setDailyFineRate(Double dailyFineRate) { this.dailyFineRate = dailyFineRate; }
}
