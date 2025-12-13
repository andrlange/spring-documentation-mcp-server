package com.example.library.notifications.internal;

import com.example.library.catalog.events.BookAddedEvent;
import com.example.library.loans.events.BookLoanedEvent;
import com.example.library.loans.events.BookReturnedEvent;
import com.example.library.loans.events.LoanOverdueEvent;
import com.example.library.members.events.MemberRegisteredEvent;
import com.example.library.notifications.api.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for handling events from other modules.
 * Uses Spring Modulith's @ApplicationModuleListener for async, transactional event handling.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ApplicationModuleListener
    public void onMemberRegistered(MemberRegisteredEvent event) {
        log.info("Received MemberRegisteredEvent: {}", event);
        notificationService.sendWelcomeNotification(
            event.memberId(),
            event.fullName(),
            event.cardNumber()
        );
    }

    @ApplicationModuleListener
    public void onBookLoaned(BookLoanedEvent event) {
        log.info("Received BookLoanedEvent: {}", event);
        notificationService.sendLoanConfirmation(
            event.memberId(),
            event.bookIsbn(), // In real app, would fetch book title
            event.dueDate().toString()
        );
    }

    @ApplicationModuleListener
    public void onBookReturned(BookReturnedEvent event) {
        log.info("Received BookReturnedEvent: {}", event);
        notificationService.sendReturnConfirmation(
            event.memberId(),
            event.bookIsbn(), // In real app, would fetch book title
            event.fineAmount()
        );
    }

    @ApplicationModuleListener
    public void onLoanOverdue(LoanOverdueEvent event) {
        log.info("Received LoanOverdueEvent: {}", event);
        notificationService.sendOverdueNotice(
            event.memberId(),
            event.loanId(),
            event.bookIsbn(), // In real app, would fetch book title
            event.daysOverdue()
        );
    }

    @ApplicationModuleListener
    public void onBookAdded(BookAddedEvent event) {
        log.info("Received BookAddedEvent: {} - {} by {}",
            event.isbn(), event.title(), event.authorName());
        // Could notify members who have this author/book on their wishlist
    }
}
