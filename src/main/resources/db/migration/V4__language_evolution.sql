-- Spring MCP Server - Language Evolution Feature
-- Version: 1.2.0
-- Description: Track Java and Kotlin language version evolution (features, deprecations, removals)
-- Date: 2025-11-29

-- ============================================================
-- LANGUAGE VERSIONS TABLE
-- ============================================================
CREATE TABLE language_versions (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(20) NOT NULL,
    version VARCHAR(50) NOT NULL,
    major_version INT NOT NULL,
    minor_version INT NOT NULL,
    patch_version INT,
    codename VARCHAR(100),
    release_date DATE,
    is_lts BOOLEAN DEFAULT false,
    is_current BOOLEAN DEFAULT false,
    oss_support_end DATE,
    extended_support_end DATE,
    min_spring_boot_version VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_language_version UNIQUE(language, version),
    CONSTRAINT chk_language CHECK (language IN ('JAVA', 'KOTLIN'))
);

COMMENT ON TABLE language_versions IS 'Tracks Java and Kotlin language versions with support dates';
COMMENT ON COLUMN language_versions.language IS 'Programming language: JAVA or KOTLIN';
COMMENT ON COLUMN language_versions.is_lts IS 'Long Term Support version (Java only)';
COMMENT ON COLUMN language_versions.min_spring_boot_version IS 'Minimum Spring Boot version that supports this language version';

-- ============================================================
-- LANGUAGE FEATURES TABLE
-- ============================================================
CREATE TABLE language_features (
    id BIGSERIAL PRIMARY KEY,
    language_version_id BIGINT NOT NULL REFERENCES language_versions(id) ON DELETE CASCADE,
    feature_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    category VARCHAR(100),
    jep_number VARCHAR(20),
    kep_number VARCHAR(20),
    description TEXT,
    impact_level VARCHAR(20),
    migration_notes TEXT,
    documentation_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_feature_status CHECK (status IN ('NEW', 'DEPRECATED', 'REMOVED', 'PREVIEW', 'INCUBATING')),
    CONSTRAINT chk_impact CHECK (impact_level IS NULL OR impact_level IN ('HIGH', 'MEDIUM', 'LOW'))
);

COMMENT ON TABLE language_features IS 'Individual language features, deprecations, and removals per version';
COMMENT ON COLUMN language_features.status IS 'Feature status: NEW (added), DEPRECATED (marked for removal), REMOVED, PREVIEW (experimental), INCUBATING';
COMMENT ON COLUMN language_features.jep_number IS 'JDK Enhancement Proposal number (Java only)';
COMMENT ON COLUMN language_features.kep_number IS 'Kotlin Enhancement Proposal number (Kotlin only)';
COMMENT ON COLUMN language_features.impact_level IS 'Impact on existing code: HIGH (breaking), MEDIUM (significant), LOW (minor)';

