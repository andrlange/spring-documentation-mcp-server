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
 *
 * <p>Spring Modulith 2.0 Event System Features:
 * <ul>
 *   <li>Events are persisted to event_publication table before processing</li>
 *   <li>At-least-once delivery guarantee with automatic retry</li>
 *   <li>Idempotency checks prevent duplicate notifications on retry</li>
 * </ul>
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

        // Idempotency check: Skip if welcome notification already sent
        if (notificationService.existsWelcomeNotification(event.memberId())) {
            log.debug("Welcome notification already exists for member {}, skipping", event.memberId());
            return;
        }

        notificationService.sendWelcomeNotification(
            event.memberId(),
            event.fullName(),
            event.cardNumber()
        );
    }

    @ApplicationModuleListener
    public void onBookLoaned(BookLoanedEvent event) {
        log.info("Received BookLoanedEvent: {}", event);

        // Idempotency check: Skip if loan confirmation already sent
        if (notificationService.existsLoanConfirmation(event.memberId(), event.loanId())) {
            log.debug("Loan confirmation already exists for loan {}, skipping", event.loanId());
            return;
        }

        notificationService.sendLoanConfirmation(
            event.memberId(),
            event.bookIsbn(), // In real app, would fetch book title
            event.dueDate().toString()
        );
    }

    @ApplicationModuleListener
    public void onBookReturned(BookReturnedEvent event) {
        log.info("Received BookReturnedEvent: {}", event);

        // Idempotency check: Skip if return confirmation already sent
        if (notificationService.existsReturnConfirmation(event.memberId(), event.loanId())) {
            log.debug("Return confirmation already exists for loan {}, skipping", event.loanId());
            return;
        }

        notificationService.sendReturnConfirmation(
            event.memberId(),
            event.bookIsbn(), // In real app, would fetch book title
            event.fineAmount()
        );
    }

    @ApplicationModuleListener
    public void onLoanOverdue(LoanOverdueEvent event) {
        log.info("Received LoanOverdueEvent: {}", event);

        // Idempotency check: Skip if overdue notice already sent
        if (notificationService.existsOverdueNotice(event.memberId(), event.loanId())) {
            log.debug("Overdue notice already exists for loan {}, skipping", event.loanId());
            return;
        }

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
        // No notification created for this event currently
    }
}
