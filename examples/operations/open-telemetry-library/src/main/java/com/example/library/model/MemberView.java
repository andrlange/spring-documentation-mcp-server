package com.example.library.model;

import java.time.LocalDate;

/**
 * View model for displaying members with current loan count.
 */
public record MemberView(
    Long id,
    String name,
    String email,
    MembershipType membershipType,
    LocalDate joinDate,
    boolean active,
    int currentLoans
) {
    /**
     * Create a MemberView from a Member and current loan count.
     */
    public static MemberView from(Member member, int currentLoans) {
        return new MemberView(
            member.id(),
            member.name(),
            member.email(),
            member.membershipType(),
            member.joinDate(),
            member.active(),
            currentLoans
        );
    }
}