-- ============================================================
-- LANGUAGE CODE PATTERNS TABLE
-- ============================================================
CREATE TABLE language_code_patterns (
    id BIGSERIAL PRIMARY KEY,
    feature_id BIGINT NOT NULL REFERENCES language_features(id) ON DELETE CASCADE,
    old_pattern TEXT NOT NULL,
    new_pattern TEXT NOT NULL,
    pattern_language VARCHAR(20) NOT NULL,
    explanation TEXT,
    min_version VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE language_code_patterns IS 'Code examples showing old vs new patterns for features';
COMMENT ON COLUMN language_code_patterns.old_pattern IS 'Legacy code pattern (pre-feature)';
COMMENT ON COLUMN language_code_patterns.new_pattern IS 'Modern code pattern using the feature';
COMMENT ON COLUMN language_code_patterns.pattern_language IS 'Code language: java, kotlin';

-- ============================================================
-- SPRING BOOT LANGUAGE REQUIREMENTS TABLE
-- ============================================================
CREATE TABLE spring_boot_language_requirements (
    id BIGSERIAL PRIMARY KEY,
    spring_boot_version_id BIGINT NOT NULL REFERENCES spring_boot_versions(id) ON DELETE CASCADE,
    language VARCHAR(20) NOT NULL,
    min_version VARCHAR(50) NOT NULL,
    recommended_version VARCHAR(50),
    max_version VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_boot_language UNIQUE(spring_boot_version_id, language),
    CONSTRAINT chk_req_language CHECK (language IN ('JAVA', 'KOTLIN'))
);

COMMENT ON TABLE spring_boot_language_requirements IS 'Maps Spring Boot versions to required Java/Kotlin versions';
COMMENT ON COLUMN spring_boot_language_requirements.min_version IS 'Minimum required language version';
COMMENT ON COLUMN spring_boot_language_requirements.recommended_version IS 'Recommended language version for best compatibility';

-- ============================================================
-- LANGUAGE SCHEDULER SETTINGS TABLE
-- ============================================================
CREATE TABLE language_scheduler_settings (
    id BIGSERIAL PRIMARY KEY,
    sync_enabled BOOLEAN NOT NULL DEFAULT false,
    sync_time VARCHAR(5) NOT NULL DEFAULT '04:00',
    time_format VARCHAR(3) NOT NULL DEFAULT '24h',
    frequency VARCHAR(10) NOT NULL DEFAULT 'WEEKLY',
    weekdays VARCHAR(30) DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN',
    all_weekdays BOOLEAN DEFAULT true,
    day_of_month INT DEFAULT 1,
    months VARCHAR(50) DEFAULT 'JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC',
    all_months BOOLEAN DEFAULT true,
    last_sync_run TIMESTAMP,
    next_sync_run TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_lang_time_format CHECK (time_format IN ('12h', '24h')),
    CONSTRAINT chk_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_day_of_month CHECK (day_of_month BETWEEN 1 AND 31),
    CONSTRAINT chk_lang_sync_time_format CHECK (sync_time ~ '^([0-1][0-9]|2[0-3]):[0-5][0-9]$')
);

COMMENT ON TABLE language_scheduler_settings IS 'Scheduler configuration for language evolution sync (separate from comprehensive sync)';
COMMENT ON COLUMN language_scheduler_settings.frequency IS 'Sync frequency: DAILY, WEEKLY, MONTHLY';
COMMENT ON COLUMN language_scheduler_settings.weekdays IS 'Comma-separated weekday codes for WEEKLY frequency';
COMMENT ON COLUMN language_scheduler_settings.day_of_month IS 'Day of month for MONTHLY frequency (1-31, uses last day fallback)';
COMMENT ON COLUMN language_scheduler_settings.months IS 'Comma-separated month codes for MONTHLY frequency';

-- ============================================================
-- EXTEND EXISTING SCHEDULER_SETTINGS TABLE
-- ============================================================
ALTER TABLE scheduler_settings ADD COLUMN IF NOT EXISTS weekdays VARCHAR(30) DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN';
ALTER TABLE scheduler_settings ADD COLUMN IF NOT EXISTS all_weekdays BOOLEAN DEFAULT true;

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX idx_lang_ver_language ON language_versions(language);
CREATE INDEX idx_lang_ver_lts ON language_versions(is_lts) WHERE is_lts = true;
CREATE INDEX idx_lang_ver_current ON language_versions(is_current) WHERE is_current = true;
CREATE INDEX idx_lang_feat_version ON language_features(language_version_id);
CREATE INDEX idx_lang_feat_status ON language_features(status);
CREATE INDEX idx_lang_feat_category ON language_features(category);
CREATE INDEX idx_lang_feat_jep ON language_features(jep_number) WHERE jep_number IS NOT NULL;
CREATE INDEX idx_code_patterns_feature ON language_code_patterns(feature_id);
CREATE INDEX idx_boot_lang_req ON spring_boot_language_requirements(spring_boot_version_id);
CREATE INDEX idx_boot_lang_req_language ON spring_boot_language_requirements(language);

-- Full-text search index for feature names and descriptions
CREATE INDEX idx_lang_feat_search ON language_features USING gin(to_tsvector('english', feature_name || ' ' || COALESCE(description, '')));

-- ============================================================
-- INSERT DEFAULT LANGUAGE SCHEDULER SETTINGS
-- ============================================================
INSERT INTO language_scheduler_settings (
    sync_enabled,
    sync_time,
    time_format,
    frequency,
    weekdays,
    all_weekdays,
    day_of_month,
    months,
    all_months
) VALUES (
    false,          -- disabled by default
    '04:00',        -- 4 AM
    '24h',          -- 24-hour format
    'WEEKLY',       -- weekly sync
    'MON,TUE,WED,THU,FRI,SAT,SUN',
    true,           -- all weekdays
    1,              -- 1st of month (for monthly)
    'JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC',
    true            -- all months
);

-- ============================================================
-- SEED DATA: JAVA VERSIONS (8-25)
-- ============================================================
INSERT INTO language_versions (language, version, major_version, minor_version, codename, release_date, is_lts, is_current, oss_support_end, extended_support_end, min_spring_boot_version) VALUES
('JAVA', '8', 8, 0, 'Spider', '2014-03-18', true, false, '2030-12-01', '2030-12-01', '1.5.0'),
('JAVA', '9', 9, 0, NULL, '2017-09-21', false, false, '2018-03-01', NULL, '2.0.0'),
('JAVA', '10', 10, 0, NULL, '2018-03-20', false, false, '2018-09-01', NULL, '2.0.0'),
('JAVA', '11', 11, 0, NULL, '2018-09-25', true, false, '2026-09-01', '2032-01-01', '2.1.0'),
('JAVA', '12', 12, 0, NULL, '2019-03-19', false, false, '2019-09-01', NULL, '2.1.0'),
('JAVA', '13', 13, 0, NULL, '2019-09-17', false, false, '2020-03-01', NULL, '2.2.0'),
('JAVA', '14', 14, 0, NULL, '2020-03-17', false, false, '2020-09-01', NULL, '2.3.0'),
('JAVA', '15', 15, 0, NULL, '2020-09-15', false, false, '2021-03-01', NULL, '2.4.0'),
('JAVA', '16', 16, 0, NULL, '2021-03-16', false, false, '2021-09-01', NULL, '2.5.0'),
('JAVA', '17', 17, 0, NULL, '2021-09-14', true, false, '2029-09-01', '2032-09-01', '3.0.0'),
('JAVA', '18', 18, 0, NULL, '2022-03-22', false, false, '2022-09-01', NULL, '3.0.0'),
('JAVA', '19', 19, 0, NULL, '2022-09-20', false, false, '2023-03-01', NULL, '3.0.0'),
('JAVA', '20', 20, 0, NULL, '2023-03-21', false, false, '2023-09-01', NULL, '3.1.0'),
('JAVA', '21', 21, 0, NULL, '2023-09-19', true, false, '2031-09-01', '2031-09-01', '3.2.0'),
('JAVA', '22', 22, 0, NULL, '2024-03-19', false, false, '2024-09-01', NULL, '3.3.0'),
('JAVA', '23', 23, 0, NULL, '2024-09-17', false, false, '2025-03-01', NULL, '3.4.0'),
('JAVA', '24', 24, 0, NULL, '2025-03-18', false, false, '2025-09-01', NULL, '3.5.0'),
('JAVA', '25', 25, 0, NULL, '2025-09-16', true, true, '2033-09-01', '2033-09-01', '4.0.0');

-- ============================================================
-- SEED DATA: KOTLIN VERSIONS (1.6-2.1)
-- ============================================================
INSERT INTO language_versions (language, version, major_version, minor_version, release_date, is_lts, is_current, min_spring_boot_version) VALUES
('KOTLIN', '1.6', 1, 6, '2021-11-16', false, false, '2.6.0'),
('KOTLIN', '1.7', 1, 7, '2022-06-09', false, false, '2.7.0'),
('KOTLIN', '1.8', 1, 8, '2022-12-28', false, false, '3.0.0'),
('KOTLIN', '1.9', 1, 9, '2023-07-06', false, false, '3.1.0'),
('KOTLIN', '2.0', 2, 0, '2024-05-21', false, false, '3.3.0'),
('KOTLIN', '2.1', 2, 1, '2024-11-27', false, true, '3.4.0');

-- ============================================================
-- SEED DATA: KEY JAVA FEATURES (Comprehensive)
-- ============================================================

-- Java 8 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '8'), 'Lambda Expressions', 'NEW', 'Syntax', '126', 'Functional programming with lambda expressions and functional interfaces', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '8'), 'Stream API', 'NEW', 'API', '107', 'Functional-style operations on streams of elements', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '8'), 'Optional Class', 'NEW', 'API', NULL, 'Container object for handling null values', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '8'), 'Default Methods', 'NEW', 'Syntax', NULL, 'Default method implementations in interfaces', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '8'), 'Method References', 'NEW', 'Syntax', NULL, 'Reference to methods or constructors using ::', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '8'), 'Date/Time API', 'NEW', 'API', '150', 'New java.time package replacing java.util.Date', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '8'), 'CompletableFuture', 'NEW', 'Concurrency', NULL, 'Asynchronous programming with futures', 'MEDIUM');

