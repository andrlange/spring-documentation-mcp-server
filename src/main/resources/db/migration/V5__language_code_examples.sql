-- V5: Add code_example column to language_features table
-- This column stores a simple code example for each language feature

ALTER TABLE language_features
ADD COLUMN code_example TEXT;

-- Add comment for documentation
COMMENT ON COLUMN language_features.code_example IS 'Simple code example demonstrating the language feature';
