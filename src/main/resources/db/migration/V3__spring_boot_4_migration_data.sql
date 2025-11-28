-- ============================================
-- Spring Boot 4.0 Migration Knowledge
-- Source: OpenRewrite recipes + practical experience
-- ============================================

-- Insert Spring Boot 4.0 migration recipe
INSERT INTO migration_recipes (name, display_name, description, from_project, from_version_min, to_version, source_url, license)
VALUES (
    'org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0',
    'Migrate to Spring Boot 4.0',
    'Comprehensive migration guide from Spring Boot 3.x to 4.0. Includes dependency updates, import changes, property migrations, and API replacements.',
    'spring-boot',
    '3.0.0',
    '4.0.0',
    'https://docs.openrewrite.org/recipes/java/spring/boot4/upgradespringboot_4_0-community-edition',
    'Moderne Source Available'
);

-- Insert transformations for Spring Boot 4.0 recipe
INSERT INTO migration_transformations (recipe_id, transformation_type, category, subcategory, old_pattern, new_pattern, file_pattern, breaking_change, severity, priority, explanation, code_example, additional_steps, tags)
SELECT r.id, t.*
FROM migration_recipes r
CROSS JOIN (VALUES
    -- BUILD: Gradle version requirement
    ('BUILD', 'gradle', 'wrapper',
     'distributionUrl=https://services.gradle.org/distributions/gradle-8.(1[0-3]|[0-9])-',
     'distributionUrl=https://services.gradle.org/distributions/gradle-8.14-bin.zip',
     'gradle/wrapper/gradle-wrapper.properties',
     true, 'CRITICAL', 100,
     'Spring Boot 4.0 requires Gradle 8.14 or later. Earlier versions will fail with: "Spring Boot plugin requires Gradle 8.x (8.14 or later)"',
     E'# Before (gradle-wrapper.properties)\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.11-bin.zip\n\n# After\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.14-bin.zip',
     'Run: ./gradlew wrapper --gradle-version=8.14',
     ARRAY['gradle', 'build', 'wrapper', 'version']),

    -- DEPENDENCY: Flyway starter
    ('DEPENDENCY', 'database', 'flyway',
     'org.flywaydb:flyway-core',
     'org.springframework.boot:spring-boot-starter-flyway',
     'build.gradle',
     true, 'CRITICAL', 95,
     'Flyway auto-configuration moved to separate starter in Spring Boot 4.0. Using flyway-core alone will not trigger auto-configuration.',
     E'// Before (build.gradle)\nimplementation ''org.flywaydb:flyway-core''\nimplementation ''org.flywaydb:flyway-database-postgresql''\n\n// After\nimplementation ''org.springframework.boot:spring-boot-starter-flyway''\nimplementation ''org.flywaydb:flyway-database-postgresql''',
     'Keep flyway-database-postgresql for PostgreSQL support',
     ARRAY['flyway', 'database', 'migration', 'dependency']),

    -- IMPORT: Health classes moved
    ('IMPORT', 'actuator', 'health',
     'org.springframework.boot.actuate.health.Health',
     'org.springframework.boot.health.contributor.Health',
     '*.java',
     true, 'ERROR', 90,
     'Health classes relocated from spring-boot-actuator to spring-boot-health module in Spring Boot 4.0.',
     E'// Before\nimport org.springframework.boot.actuate.health.Health;\nimport org.springframework.boot.actuate.health.HealthIndicator;\n\n// After\nimport org.springframework.boot.health.contributor.Health;\nimport org.springframework.boot.health.contributor.HealthIndicator;',
     NULL,
     ARRAY['actuator', 'health', 'import', 'package']),

    -- IMPORT: HealthIndicator moved
    ('IMPORT', 'actuator', 'health',
     'org.springframework.boot.actuate.health.HealthIndicator',
     'org.springframework.boot.health.contributor.HealthIndicator',
     '*.java',
     true, 'ERROR', 90,
     'HealthIndicator interface relocated to spring-boot-health module.',
     NULL, NULL,
     ARRAY['actuator', 'health', 'import', 'indicator']),

    -- IMPORT: AbstractHealthIndicator moved
    ('IMPORT', 'actuator', 'health',
     'org.springframework.boot.actuate.health.AbstractHealthIndicator',
     'org.springframework.boot.health.contributor.AbstractHealthIndicator',
     '*.java',
     true, 'ERROR', 90,
     'AbstractHealthIndicator base class relocated to spring-boot-health module.',
     NULL, NULL,
     ARRAY['actuator', 'health', 'import', 'abstract']),

    -- TEMPLATE: Thymeleaf #request removed
    ('TEMPLATE', 'thymeleaf', 'request',
     '#request',
     'Use @ControllerAdvice with @ModelAttribute or pass data explicitly from controller',
     '*.html',
     true, 'ERROR', 85,
     'The #request object is no longer available in Thymeleaf templates in Spring Boot 4.0 due to security improvements. Request data must now be passed explicitly from the controller.',
     E'<!-- Before (NOT WORKING) -->\n<span th:text="${#request.requestURI}">URI</span>\n\n<!-- After - Controller -->\n@GetMapping\npublic String page(Model model, HttpServletRequest request) {\n    model.addAttribute("currentUri", request.getRequestURI());\n    return "page";\n}\n\n<!-- After - Template -->\n<span th:text="${currentUri}">URI</span>',
     'Create a @ControllerAdvice class with @ModelAttribute methods for commonly needed request data',
     ARRAY['thymeleaf', 'template', 'request', 'security']),

    -- CODE: Spring Security 7.0 changes
    ('CODE', 'security', 'configuration',
     'authorizeHttpRequests()',
     'authorizeHttpRequests(authorize -> authorize...)',
     '*.java',
     true, 'ERROR', 88,
     'Spring Security 7.0 (included in Spring Boot 4.0) requires lambda DSL for security configuration. Method chaining without lambdas is deprecated.',
     E'// Before (deprecated)\nhttp.authorizeHttpRequests()\n    .requestMatchers("/public/**").permitAll()\n    .anyRequest().authenticated();\n\n// After (required)\nhttp.authorizeHttpRequests(authorize -> authorize\n    .requestMatchers("/public/**").permitAll()\n    .anyRequest().authenticated()\n);',
     NULL,
     ARRAY['security', 'configuration', 'lambda', 'dsl']),

    -- ANNOTATION: @MockBean removed
    ('ANNOTATION', 'test', 'mock',
     '@MockBean',
     '@MockitoBean',
     '*.java',
     true, 'ERROR', 87,
     '@MockBean annotation has been removed in Spring Boot 4.0. Use @MockitoBean from spring-boot-test-autoconfigure instead.',
     E'// Before\nimport org.springframework.boot.test.mock.mockito.MockBean;\n\n@MockBean\nprivate MyService myService;\n\n// After\nimport org.springframework.test.context.bean.override.mockito.MockitoBean;\n\n@MockitoBean\nprivate MyService myService;',
     'Update test dependencies if needed',
     ARRAY['test', 'mock', 'mockbean', 'annotation']),

    -- ANNOTATION: @SpyBean removed
    ('ANNOTATION', 'test', 'spy',
     '@SpyBean',
     '@MockitoSpyBean',
     '*.java',
     true, 'ERROR', 87,
     '@SpyBean annotation has been removed in Spring Boot 4.0. Use @MockitoSpyBean from spring-boot-test-autoconfigure instead.',
     E'// Before\nimport org.springframework.boot.test.mock.mockito.SpyBean;\n\n@SpyBean\nprivate MyService myService;\n\n// After\nimport org.springframework.test.context.bean.override.mockito.MockitoSpyBean;\n\n@MockitoSpyBean\nprivate MyService myService;',
     NULL,
     ARRAY['test', 'spy', 'spybean', 'annotation']),

    -- PROPERTY: Server SSL configuration
    ('PROPERTY', 'server', 'ssl',
     'server.ssl.enabled',
     'server.ssl.bundle',
     'application.yml',
     true, 'WARNING', 75,
     'SSL configuration has been refactored to use SSL bundles in Spring Boot 4.0 for better certificate management.',
     E'# Before\nserver:\n  ssl:\n    enabled: true\n    key-store: classpath:keystore.p12\n    key-store-password: secret\n\n# After\nspring:\n  ssl:\n    bundle:\n      jks:\n        server:\n          keystore:\n            location: classpath:keystore.p12\n            password: secret\nserver:\n  ssl:\n    bundle: server',
     'Review SSL bundle documentation for advanced configuration',
     ARRAY['ssl', 'server', 'configuration', 'bundle']),

    -- BUILD: Java version requirement
    ('BUILD', 'java', 'version',
     'sourceCompatibility = ''17''',
     'sourceCompatibility = ''21''',
     'build.gradle',
     true, 'CRITICAL', 99,
     'Spring Boot 4.0 requires Java 21 or later. Java 17 is no longer supported.',
     E'// Before (build.gradle)\njava {\n    sourceCompatibility = ''17''\n}\n\n// After\njava {\n    sourceCompatibility = ''21''\n}',
     'Upgrade JDK to 21 or later before migrating',
     ARRAY['java', 'version', 'jdk', 'build']),

    -- DEPENDENCY: Jakarta namespace
    ('DEPENDENCY', 'jakarta', 'namespace',
     'javax.servlet',
     'jakarta.servlet',
     '*.java',
     true, 'ERROR', 92,
     'All javax.* packages have been replaced with jakarta.* namespace. This change was already in Spring Boot 3.0 but still common source of errors.',
     E'// Before\nimport javax.servlet.http.HttpServletRequest;\n\n// After\nimport jakarta.servlet.http.HttpServletRequest;',
     'Use IDE refactoring tools for batch replacement',
     ARRAY['jakarta', 'javax', 'namespace', 'import']),

    -- CODE: WebMvcConfigurer changes
    ('CODE', 'web', 'mvc',
     'WebMvcConfigurerAdapter',
     'WebMvcConfigurer',
     '*.java',
     true, 'ERROR', 80,
     'WebMvcConfigurerAdapter was removed. Implement WebMvcConfigurer interface directly.',
     E'// Before\npublic class WebConfig extends WebMvcConfigurerAdapter {\n    @Override\n    public void addViewControllers(ViewControllerRegistry registry) {\n        ...\n    }\n}\n\n// After\n@Configuration\npublic class WebConfig implements WebMvcConfigurer {\n    @Override\n    public void addViewControllers(ViewControllerRegistry registry) {\n        ...\n    }\n}',
     NULL,
     ARRAY['webmvc', 'configuration', 'adapter']),

    -- PROPERTY: Actuator base path
    ('PROPERTY', 'actuator', 'endpoint',
     'management.endpoints.web.base-path=/actuator',
     'management.endpoints.web.base-path=/management',
     'application.yml',
     false, 'INFO', 50,
     'Consider changing actuator base path from /actuator to /management for clearer separation (optional).',
     NULL, NULL,
     ARRAY['actuator', 'endpoint', 'path'])

) AS t(transformation_type, category, subcategory, old_pattern, new_pattern, file_pattern, breaking_change, severity, priority, explanation, code_example, additional_steps, tags)
WHERE r.name = 'org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0';