-- Java 9 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '9'), 'Module System (Jigsaw)', 'NEW', 'Architecture', '261', 'Java Platform Module System for modular applications', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '9'), 'JShell REPL', 'NEW', 'Tooling', '222', 'Interactive Java shell for quick testing', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '9'), 'Private Interface Methods', 'NEW', 'Syntax', '213', 'Private methods in interfaces', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '9'), 'Collection Factory Methods', 'NEW', 'API', '269', 'List.of(), Set.of(), Map.of() factory methods', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '9'), 'Stream Improvements', 'NEW', 'API', NULL, 'takeWhile(), dropWhile(), ofNullable() methods', 'LOW');

-- Java 10 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '10'), 'Local Variable Type Inference (var)', 'NEW', 'Syntax', '286', 'Type inference for local variables with var keyword', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '10'), 'Unmodifiable Collections', 'NEW', 'API', NULL, 'List.copyOf(), Set.copyOf(), Map.copyOf() methods', 'LOW');

-- Java 11 Features (LTS)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '11'), 'HTTP Client API', 'NEW', 'API', '321', 'Standard HTTP/2 client replacing HttpURLConnection', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '11'), 'String Methods', 'NEW', 'API', NULL, 'isBlank(), lines(), strip(), repeat() methods', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '11'), 'Local-Variable Syntax for Lambda', 'NEW', 'Syntax', '323', 'var in lambda expressions', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '11'), 'Files.readString/writeString', 'NEW', 'API', NULL, 'Convenient file I/O methods', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '11'), 'Java EE Modules Removed', 'REMOVED', 'API', '320', 'Removed java.xml.ws, java.xml.bind, java.activation, etc.', 'HIGH');

