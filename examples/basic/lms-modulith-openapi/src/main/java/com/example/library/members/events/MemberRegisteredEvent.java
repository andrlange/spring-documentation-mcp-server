package com.example.library.members.events;

/**
 * Event published when a new member is registered.
 * Other modules can listen to this event to perform actions.
 */
public record MemberRegisteredEvent(
    Long memberId,
    String email,
    String fullName,
    String membershipType,
    String cardNumber
) {}
