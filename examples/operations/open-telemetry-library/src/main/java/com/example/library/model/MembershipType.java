package com.example.library.model;

/**
 * Represents the type of membership a library member has.
 */
public enum MembershipType {
    STANDARD(14, 5),   // 14 days loan period, max 5 books
    PREMIUM(30, 10);   // 30 days loan period, max 10 books

    private final int loanDays;
    private final int maxLoans;

    MembershipType(int loanDays, int maxLoans) {
        this.loanDays = loanDays;
        this.maxLoans = maxLoans;
    }

    public int getLoanDays() {
        return loanDays;
    }

    public int getMaxLoans() {
        return maxLoans;
    }
}