-- Java 12 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '12'), 'Switch Expressions (Preview)', 'PREVIEW', 'Syntax', '325', 'Switch as expression with arrow syntax', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '12'), 'Compact Number Formatting', 'NEW', 'API', NULL, 'NumberFormat.getCompactNumberInstance()', 'LOW');

-- Java 13 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '13'), 'Text Blocks (Preview)', 'PREVIEW', 'Syntax', '355', 'Multi-line string literals with triple quotes', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '13'), 'Switch Expressions (2nd Preview)', 'PREVIEW', 'Syntax', '354', 'Refined switch expressions with yield', 'HIGH');

-- Java 14 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '14'), 'Switch Expressions (Standard)', 'NEW', 'Syntax', '361', 'Switch expressions finalized', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '14'), 'Records (Preview)', 'PREVIEW', 'Syntax', '359', 'Compact syntax for data carrier classes', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '14'), 'Pattern Matching instanceof (Preview)', 'PREVIEW', 'Syntax', '305', 'instanceof with pattern variable binding', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '14'), 'Helpful NullPointerExceptions', 'NEW', 'Debugging', '358', 'Detailed NPE messages showing which variable was null', 'MEDIUM');

-- Java 15 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '15'), 'Text Blocks (Standard)', 'NEW', 'Syntax', '378', 'Text blocks finalized', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '15'), 'Sealed Classes (Preview)', 'PREVIEW', 'Syntax', '360', 'Restrict which classes can extend/implement', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '15'), 'Records (2nd Preview)', 'PREVIEW', 'Syntax', '384', 'Records refined', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '15'), 'Hidden Classes', 'NEW', 'API', '371', 'Classes that cannot be used directly by bytecode', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '15'), 'Nashorn Removed', 'REMOVED', 'API', '372', 'Nashorn JavaScript engine removed', 'MEDIUM');

-- Java 16 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '16'), 'Records (Standard)', 'NEW', 'Syntax', '395', 'Records finalized - immutable data classes', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '16'), 'Pattern Matching instanceof (Standard)', 'NEW', 'Syntax', '394', 'Pattern matching for instanceof finalized', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '16'), 'Sealed Classes (2nd Preview)', 'PREVIEW', 'Syntax', '397', 'Sealed classes refined', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '16'), 'Stream.toList()', 'NEW', 'API', NULL, 'Convenient method to collect stream to unmodifiable list', 'MEDIUM');

-- Java 17 Features (LTS)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '17'), 'Sealed Classes (Standard)', 'NEW', 'Syntax', '409', 'Sealed classes finalized', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '17'), 'Pattern Matching for switch (Preview)', 'PREVIEW', 'Syntax', '406', 'Pattern matching in switch expressions/statements', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '17'), 'Strong Encapsulation by Default', 'NEW', 'Architecture', '403', 'Strongly encapsulate JDK internals', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '17'), 'SecurityManager Deprecated', 'DEPRECATED', 'Security', '411', 'SecurityManager marked for future removal', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '17'), 'RMI Activation Removed', 'REMOVED', 'API', '407', 'Remote Method Invocation Activation removed', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '17'), 'Applet API Deprecated', 'DEPRECATED', 'API', '398', 'Applet API marked for removal', 'LOW');

