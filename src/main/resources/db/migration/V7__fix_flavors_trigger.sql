-- Fix flavors_update_trigger bytea conversion issue
-- The ::bytea cast doesn't work correctly with plain text
-- Use convert_to() function instead

CREATE OR REPLACE FUNCTION flavors_update_trigger() RETURNS trigger AS $$
BEGIN
    -- Update search vector
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.unique_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.display_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.pattern_name, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.tags, ' '), '')), 'B');

    -- Update timestamp
    NEW.updated_at := CURRENT_TIMESTAMP;

    -- Generate content hash for change detection
    -- Use convert_to() instead of ::bytea cast for proper text to bytea conversion
    NEW.content_hash := encode(sha256(convert_to(NEW.content, 'UTF8')), 'hex');

    RETURN NEW;
END
$$ LANGUAGE plpgsql;
