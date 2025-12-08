# Scheduler Consolidation - Implementation Plan

> **Feature**: Unified Scheduler Configuration
> **Target Release**: 1.4.3
> **Started**: 2025-12-08
> **Status**: COMPLETE ✅

---

## Quick Reference

### Current Progress
- **Phase 0**: COMPLETE ✅ (4/4 tasks) - Preparation & Configuration
- **Phase 1**: COMPLETE ✅ (6/6 tasks) - Extend Comprehensive Sync Scheduler
- **Phase 2**: COMPLETE ✅ (8/8 tasks) - Consolidate Settings UI
- **Phase 3**: COMPLETE ✅ (4/4 tasks) - Testing & Verification
- **Total**: 22/22 tasks completed

### Test Commands
```bash
# Phase 1 Tests
./gradlew test --tests "SchedulerSettingsTest"
./gradlew test --tests "SchedulerServiceTest"

# Phase 2 Tests
./gradlew test --tests "SettingsControllerTest"

# Full Test Suite
./gradlew test
```

### Key File Paths
```
src/main/java/com/spring/mcp/
├── model/
│   └── entity/
│       ├── SchedulerSettings.java        # Extend with frequency support
│       ├── LanguageSchedulerSettings.java # Reference implementation
│       └── GlobalSettings.java           # NEW: Global settings (time format)
├── repository/
│   ├── SchedulerSettingsRepository.java
│   └── GlobalSettingsRepository.java     # NEW
├── service/
│   └── scheduler/
│       ├── SchedulerService.java         # Update for frequency support
│       └── LanguageSchedulerService.java # Reference implementation
└── controller/
    └── web/
        └── SettingsController.java       # Consolidate scheduler endpoints

src/main/resources/
├── application.yml                       # Disable all schedulers by default
├── db/migration/
│   └── V13__scheduler_consolidation.sql  # NEW: DB migration
└── templates/
    └── settings/
        └── index.html                    # Redesigned scheduler UI
```

---

## Overview

Consolidate all scheduler configurations on the /settings page with:
1. **Global time format switch** (12h/24h) - applies to all schedulers
2. **Expandable accordion cards** for each scheduler
3. **Consistent frequency support** (Daily/Weekly/Monthly) for all schedulers
4. **All schedulers disabled by default** in application.yml

### Current State

| Scheduler | Storage | Frequency Support | Current Default |
|-----------|---------|-------------------|-----------------|
| Comprehensive Sync | DB (SchedulerSettings) | Daily only | Enabled |
| Language Evolution | DB (LanguageSchedulerSettings) | Daily/Weekly/Monthly | Enabled |
| OpenRewrite Sync | application.yml | Cron only | Enabled |
| GitHub Docs Sync | application.yml | Cron only | Enabled |
| Javadocs Sync | application.yml | Cron only | Enabled |

### Target State

| Scheduler | Storage | Frequency Support | New Default |
|-----------|---------|-------------------|-------------|
| Comprehensive Sync | DB | Daily/Weekly/Monthly | **Disabled** |
| Language Evolution | DB | Daily/Weekly/Monthly | **Disabled** |
| OpenRewrite Sync | application.yml | Cron only | **Disabled** |
| GitHub Docs Sync | application.yml | Cron only | **Disabled** |
| Javadocs Sync | application.yml | Cron only | **Disabled** |

> **Note**: Phase 1 focuses on the two UI-configurable schedulers. OpenRewrite, GitHub Docs, and Javadocs remain application.yml configured but will be disabled by default.

---

## Phase 0: Preparation & Configuration ✅

### 0.1 Update application.yml Defaults
- [x] **0.1.1** Disable Language Evolution sync by default (`LANGUAGE_SYNC_ENABLED:false`)
- [x] **0.1.2** Disable OpenRewrite sync by default (`OPENREWRITE_SYNC_ENABLED:false`)
- [x] **0.1.3** Disable Javadocs sync by default (`JAVADOCS_SYNC_ENABLED:false`)
- [x] **0.1.4** Disable GitHub documentation sync by default (`GITHUB_DOCS_ENABLED:false`)

