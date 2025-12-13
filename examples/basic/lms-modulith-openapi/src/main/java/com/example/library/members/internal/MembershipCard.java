package com.example.library.members.internal;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_cards")
public class MembershipCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "card_number", nullable = false, unique = true, length = 20)
    private String cardNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MembershipCard() {}

    public MembershipCard(Member member, String cardNumber, LocalDate expiryDate) {
        this.member = member;
        this.cardNumber = cardNumber;
        this.issueDate = LocalDate.now();
        this.expiryDate = expiryDate;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (issueDate == null) {
            issueDate = LocalDate.now();
        }
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !isExpired();
    }

    public boolean expiringSoon(int daysAhead) {
        if (isExpired()) return false;
        LocalDate threshold = LocalDate.now().plusDays(daysAhead);
        return expiryDate.isBefore(threshold) || expiryDate.isEqual(threshold);
    }

    // Getters
    public Long getId() { return id; }
    public Member getMember() { return member; }
    public String getCardNumber() { return cardNumber; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