-- Java 18 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '18'), 'Simple Web Server', 'NEW', 'Tooling', '408', 'jwebserver command-line tool for static files', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '18'), 'Code Snippets in JavaDoc', 'NEW', 'Tooling', '413', '@snippet tag for code examples in documentation', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '18'), 'Pattern Matching for switch (2nd Preview)', 'PREVIEW', 'Syntax', '420', 'Refined pattern matching for switch', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '18'), 'UTF-8 by Default', 'NEW', 'API', '400', 'UTF-8 as default charset', 'MEDIUM');

-- Java 19 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '19'), 'Virtual Threads (Preview)', 'PREVIEW', 'Concurrency', '425', 'Lightweight threads for high-throughput concurrent applications', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '19'), 'Record Patterns (Preview)', 'PREVIEW', 'Syntax', '405', 'Deconstruct record values in pattern matching', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '19'), 'Pattern Matching for switch (3rd Preview)', 'PREVIEW', 'Syntax', '427', 'Further refinements to switch patterns', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '19'), 'Foreign Function & Memory API (Preview)', 'PREVIEW', 'API', '424', 'Interoperate with code and data outside Java runtime', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '19'), 'Structured Concurrency (Incubator)', 'INCUBATING', 'Concurrency', '428', 'Treat multiple tasks as single unit of work', 'MEDIUM');

-- Java 20 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '20'), 'Scoped Values (Incubator)', 'INCUBATING', 'Concurrency', '429', 'Share immutable data within and across threads', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '20'), 'Virtual Threads (2nd Preview)', 'PREVIEW', 'Concurrency', '436', 'Refined virtual threads', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '20'), 'Record Patterns (2nd Preview)', 'PREVIEW', 'Syntax', '432', 'Refined record patterns', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '20'), 'Pattern Matching for switch (4th Preview)', 'PREVIEW', 'Syntax', '433', 'Further switch pattern refinements', 'HIGH');

-- Java 21 Features (LTS)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Virtual Threads (Standard)', 'NEW', 'Concurrency', '444', 'Virtual threads finalized - scalable concurrency', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Record Patterns (Standard)', 'NEW', 'Syntax', '440', 'Record patterns finalized for destructuring', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Pattern Matching for switch (Standard)', 'NEW', 'Syntax', '441', 'Pattern matching for switch finalized', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Sequenced Collections', 'NEW', 'API', '431', 'SequencedCollection, SequencedSet, SequencedMap interfaces', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'String Templates (Preview)', 'PREVIEW', 'Syntax', '430', 'String interpolation with STR, FMT, RAW processors', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Unnamed Patterns and Variables (Preview)', 'PREVIEW', 'Syntax', '443', 'Use _ for unused variables', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Unnamed Classes (Preview)', 'PREVIEW', 'Syntax', '445', 'Simplified main classes without class declaration', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Scoped Values (Preview)', 'PREVIEW', 'Concurrency', '446', 'Share immutable data across threads safely', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21'), 'Structured Concurrency (Preview)', 'PREVIEW', 'Concurrency', '453', 'Structured concurrency API', 'MEDIUM');

-- Java 22 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '22'), 'Unnamed Variables & Patterns (Standard)', 'NEW', 'Syntax', '456', 'Underscore for unused variables finalized', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '22'), 'Foreign Function & Memory API (Standard)', 'NEW', 'API', '454', 'FFM API finalized for native interop', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '22'), 'Statements before super() (Preview)', 'PREVIEW', 'Syntax', '447', 'Execute code before super() call in constructors', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '22'), 'String Templates (2nd Preview)', 'PREVIEW', 'Syntax', '459', 'String templates refined', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '22'), 'Stream Gatherers (Preview)', 'PREVIEW', 'API', '461', 'Custom intermediate stream operations', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '22'), 'Class-File API (Preview)', 'PREVIEW', 'API', '457', 'API for parsing and generating class files', 'LOW');

