-- ============================================================
-- V19: Fix Java 25 JEP Numbers and Feature Status
-- ============================================================
-- This migration fixes incorrect JEP numbers and feature status
-- for Java 25 language features based on the official JDK 25 release:
-- https://openjdk.org/projects/jdk/25/
--
-- Corrections:
-- 1. Flexible Constructor Bodies: JEP 501 -> JEP 513 (Standard)
-- 2. Scoped Values: JEP 502 -> JEP 506 (finalized, not preview)
-- 3. Structured Concurrency: 4th Preview -> 5th Preview
-- 4. Module Import Declarations: 3rd Preview -> Standard (finalized)
-- ============================================================

-- Fix Flexible Constructor Bodies: Wrong JEP 501 -> Correct JEP 513
UPDATE language_features
SET jep_number = '513'
WHERE feature_name = 'Flexible Constructor Bodies (Standard)'
  AND jep_number = '501'
  AND language_version_id = (
    SELECT id FROM language_versions
    WHERE language = 'JAVA' AND version = '25'
  );

-- Fix Scoped Values: JEP 502 -> JEP 506 and change from 4th Preview to Standard (finalized)
UPDATE language_features
SET feature_name = 'Scoped Values (Standard)',
    status = 'NEW',
    jep_number = '506',
    description = 'Scoped values finalized - share immutable data within and across threads'
WHERE feature_name = 'Scoped Values (4th Preview)'
  AND language_version_id = (
    SELECT id FROM language_versions
    WHERE language = 'JAVA' AND version = '25'
  );

-- Fix Structured Concurrency: 4th Preview -> 5th Preview
UPDATE language_features
SET feature_name = 'Structured Concurrency (5th Preview)',
    description = 'Structured concurrency fifth preview refinement'
WHERE feature_name = 'Structured Concurrency (4th Preview)'
  AND jep_number = '505'
  AND language_version_id = (
    SELECT id FROM language_versions
    WHERE language = 'JAVA' AND version = '25'
  );

-- Fix Module Import Declarations: 3rd Preview -> Standard (finalized)
UPDATE language_features
SET feature_name = 'Module Import Declarations (Standard)',
    status = 'NEW',
    description = 'Module import declarations finalized - import all packages from a module'
WHERE feature_name = 'Module Import Declarations (3rd Preview)'
  AND jep_number = '511'
  AND language_version_id = (
    SELECT id FROM language_versions
    WHERE language = 'JAVA' AND version = '25'
  );

-- Add missing Java 25 features
-- JEP 512: Compact Source Files and Instance Main Methods (finalized from Implicitly Declared Classes)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level)
SELECT
    (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'),
    'Compact Source Files and Instance Main Methods (Standard)',
    'NEW',
    'Syntax',
    '512',
    'Simplified main class declarations finalized - void main() without class declaration',
    'MEDIUM'
WHERE NOT EXISTS (
    SELECT 1 FROM language_features
    WHERE jep_number = '512'
      AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25')
);

-- JEP 510: Key Derivation Function API (new feature)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level)
SELECT
    (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'),
    'Key Derivation Function API',
    'NEW',
    'Security',
    '510',
    'API for Key Derivation Functions like HKDF for secure key generation',
    'MEDIUM'
WHERE NOT EXISTS (
    SELECT 1 FROM language_features
    WHERE jep_number = '510'
      AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25')
);

-- JEP 503: Remove the 32-bit x86 Port (removal)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level)
SELECT
    (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'),
    '32-bit x86 Port Removed',
    'REMOVED',
    'Platform',
    '503',
    '32-bit x86 port code and build support removed from OpenJDK',
    'LOW'
WHERE NOT EXISTS (
    SELECT 1 FROM language_features
    WHERE jep_number = '503'
      AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25')
);

-- JEP 519: Compact Object Headers (new feature)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level)
SELECT
    (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'),
    'Compact Object Headers',
    'NEW',
    'Performance',
    '519',
    'Reduce object header size from 96-128 bits to 64 bits for better memory efficiency',
    'HIGH'
WHERE NOT EXISTS (
    SELECT 1 FROM language_features
    WHERE jep_number = '519'
      AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25')
);

-- JEP 470: PEM Encodings of Cryptographic Objects (Preview)
INSERT INTO language_features (language_version_id, feature_name, status, category, jep_number, description, impact_level)
SELECT
    (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25'),
    'PEM Encodings of Cryptographic Objects (Preview)',
    'PREVIEW',
    'Security',
    '470',
    'API for encoding and decoding cryptographic objects in PEM format',
    'LOW'
WHERE NOT EXISTS (
    SELECT 1 FROM language_features
    WHERE jep_number = '470'
      AND language_version_id = (SELECT id FROM language_versions WHERE language = 'JAVA' AND version = '25')
);