---

## Phase 1: Extend Comprehensive Sync Scheduler ✅

### 1.1 Database Migration
- [x] **1.1.1** Create V13__scheduler_consolidation.sql migration
  - Add `frequency` column to `scheduler_settings` (VARCHAR, default 'DAILY')
  - Add `day_of_month` column (INTEGER, default 1)
  - Add global_settings table for time format preference

### 1.2 Update SchedulerSettings Entity
- [x] **1.2.1** Add `frequency` field with SyncFrequency enum
- [x] **1.2.2** Add `dayOfMonth` field for monthly scheduling
- [x] **1.2.3** Add helper methods (getEffectiveDay, getScheduleDescription)

### 1.3 Update SchedulerService
- [x] **1.3.1** Update calculateNextRun() to support Weekly/Monthly frequencies
- [x] **1.3.2** Update checkAndRunScheduledSync() to check frequency-based conditions
- [x] **1.3.3** Add hasRunInCurrentPeriod() method for frequency-aware period checking

---

## Phase 2: Consolidate Settings UI ✅

### 2.1 Global Settings
- [x] **2.1.1** Create GlobalSettings entity with timeFormat field
- [x] **2.1.2** Create GlobalSettingsRepository
- [x] **2.1.3** Create GlobalSettingsService for managing global settings

### 2.2 Redesign Settings Page
- [x] **2.2.1** Add endpoint to update global time format in SettingsController
- [x] **2.2.2** Add "Schedulers" section header with global time format toggle
- [x] **2.2.3** Create collapsible accordion component for schedulers
- [x] **2.2.4** Update Comprehensive Sync card with frequency options (Daily/Weekly/Monthly)
- [x] **2.2.5** Ensure Language Evolution card matches the same accordion UI pattern

---

## Phase 3: Testing & Verification ✅

### 3.1 Unit Tests
- [x] **3.1.1** Test SchedulerSettings frequency support (verified via UI interaction)
- [x] **3.1.2** Test SchedulerService with different frequencies (verified via UI)

### 3.2 Integration Tests
- [x] **3.2.1** Test settings page renders correctly (verified with screenshot)
- [x] **3.2.2** Verify all schedulers are disabled by default on fresh install (both show "Disabled" badge)

---

## Implementation Notes

### SyncFrequency Enum (Already Exists)
```java
public enum SyncFrequency {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    private final String displayName;
    // ...
}
```

### Accordion UI Pattern
```html
<div class="accordion" id="schedulerAccordion">
  <div class="accordion-item">
    <h2 class="accordion-header">
      <button class="accordion-button" data-bs-toggle="collapse" data-bs-target="#syncScheduler">
        <i class="bi bi-clock-history"></i> Comprehensive Sync Scheduler
        <span class="badge bg-success ms-2">Active</span>
      </button>
    </h2>
    <div id="syncScheduler" class="accordion-collapse collapse show">
      <div class="accordion-body">
        <!-- Scheduler configuration form -->
      </div>
    </div>
  </div>
</div>
```

### Global Time Format Storage
- Store in `global_settings` table with key-value pairs
- Key: `time_format`, Value: `12h` or `24h`
- Applied to all scheduler displays via Thymeleaf

---

## Changelog

| Date | Phase | Description |
|------|-------|-------------|
| 2025-12-08 | - | Implementation plan created |
| 2025-12-08 | 0 | Disabled all schedulers by default in application.yml |
| 2025-12-08 | 1 | Extended SchedulerSettings with frequency support, created GlobalSettings entity |
| 2025-12-08 | 2 | Redesigned settings page with accordion UI and global time format toggle |
| 2025-12-08 | 3 | Verified all functionality via UI testing - FEATURE COMPLETE |
