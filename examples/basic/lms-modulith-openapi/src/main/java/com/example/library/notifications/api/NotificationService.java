package com.example.library.notifications.api;

import com.example.library.notifications.internal.Notification;
import com.example.library.notifications.internal.Notification.NotificationStatus;
import com.example.library.notifications.internal.Notification.NotificationType;
import com.example.library.notifications.internal.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Public API for the Notifications module.
 * This service is exposed to other modules via the Named Interface.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    public List<Notification> findByMemberId(Long memberId) {
        return notificationRepository.findByMemberId(memberId);
    }

    public Page<Notification> getMemberNotifications(Long memberId, Pageable pageable) {
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    public List<Notification> getUnreadNotifications(Long memberId) {
        return notificationRepository.findUnreadByMemberId(memberId);
    }

    public long countUnread(Long memberId) {
        return notificationRepository.countByMemberIdAndStatus(memberId, NotificationStatus.PENDING) +
               notificationRepository.countByMemberIdAndStatus(memberId, NotificationStatus.SENT);
    }

    @Transactional
    public Notification createNotification(CreateNotificationRequest request) {
        Notification notification = new Notification(
            request.memberId(),
            request.type(),
            request.subject(),
            request.message()
        );

        if (request.referenceType() != null && request.referenceId() != null) {
            notification.setReference(request.referenceType(), request.referenceId());
        }

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification: {} for member {}", request.type(), request.memberId());
        return saved;
    }

    @Async
    @Transactional
    public void sendNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        try {
            // In a real app, this would send email/SMS/push notification
            // For demo, we just mark it as sent
            log.info("Sending notification {} to member {}: {}",
                notification.getType(), notification.getMemberId(), notification.getSubject());

            notification.markAsSent();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send notification {}", notificationId, e);
            notification.markAsFailed();
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        List<Notification> unread = notificationRepository.findUnreadByMemberId(memberId);
        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    // Convenience methods for creating specific notification types

    @Transactional
    public Notification sendLoanConfirmation(Long memberId, String bookTitle, String dueDate) {
        Notification notification = createNotification(new CreateNotificationRequest(
            memberId,
            NotificationType.LOAN_CONFIRMATION,
            "Book Loaned: " + bookTitle,
            "You have successfully borrowed \"" + bookTitle + "\". Please return it by " + dueDate + ".",
            "LOAN",
            null
        ));
        sendNotification(notification.getId());
        return notification;
    }

    @Transactional
    public Notification sendReturnConfirmation(Long memberId, String bookTitle, Double fine) {
        String message = "You have successfully returned \"" + bookTitle + "\".";
        if (fine != null && fine > 0) {
            message += " A late fee of $" + String.format("%.2f", fine) + " has been applied.";
        }

        Notification notification = createNotification(new CreateNotificationRequest(
            memberId,
            NotificationType.RETURN_CONFIRMATION,
            "Book Returned: " + bookTitle,
            message,
            "LOAN",
            null
        ));
        sendNotification(notification.getId());
        return notification;
    }

    @Transactional
    public Notification sendDueSoonReminder(Long memberId, Long loanId, String bookTitle, String dueDate) {
        Notification notification = createNotification(new CreateNotificationRequest(
            memberId,
            NotificationType.DUE_SOON_REMINDER,
            "Reminder: Book Due Soon",
            "Your loan of \"" + bookTitle + "\" is due on " + dueDate + ". Please return it on time to avoid late fees.",
            "LOAN",
            loanId
        ));
        sendNotification(notification.getId());
        return notification;
    }

    @Transactional
    public Notification sendOverdueNotice(Long memberId, Long loanId, String bookTitle, int daysOverdue) {
        Notification notification = createNotification(new CreateNotificationRequest(
            memberId,
            NotificationType.OVERDUE_NOTICE,
            "OVERDUE: " + bookTitle,
            "Your loan of \"" + bookTitle + "\" is " + daysOverdue + " day(s) overdue. " +
            "Please return it as soon as possible. Late fees are being applied daily.",
            "LOAN",
            loanId
        ));
        sendNotification(notification.getId());
        return notification;
    }

    @Transactional
    public Notification sendCardExpiringReminder(Long memberId, String cardNumber, String expiryDate) {
        Notification notification = createNotification(new CreateNotificationRequest(
            memberId,
            NotificationType.CARD_EXPIRING,
            "Membership Card Expiring Soon",
            "Your membership card (" + cardNumber + ") will expire on " + expiryDate + ". " +
            "Please visit the library to renew your membership.",
            "CARD",
            null
        ));
        sendNotification(notification.getId());
        return notification;
    }

    @Transactional
    public Notification sendWelcomeNotification(Long memberId, String memberName, String cardNumber) {
        Notification notification = createNotification(new CreateNotificationRequest(
            memberId,
            NotificationType.WELCOME,
            "Welcome to the Library!",
            "Dear " + memberName + ", welcome to our library! Your membership card number is " + cardNumber + ". " +
            "You can now borrow books and access all library services.",
            "MEMBER",
            memberId
        ));
        sendNotification(notification.getId());
        return notification;
    }

    // Statistics
    public long countPending() {
        return notificationRepository.countByStatus(NotificationStatus.PENDING);
    }

    public long countFailed() {
        return notificationRepository.countByStatus(NotificationStatus.FAILED);
    }

    // Request DTO
    public record CreateNotificationRequest(
        Long memberId,
        NotificationType type,
        String subject,
        String message,
        String referenceType,
        Long referenceId
    ) {}
}
