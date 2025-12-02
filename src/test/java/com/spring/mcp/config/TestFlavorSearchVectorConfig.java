package com.spring.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test configuration to add the search_vector column and trigger to the flavors table.
 * This is needed because JPA's ddl-auto doesn't create PostgreSQL-specific features like TSVECTOR.
 */
@TestConfiguration
@Profile("test")
public class TestFlavorSearchVectorConfig {

    private static final Logger log = LoggerFactory.getLogger(TestFlavorSearchVectorConfig.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // Run before TestDataBootstrapConfig
    public ApplicationRunner searchVectorInitializer(JdbcTemplate jdbcTemplate) {
        return args -> addSearchVectorColumn(jdbcTemplate);
    }

    private void addSearchVectorColumn(JdbcTemplate jdbcTemplate) {
        log.info("Adding search_vector column and trigger to flavors table for tests...");

        // Add search_vector column if it doesn't exist
        try {
            jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'flavors' AND column_name = 'search_vector'
                    ) THEN
                        ALTER TABLE flavors ADD COLUMN search_vector TSVECTOR;
                        CREATE INDEX IF NOT EXISTS idx_flavors_search ON flavors USING GIN(search_vector);
                    END IF;
                END
                $$;
                """);

            // Create the trigger function
            jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION flavors_update_trigger() RETURNS trigger AS $$
                BEGIN
                    NEW.search_vector :=
                        setweight(to_tsvector('english', COALESCE(NEW.unique_name, '')), 'A') ||
                        setweight(to_tsvector('english', COALESCE(NEW.display_name, '')), 'A') ||
                        setweight(to_tsvector('english', COALESCE(NEW.pattern_name, '')), 'B') ||
                        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
                        setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'C') ||
                        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.tags, ' '), '')), 'B');
                    NEW.updated_at := CURRENT_TIMESTAMP;
                    NEW.content_hash := encode(sha256(NEW.content::bytea), 'hex');
                    RETURN NEW;
                END
                $$ LANGUAGE plpgsql;
                """);

            // Create the trigger (drop first to avoid "already exists" errors)
            jdbcTemplate.execute("""
                DROP TRIGGER IF EXISTS trigger_flavors_update ON flavors;
                CREATE TRIGGER trigger_flavors_update
                    BEFORE INSERT OR UPDATE ON flavors
                    FOR EACH ROW EXECUTE FUNCTION flavors_update_trigger();
                """);

            log.info("Successfully added search_vector column and trigger to flavors table");
        } catch (Exception e) {
            log.error("Failed to add search_vector column and trigger", e);
            throw new RuntimeException("Failed to configure flavors search_vector", e);
        }
    }
}
