package com.example.library.model;

import java.time.LocalDate;

/**
 * Represents a library member.
 */
public record Member(
    Long id,
    String name,
    String email,
    MembershipType membershipType,
    LocalDate joinDate,
    boolean active
) {
    /**
     * Creates a copy of this member with updated active status.
     */
    public Member withActive(boolean newActive) {
        return new Member(id, name, email, membershipType, joinDate, newActive);
    }
}
