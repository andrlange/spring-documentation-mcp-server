package com.example.library.notifications.api;

import com.example.library.notifications.internal.Notification;
import com.example.library.notifications.internal.Notification.NotificationStatus;
import com.example.library.notifications.internal.Notification.NotificationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for Notification management with API Versioning.
 * Demonstrates Spring Framework 7's first-class API versioning support.
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Management", description = "APIs for managing notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ==================== VERSION 1.0 APIs ====================

    @GetMapping(path = "/member/{memberId}", version = "1.0")
    @Operation(summary = "Get member notifications (v1.0)", description = "Returns basic notification list")
    @ApiResponse(responseCode = "200", description = "List of notifications")
    public List<NotificationResponseV1> getMemberNotificationsV1(
            @Parameter(description = "Member ID") @PathVariable Long memberId) {
        return notificationService.findByMemberId(memberId).stream()
            .map(this::toNotificationResponseV1)
            .toList();
    }

    @GetMapping(path = "/{id}", version = "1.0")
    @Operation(summary = "Get notification by ID (v1.0)", description = "Returns notification details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification found",
            content = @Content(schema = @Schema(implementation = NotificationResponseV1.class))),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponseV1> getNotificationByIdV1(
            @Parameter(description = "Notification ID") @PathVariable Long id) {
        return notificationService.findById(id)
            .map(n -> ResponseEntity.ok(toNotificationResponseV1(n)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/{id}/read", version = "1.0")
    @Operation(summary = "Mark notification as read (v1.0)", description = "Marks a notification as read")
    @ApiResponse(responseCode = "204", description = "Notification marked as read")
    public ResponseEntity<Void> markAsReadV1(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== VERSION 2.0 APIs (Extended) ====================

    @GetMapping(path = "/member/{memberId}", version = "2.0+")
    @Operation(summary = "Get member notifications (v2.0)", description = "Returns paginated notification list with full details")
    @ApiResponse(responseCode = "200", description = "Paginated list of notifications")
    public Page<NotificationResponseV2> getMemberNotificationsV2(
            @PathVariable Long memberId,
            Pageable pageable) {
        return notificationService.getMemberNotifications(memberId, pageable)
            .map(this::toNotificationResponseV2);
    }

    @GetMapping(path = "/member/{memberId}/unread", version = "2.0+")
    @Operation(summary = "Get unread notifications (v2.0)", description = "Returns unread notifications for a member")
    public List<NotificationResponseV2> getUnreadNotificationsV2(@PathVariable Long memberId) {
        return notificationService.getUnreadNotifications(memberId).stream()
            .map(this::toNotificationResponseV2)
            .toList();
    }

    @GetMapping(path = "/member/{memberId}/count", version = "2.0+")
    @Operation(summary = "Count unread notifications (v2.0)", description = "Returns count of unread notifications")
    public UnreadCountResponse countUnreadV2(@PathVariable Long memberId) {
        return new UnreadCountResponse(notificationService.countUnread(memberId));
    }

    @GetMapping(path = "/{id}", version = "2.0+")
    @Operation(summary = "Get notification by ID (v2.0)", description = "Returns extended notification details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification found",
            content = @Content(schema = @Schema(implementation = NotificationResponseV2.class))),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponseV2> getNotificationByIdV2(@PathVariable Long id) {
        return notificationService.findById(id)
            .map(n -> ResponseEntity.ok(toNotificationResponseV2(n)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(version = "2.0+")
    @Operation(summary = "Create notification (v2.0)", description = "Creates a custom notification")
    @ApiResponse(responseCode = "201", description = "Notification created successfully")
    public ResponseEntity<NotificationResponseV2> createNotificationV2(
            @Valid @RequestBody CreateNotificationRequestV2 request) {
        Notification notification = notificationService.createNotification(
            new NotificationService.CreateNotificationRequest(
                request.memberId(),
                request.type(),
                request.subject(),
                request.message(),
                request.referenceType(),
                request.referenceId()
            )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toNotificationResponseV2(notification));
    }

    @PostMapping(path = "/{id}/read", version = "2.0+")
    @Operation(summary = "Mark notification as read (v2.0)", description = "Marks a notification as read and returns updated notification")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    public ResponseEntity<NotificationResponseV2> markAsReadV2(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return notificationService.findById(id)
            .map(n -> ResponseEntity.ok(toNotificationResponseV2(n)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/member/{memberId}/read-all", version = "2.0+")
    @Operation(summary = "Mark all as read (v2.0)", description = "Marks all member notifications as read")
    @ApiResponse(responseCode = "204", description = "All notifications marked as read")
    public ResponseEntity<Void> markAllAsReadV2(@PathVariable Long memberId) {
        notificationService.markAllAsRead(memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/send", version = "2.0+")
    @Operation(summary = "Send notification (v2.0)", description = "Sends a pending notification")
    @ApiResponse(responseCode = "204", description = "Notification sent")
    public ResponseEntity<Void> sendNotificationV2(@PathVariable Long id) {
        notificationService.sendNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path = "/{id}", version = "2.0+")
    @Operation(summary = "Delete notification (v2.0)", description = "Deletes a notification")
    @ApiResponse(responseCode = "204", description = "Notification deleted")
    public ResponseEntity<Void> deleteNotificationV2(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Statistics (v2.0+) ====================

    @GetMapping(path = "/statistics", version = "2.0+")
    @Operation(summary = "Get notification statistics (v2.0)", description = "Returns notification statistics")
    public NotificationStatistics getStatisticsV2() {
        return new NotificationStatistics(
            notificationService.countPending(),
            notificationService.countFailed()
        );
    }

    // ==================== DTO Mappers ====================

    private NotificationResponseV1 toNotificationResponseV1(Notification notification) {
        return new NotificationResponseV1(
            notification.getId(),
            notification.getSubject(),
            notification.getMessage(),
            notification.getStatus().name()
        );
    }

    private NotificationResponseV2 toNotificationResponseV2(Notification notification) {
        return new NotificationResponseV2(
            notification.getId(),
            notification.getMemberId(),
            notification.getType().name(),
            notification.getSubject(),
            notification.getMessage(),
            notification.getStatus().name(),
            notification.getSentAt(),
            notification.getReadAt(),
            notification.getReferenceType(),
            notification.getReferenceId(),
            notification.getCreatedAt()
        );
    }

    // ==================== Request/Response DTOs ====================

    // V1 DTOs
    @Schema(description = "Notification response (Version 1.0)")
    public record NotificationResponseV1(
        @Schema(description = "Notification ID", example = "1") Long id,
        @Schema(description = "Subject", example = "Book Loaned") String subject,
        @Schema(description = "Message") String message,
        @Schema(description = "Status", example = "SENT") String status
    ) {}

    // V2 DTOs (Extended)
    @Schema(description = "Notification response (Version 2.0) - Extended with full details")
    public record NotificationResponseV2(
        @Schema(description = "Notification ID", example = "1") Long id,
        @Schema(description = "Member ID", example = "1") Long memberId,
        @Schema(description = "Notification type", example = "LOAN_CONFIRMATION") String type,
        @Schema(description = "Subject", example = "Book Loaned: Clean Code") String subject,
        @Schema(description = "Message") String message,
        @Schema(description = "Status", example = "SENT") String status,
        @Schema(description = "Sent at") LocalDateTime sentAt,
        @Schema(description = "Read at") LocalDateTime readAt,
        @Schema(description = "Reference type", example = "LOAN") String referenceType,
        @Schema(description = "Reference ID", example = "1") Long referenceId,
        @Schema(description = "Created at") LocalDateTime createdAt
    ) {}

    @Schema(description = "Create notification request (Version 2.0)")
    public record CreateNotificationRequestV2(
        @NotNull @Schema(description = "Member ID", example = "1") Long memberId,
        @NotNull @Schema(description = "Notification type") NotificationType type,
        @NotBlank @Schema(description = "Subject", example = "Important Notice") String subject,
        @NotBlank @Schema(description = "Message") String message,
        @Schema(description = "Reference type", example = "LOAN") String referenceType,
        @Schema(description = "Reference ID", example = "1") Long referenceId
    ) {}

    @Schema(description = "Unread count response")
    public record UnreadCountResponse(
        @Schema(description = "Unread count", example = "5") long count
    ) {}

    @Schema(description = "Notification statistics")
    public record NotificationStatistics(
        @Schema(description = "Pending notifications", example = "10") long pending,
        @Schema(description = "Failed notifications", example = "2") long failed
    ) {}
}
