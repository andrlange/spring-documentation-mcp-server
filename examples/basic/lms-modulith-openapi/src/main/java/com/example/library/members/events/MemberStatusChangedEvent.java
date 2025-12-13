package com.example.library.members.events;

/**
 * Event published when a member's status changes.
 */
public record MemberStatusChangedEvent(
    Long memberId,
    String email,
    String previousStatus,
    String newStatus
) {}