-- Java 23 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'Primitive Types in Patterns (Preview)', 'PREVIEW', 'Syntax', '455', 'Pattern matching with primitive types', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'Module Import Declarations (Preview)', 'PREVIEW', 'Syntax', '476', 'Import all packages from a module', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'Flexible Constructor Bodies (Preview)', 'PREVIEW', 'Syntax', '482', 'More flexibility in constructor statements', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'String Templates Withdrawn', 'REMOVED', 'Syntax', NULL, 'String Templates feature withdrawn pending redesign', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'Implicitly Declared Classes (2nd Preview)', 'PREVIEW', 'Syntax', '477', 'Simplified main class declarations', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'Scoped Values (2nd Preview)', 'PREVIEW', 'Concurrency', '481', 'Refined scoped values', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'Structured Concurrency (2nd Preview)', 'PREVIEW', 'Concurrency', '480', 'Refined structured concurrency', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '23'), 'Stream Gatherers (2nd Preview)', 'PREVIEW', 'API', '473', 'Refined stream gatherers', 'MEDIUM');

-- Java 24 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'Primitive Types in Patterns (2nd Preview)', 'PREVIEW', 'Syntax', '488', 'Refined primitive patterns', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'Flexible Constructor Bodies (2nd Preview)', 'PREVIEW', 'Syntax', '492', 'Further constructor flexibility', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'Module Import Declarations (2nd Preview)', 'PREVIEW', 'Syntax', '494', 'Refined module imports', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'Stream Gatherers (Standard)', 'NEW', 'API', '485', 'Stream gatherers finalized', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'Class-File API (2nd Preview)', 'PREVIEW', 'API', '484', 'Refined class-file API', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'Scoped Values (3rd Preview)', 'PREVIEW', 'Concurrency', '487', 'Further scoped values refinement', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'Structured Concurrency (3rd Preview)', 'PREVIEW', 'Concurrency', '499', 'Further structured concurrency refinement', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '24'), 'SecurityManager Removed', 'REMOVED', 'Security', '486', 'SecurityManager permanently removed', 'HIGH');

-- Java 25 Features (LTS)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'), 'Flexible Constructor Bodies (Standard)', 'NEW', 'Syntax', '501', 'Pre-super() code finalized', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'), 'Primitive Types in Patterns (3rd Preview)', 'PREVIEW', 'Syntax', '507', 'Primitive patterns nearing finalization', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'), 'Module Import Declarations (3rd Preview)', 'PREVIEW', 'Syntax', '511', 'Module imports nearing finalization', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'), 'Structured Concurrency (4th Preview)', 'PREVIEW', 'Concurrency', '505', 'Structured concurrency continued refinement', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'), 'Scoped Values (4th Preview)', 'PREVIEW', 'Concurrency', '502', 'Scoped values continued refinement', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'), 'Stable Values (Preview)', 'PREVIEW', 'API', '502', 'Immutable holders with deferred initialization', 'MEDIUM');

-- ============================================================
-- SEED DATA: KOTLIN FEATURES
-- ============================================================

-- Kotlin 1.6 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.6'), 'Exhaustive when Statements', 'NEW', 'Syntax', 'when statements must be exhaustive when used as expressions', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.6'), 'Suspend Functions as Supertypes', 'NEW', 'Concurrency', 'Suspend function types as supertypes', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.6'), 'Builder Inference', 'NEW', 'Syntax', 'Improved type inference in builder patterns', 'LOW');

-- Kotlin 1.7 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.7'), 'Underscore Operator for Type Arguments', 'NEW', 'Syntax', 'Use _ for automatic type inference in generics', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.7'), 'Stable Builder Inference', 'NEW', 'Syntax', 'Builder inference graduated to stable', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.7'), 'Definitely Non-Nullable Types', 'NEW', 'Types', 'T & Any syntax for definitely non-nullable types', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.7'), 'Named Arguments in Function Types', 'NEW', 'Syntax', 'Support for named arguments in functional types', 'LOW');

-- Kotlin 1.8 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.8'), 'Recursive copyOf in Arrays', 'NEW', 'API', 'Deep copy functionality for nested arrays', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.8'), 'Stable kotlin.time API', 'NEW', 'API', 'kotlin.time.Duration and related APIs stabilized', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.8'), 'JVM IR Backend Default', 'NEW', 'Compiler', 'IR backend becomes default for JVM', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.8'), 'Kotlin/JS IR Stable', 'NEW', 'Compiler', 'IR compiler for JavaScript stabilized', 'MEDIUM');

-- Kotlin 1.9 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.9'), 'K2 Compiler (Beta)', 'PREVIEW', 'Compiler', 'New K2 compiler in beta, up to 94% faster compilation', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.9'), 'Data Objects', 'NEW', 'Syntax', 'data object for singleton data classes', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.9'), 'Enum entries Property', 'NEW', 'API', 'entries property replacing values() for enums', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.9'), 'Open-Ended Ranges', 'NEW', 'API', 'Ranges with exclusive upper bound using ..<', 'LOW'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '1.9'), 'Secondary Constructor Body in Inline Classes', 'NEW', 'Syntax', 'Inline value classes can have secondary constructors with body', 'LOW');

