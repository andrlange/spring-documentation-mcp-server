package com.example.library.notifications.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberId(Long memberId);

    List<Notification> findByMemberIdAndStatus(Long memberId, Notification.NotificationStatus status);

    List<Notification> findByStatus(Notification.NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.memberId = :memberId ORDER BY n.createdAt DESC")
    Page<Notification> findByMemberIdOrderByCreatedAtDesc(
        @Param("memberId") Long memberId,
        Pageable pageable
    );

    @Query("SELECT n FROM Notification n WHERE n.memberId = :memberId AND n.status != 'READ' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.createdAt < :before")
    List<Notification> findPendingOlderThan(@Param("before") LocalDateTime before);

    long countByMemberIdAndStatus(Long memberId, Notification.NotificationStatus status);

    long countByStatus(Notification.NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.referenceType = :refType AND n.referenceId = :refId")
    List<Notification> findByReference(
        @Param("refType") String referenceType,
        @Param("refId") Long referenceId
    );

    // Idempotency checks for event-driven notifications
    boolean existsByMemberIdAndTypeAndReferenceTypeAndReferenceId(
        Long memberId,
        Notification.NotificationType type,
        String referenceType,
        Long referenceId
    );

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.memberId = :memberId AND n.type = :type AND n.referenceType = 'LOAN' AND n.referenceId = :loanId")
    boolean existsForLoan(
        @Param("memberId") Long memberId,
        @Param("type") Notification.NotificationType type,
        @Param("loanId") Long loanId
    );

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.memberId = :memberId AND n.type = 'WELCOME'")
    boolean existsWelcomeNotification(@Param("memberId") Long memberId);
}
