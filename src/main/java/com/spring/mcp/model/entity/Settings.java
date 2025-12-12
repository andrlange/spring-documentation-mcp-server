package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing system settings.
 * This entity uses a singleton pattern - only one row should exist.
 */
@Entity
@Table(name = "settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = {"id"})
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enterprise_subscription_enabled", nullable = false)
    @Builder.Default
    private Boolean enterpriseSubscriptionEnabled = false;

    /**
     * Include SNAPSHOT versions in Javadoc sync (e.g., 2.0.0-SNAPSHOT).
     * Default: false (exclude SNAPSHOTs)
     */
    @Column(name = "javadoc_sync_snapshot", nullable = false)
    @Builder.Default
    private Boolean javadocSyncSnapshot = false;

    /**
     * Include RC (Release Candidate) versions in Javadoc sync (e.g., 1.0.0-RC1).
     * Default: false (exclude RCs)
     */
    @Column(name = "javadoc_sync_rc", nullable = false)
    @Builder.Default
    private Boolean javadocSyncRc = false;

    /**
     * Include Milestone versions in Javadoc sync (e.g., 1.0.0-M1).
     * Default: false (exclude Milestones)
     */
    @Column(name = "javadoc_sync_milestone", nullable = false)
    @Builder.Default
    private Boolean javadocSyncMilestone = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