-- Kotlin 2.0 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.0'), 'K2 Compiler (Stable)', 'NEW', 'Compiler', 'K2 compiler stable - up to 94% faster builds', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.0'), 'Improved Smart Casts', 'NEW', 'Types', 'Smart casts work with local variables after type checks', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.0'), 'Common Supertype Smart Casts', 'NEW', 'Types', 'Combined type checks with OR produce common supertype', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.0'), 'Smart Casts in catch/finally', 'NEW', 'Types', 'Smart cast information preserved in catch/finally blocks', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.0'), 'Immediate Property Initialization', 'NEW', 'Syntax', 'Open properties with backing fields must be immediately initialized', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.0'), 'Lambda Parameters Improvements', 'NEW', 'Syntax', 'Better inference for lambda parameters and returns', 'LOW');

-- Kotlin 2.1 Features
INSERT INTO language_features (language_version_id, feature_name, status, category, description, impact_level) VALUES
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.1'), 'Guard Conditions in when', 'NEW', 'Syntax', 'Add conditions to when branches with if keyword', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.1'), 'Non-Packed klib Artifacts', 'NEW', 'Build', 'Generate non-packed .klib files for faster builds', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.1'), 'K2 kapt Improvements', 'NEW', 'Tooling', 'Improved kapt performance with K2 compiler', 'MEDIUM'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.1'), 'Context Parameters (Preview)', 'PREVIEW', 'Syntax', 'Implicit context passing to functions', 'HIGH'),
((SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.1'), 'Multi-dollar String Interpolation (Preview)', 'PREVIEW', 'Syntax', 'Custom string interpolation delimiters', 'LOW');

-- ============================================================
-- SEED DATA: CODE PATTERNS
-- ============================================================

-- Java Pattern: instanceof with Pattern Matching
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Pattern Matching instanceof (Standard)' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '16')),
'if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}',
'if (obj instanceof String s) {
    System.out.println(s.length());
}',
'java',
'Pattern matching eliminates the explicit cast. The variable s is automatically bound to the String type.',
'16');

-- Java Pattern: Records vs POJO
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Records (Standard)' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '16')),
'public class Person {
    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }

    @Override
    public boolean equals(Object o) { ... }
    @Override
    public int hashCode() { ... }
    @Override
    public String toString() { ... }
}',
'public record Person(String name, int age) {}',
'java',
'Records provide a compact syntax for immutable data carriers with automatic implementations of constructor, getters, equals, hashCode, and toString.',
'16');

-- Java Pattern: Virtual Threads
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Virtual Threads (Standard)' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21')),
'ExecutorService executor = Executors.newFixedThreadPool(100);
try {
    executor.submit(() -> handleRequest(request));
} finally {
    executor.shutdown();
}',
'try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> handleRequest(request));
}',
'java',
'Virtual threads are lightweight and can scale to millions of concurrent tasks. Use them for I/O-bound workloads instead of platform threads.',
'21');

-- Java Pattern: Switch Expression
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Switch Expressions (Standard)' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '14')),
'String result;
switch (day) {
    case MONDAY:
    case FRIDAY:
    case SUNDAY:
        result = "Relaxed";
        break;
    case TUESDAY:
        result = "Busy";
        break;
    default:
        result = "Normal";
        break;
}',
'String result = switch (day) {
    case MONDAY, FRIDAY, SUNDAY -> "Relaxed";
    case TUESDAY -> "Busy";
    default -> "Normal";
};',
'java',
'Switch expressions return a value and use arrow syntax. Multiple case labels can be combined.',
'14');

-- Java Pattern: Pattern Matching Switch
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Pattern Matching for switch (Standard)' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '21')),
'static String format(Object obj) {
    if (obj instanceof Integer i) {
        return "int %d".formatted(i);
    } else if (obj instanceof Long l) {
        return "long %d".formatted(l);
    } else if (obj instanceof String s) {
        return "String %s".formatted(s);
    }
    return "Unknown";
}',
'static String format(Object obj) {
    return switch (obj) {
        case Integer i -> "int %d".formatted(i);
        case Long l -> "long %d".formatted(l);
        case String s -> "String %s".formatted(s);
        default -> "Unknown";
    };
}',
'java',
'Pattern matching in switch allows type testing and binding in a single, exhaustive expression.',
'21');

