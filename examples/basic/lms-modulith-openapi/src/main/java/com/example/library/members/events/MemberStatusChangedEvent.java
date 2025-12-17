package com.example.library.members.events;

import org.springframework.modulith.events.Externalized;

/**
 * Event published when a member's status changes.
 *
 * <p>Externalized to topic 'library.members.status-changed' for integration
 * with external systems (Kafka, RabbitMQ, etc.) when configured.
 */
@Externalized("library.members.status-changed")
public record MemberStatusChangedEvent(
    Long memberId,
    String email,
    String previousStatus,
    String newStatus
) {}