-- Insert Spring Security 7.0 migration recipe
INSERT INTO migration_recipes (name, display_name, description, from_project, from_version_min, to_version, source_url, license)
VALUES (
    'org.openrewrite.java.spring.security7.UpgradeSpringSecurity_7_0',
    'Migrate to Spring Security 7.0',
    'Migration guide from Spring Security 6.x to 7.0. Includes lambda DSL requirements and deprecated API removals.',
    'spring-security',
    '6.0.0',
    '7.0.0',
    'https://docs.openrewrite.org/recipes/java/spring/security7',
    'Moderne Source Available'
);

-- Insert transformations for Spring Security 7.0
INSERT INTO migration_transformations (recipe_id, transformation_type, category, subcategory, old_pattern, new_pattern, file_pattern, breaking_change, severity, priority, explanation, code_example, additional_steps, tags)
SELECT r.id, t.*
FROM migration_recipes r
CROSS JOIN (VALUES
    -- CSRF configuration changes
    ('CODE', 'csrf', 'configuration',
     'csrf().disable()',
     'csrf(csrf -> csrf.disable())',
     '*.java',
     true, 'ERROR', 88,
     'CSRF configuration requires lambda DSL in Spring Security 7.0.',
     E'// Before\nhttp.csrf().disable();\n\n// After\nhttp.csrf(csrf -> csrf.disable());',
     NULL,
     ARRAY['security', 'csrf', 'lambda']),

    -- Session management changes
    ('CODE', 'session', 'management',
     'sessionManagement().sessionCreationPolicy()',
     'sessionManagement(session -> session.sessionCreationPolicy())',
     '*.java',
     true, 'ERROR', 87,
     'Session management configuration requires lambda DSL.',
     E'// Before\nhttp.sessionManagement()\n    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);\n\n// After\nhttp.sessionManagement(session -> session\n    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)\n);',
     NULL,
     ARRAY['security', 'session', 'lambda']),

    -- Form login changes
    ('CODE', 'auth', 'formlogin',
     'formLogin().loginPage(',
     'formLogin(form -> form.loginPage(',
     '*.java',
     true, 'ERROR', 86,
     'Form login configuration requires lambda DSL.',
     E'// Before\nhttp.formLogin()\n    .loginPage("/login")\n    .permitAll();\n\n// After\nhttp.formLogin(form -> form\n    .loginPage("/login")\n    .permitAll()\n);',
     NULL,
     ARRAY['security', 'formlogin', 'lambda'])

) AS t(transformation_type, category, subcategory, old_pattern, new_pattern, file_pattern, breaking_change, severity, priority, explanation, code_example, additional_steps, tags)
WHERE r.name = 'org.openrewrite.java.spring.security7.UpgradeSpringSecurity_7_0';