-- Java Pattern: Text Blocks
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Text Blocks (Standard)' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '15')),
'String json = "{\n" +
    "  \"name\": \"John\",\n" +
    "  \"age\": 30\n" +
    "}";',
'String json = """
    {
      "name": "John",
      "age": 30
    }
    """;',
'java',
'Text blocks preserve formatting and eliminate escape sequences for multi-line strings.',
'15');

-- Kotlin Pattern: Smart Cast improvements
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Improved Smart Casts' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.0')),
'fun process(animal: Animal) {
    if (animal is Cat) {
        val cat = animal as Cat  // Explicit cast needed
        cat.meow()
    }
}',
'fun process(animal: Animal) {
    if (animal is Cat) {
        animal.meow()  // Smart cast works automatically
    }
}',
'kotlin',
'Kotlin 2.0 improves smart casts to work correctly with local variables after type checks.',
'2.0');

-- Kotlin Pattern: Guard Conditions in when
INSERT INTO language_code_patterns (feature_id, old_pattern, new_pattern, pattern_language, explanation, min_version) VALUES
((SELECT id FROM language_features WHERE feature_name = 'Guard Conditions in when' AND language_version_id = (SELECT id FROM language_versions WHERE language = 'KOTLIN' AND version = '2.1')),
'when (status) {
    Status.SUCCESS -> {
        if (result.isNotEmpty()) {
            handleSuccess(result)
        }
    }
    else -> handleError()
}',
'when (status) {
    Status.SUCCESS if result.isNotEmpty() -> handleSuccess(result)
    else -> handleError()
}',
'kotlin',
'Guard conditions allow adding if clauses directly to when branches for more concise pattern matching.',
'2.1');

-- ============================================================
-- SEED DATA: SPRING BOOT LANGUAGE REQUIREMENTS
-- ============================================================

-- Get Spring Boot version IDs and insert requirements
DO $$
DECLARE
    v_boot_id BIGINT;
BEGIN
    -- Spring Boot 2.x requirements
    SELECT id INTO v_boot_id FROM spring_boot_versions WHERE version LIKE '2.7%' LIMIT 1;
    IF v_boot_id IS NOT NULL THEN
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, notes)
        VALUES (v_boot_id, 'JAVA', '8', '17', 'Java 8, 11, or 17 supported. Java 17 recommended for best performance.');
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, notes)
        VALUES (v_boot_id, 'KOTLIN', '1.6', '1.7', 'Kotlin 1.6+ required for Spring Boot 2.7');
    END IF;

    -- Spring Boot 3.0-3.2 requirements
    SELECT id INTO v_boot_id FROM spring_boot_versions WHERE version LIKE '3.0%' OR version LIKE '3.1%' OR version LIKE '3.2%' LIMIT 1;
    IF v_boot_id IS NOT NULL THEN
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, notes)
        VALUES (v_boot_id, 'JAVA', '17', '21', 'Java 17+ required. Java 21 recommended for virtual threads.');
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, notes)
        VALUES (v_boot_id, 'KOTLIN', '1.8', '1.9', 'Kotlin 1.8+ required for Spring Boot 3.x');
    END IF;

    -- Spring Boot 3.3-3.5 requirements
    SELECT id INTO v_boot_id FROM spring_boot_versions WHERE version LIKE '3.3%' OR version LIKE '3.4%' OR version LIKE '3.5%' LIMIT 1;
    IF v_boot_id IS NOT NULL THEN
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, notes)
        VALUES (v_boot_id, 'JAVA', '17', '21', 'Java 17+ required. Java 21 LTS recommended.');
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, notes)
        VALUES (v_boot_id, 'KOTLIN', '1.9', '2.0', 'Kotlin 1.9+ required. Kotlin 2.0 recommended for K2 compiler.');
    END IF;

    -- Spring Boot 4.0 requirements
    SELECT id INTO v_boot_id FROM spring_boot_versions WHERE version LIKE '4.0%' LIMIT 1;
    IF v_boot_id IS NOT NULL THEN
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, max_version, notes)
        VALUES (v_boot_id, 'JAVA', '17', '25', NULL, 'Java 17+ required. Java 25 LTS recommended for latest features.');
        INSERT INTO spring_boot_language_requirements (spring_boot_version_id, language, min_version, recommended_version, notes)
        VALUES (v_boot_id, 'KOTLIN', '2.0', '2.1', 'Kotlin 2.0+ required for Spring Boot 4.x. K2 compiler required.');
    END IF;
END $$;
