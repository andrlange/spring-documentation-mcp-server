package com.example.library.members.events;

import org.springframework.modulith.events.Externalized;

/**
 * Event published when a new member is registered.
 * Other modules can listen to this event to perform actions.
 *
 * <p>Externalized to topic 'library.members.member-registered' for integration
 * with external systems (Kafka, RabbitMQ, etc.) when configured.
 */
@Externalized("library.members.member-registered")
public record MemberRegisteredEvent(
    Long memberId,
    String email,
    String fullName,
    String membershipType,
    String cardNumber
) {}