-- Insert version compatibility data for Spring Boot 4.0
INSERT INTO version_compatibility (spring_boot_version, dependency_group, dependency_artifact, compatible_version, notes, verified)
VALUES
    ('4.0.0', 'org.springframework.security', 'spring-security-core', '7.0.0', 'Spring Security 7.0 required for Spring Boot 4.0', true),
    ('4.0.0', 'org.springframework', 'spring-framework', '7.0.0', 'Spring Framework 7.0 required for Spring Boot 4.0', true),
    ('4.0.0', 'org.flywaydb', 'flyway-core', '10.0.0', 'Flyway 10.x compatible with Spring Boot 4.0', true),
    ('4.0.0', 'org.hibernate.orm', 'hibernate-core', '7.0.0', 'Hibernate 7.0 recommended for Spring Boot 4.0', false),
    ('4.0.0', 'org.thymeleaf', 'thymeleaf', '3.2.0', 'Thymeleaf 3.2.x compatible', true),
    ('4.0.0', 'org.postgresql', 'postgresql', '42.7.0', 'PostgreSQL JDBC driver 42.7.x compatible', true);

-- Insert deprecation replacements
INSERT INTO deprecation_replacements (deprecated_class, deprecated_method, replacement_class, replacement_method, deprecated_since, removed_in, migration_notes, code_before, code_after, project_slug)
VALUES
    ('org.springframework.boot.actuate.health.Health', NULL, 'org.springframework.boot.health.contributor.Health', NULL, '3.4.0', '4.0.0',
     'Health classes moved to spring-boot-health module for better modularity',
     'import org.springframework.boot.actuate.health.Health;',
     'import org.springframework.boot.health.contributor.Health;',
     'spring-boot'),

    ('org.springframework.boot.actuate.health.HealthIndicator', NULL, 'org.springframework.boot.health.contributor.HealthIndicator', NULL, '3.4.0', '4.0.0',
     'HealthIndicator interface moved to spring-boot-health module',
     'import org.springframework.boot.actuate.health.HealthIndicator;',
     'import org.springframework.boot.health.contributor.HealthIndicator;',
     'spring-boot'),

    ('org.springframework.boot.test.mock.mockito.MockBean', NULL, 'org.springframework.test.context.bean.override.mockito.MockitoBean', NULL, '3.4.0', '4.0.0',
     '@MockBean annotation replaced with @MockitoBean for clearer naming',
     '@MockBean\nprivate MyService service;',
     '@MockitoBean\nprivate MyService service;',
     'spring-boot'),

    ('org.springframework.boot.test.mock.mockito.SpyBean', NULL, 'org.springframework.test.context.bean.override.mockito.MockitoSpyBean', NULL, '3.4.0', '4.0.0',
     '@SpyBean annotation replaced with @MockitoSpyBean',
     '@SpyBean\nprivate MyService service;',
     '@MockitoSpyBean\nprivate MyService service;',
     'spring-boot');
