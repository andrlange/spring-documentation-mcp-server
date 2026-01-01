package com.spring.mcp.service.embedding;

import com.spring.mcp.config.EmbeddingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for hybrid search combining keyword (TSVECTOR) and semantic (vector) search.
 * Implements Reciprocal Rank Fusion (RRF) for combining results from both search types.
 *
 * The hybrid search formula:
 * score = alpha * keyword_score + (1 - alpha) * vector_score
 *
 * Where alpha = 0.3 gives 70% weight to semantic search and 30% to keyword search.
 *
 * @author Spring MCP Server
 * @version 1.6.0
 * @since 2026-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.features.embeddings.enabled", havingValue = "true")
public class HybridSearchService {

    private final EmbeddingService embeddingService;
    private final EmbeddingProperties properties;
    private final JdbcTemplate jdbcTemplate;

    // RRF constant (typically 60)
    private static final double RRF_K = 60.0;

    /**
     * Perform hybrid search on documentation content.
     *
     * @param query     search query text
     * @param limit     maximum results to return
     * @return list of entity IDs with their hybrid scores, ordered by relevance
     */
    public List<SearchResult> searchDocumentation(String query, int limit) {
        return hybridSearch(
                "documentation_content",
                "content_embedding",
                "content",
                "indexed_content",
                query,
                limit
        );
    }

    /**
     * Perform hybrid search on migration transformations.
     *
     * @param query     search query text
     * @param limit     maximum results to return
     * @return list of entity IDs with their hybrid scores
     */
    public List<SearchResult> searchTransformations(String query, int limit) {
        return hybridSearch(
                "migration_transformations",
                "transformation_embedding",
                "COALESCE(old_pattern, '') || ' ' || COALESCE(new_pattern, '') || ' ' || COALESCE(explanation, '')",
                null, // No TSVECTOR column, will use plain text search
                query,
                limit
        );
    }

    /**
     * Perform hybrid search on flavors.
     *
     * @param query     search query text
     * @param limit     maximum results to return
     * @return list of entity IDs with their hybrid scores
     */
    public List<SearchResult> searchFlavors(String query, int limit) {
        return hybridSearch(
                "flavors",
                "flavor_embedding",
                "COALESCE(display_name, '') || ' ' || COALESCE(description, '') || ' ' || COALESCE(content, '')",
                null,
                query,
                limit
        );
    }

    /**
     * Perform hybrid search on code examples.
     *
     * @param query     search query text
     * @param limit     maximum results to return
     * @return list of entity IDs with their hybrid scores
     */
    public List<SearchResult> searchCodeExamples(String query, int limit) {
        return hybridSearch(
                "code_examples",
                "example_embedding",
                "COALESCE(title, '') || ' ' || COALESCE(description, '') || ' ' || COALESCE(code_snippet, '')",
                null,
                query,
                limit
        );
    }

    /**
     * Generic hybrid search implementation.
     */
    private List<SearchResult> hybridSearch(
            String tableName,
            String embeddingColumn,
            String textColumn,
            String tsvectorColumn,
            String query,
            int limit
    ) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        double alpha = properties.getHybrid().getAlpha();
        double minSimilarity = properties.getHybrid().getMinSimilarity();

        log.debug("Performing hybrid search on {} with query='{}', alpha={}, limit={}",
                tableName, query.length() > 50 ? query.substring(0, 50) + "..." : query, alpha, limit);

        try {
            // Get keyword search results
            List<SearchResult> keywordResults = performKeywordSearch(tableName, textColumn, tsvectorColumn, query, limit * 2);

            // Get vector search results
            List<SearchResult> vectorResults = performVectorSearch(tableName, embeddingColumn, query, minSimilarity, limit * 2);

            // Combine results using Reciprocal Rank Fusion (RRF)
            List<SearchResult> hybridResults = combineWithRRF(keywordResults, vectorResults, alpha);

            // Return top N results
            return hybridResults.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Hybrid search failed on {}: {}", tableName, e.getMessage(), e);
            // Fall back to keyword search only
            return performKeywordSearch(tableName, textColumn, tsvectorColumn, query, limit);
        }
    }

    /**
     * Perform keyword search using TSVECTOR or LIKE.
     */
    private List<SearchResult> performKeywordSearch(
            String tableName,
            String textColumn,
            String tsvectorColumn,
            String query,
            int limit
    ) {
        String sql;
        if (tsvectorColumn != null) {
            // Use full-text search with TSVECTOR
            sql = String.format("""
                SELECT id, ts_rank(%s, plainto_tsquery('english', ?)) as score
                FROM %s
                WHERE %s @@ plainto_tsquery('english', ?)
                ORDER BY score DESC
                LIMIT ?
                """, tsvectorColumn, tableName, tsvectorColumn);
        } else {
            // Fall back to ILIKE search
            sql = String.format("""
                SELECT id, 1.0 as score
                FROM %s
                WHERE (%s) ILIKE ?
                ORDER BY id
                LIMIT ?
                """, tableName, textColumn);
            query = "%" + query + "%";
        }

        try {
            final String searchQuery = query;
            List<SearchResult> results = new ArrayList<>();

            if (tsvectorColumn != null) {
                jdbcTemplate.query(sql, rs -> {
                    results.add(new SearchResult(rs.getLong("id"), rs.getDouble("score")));
                }, searchQuery, searchQuery, limit);
            } else {
                jdbcTemplate.query(sql, rs -> {
                    results.add(new SearchResult(rs.getLong("id"), rs.getDouble("score")));
                }, searchQuery, limit);
            }

            return results;
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Perform vector similarity search using pgvector.
     */
    private List<SearchResult> performVectorSearch(
            String tableName,
            String embeddingColumn,
            String query,
            double minSimilarity,
            int limit
    ) {
        if (!embeddingService.isAvailable()) {
            log.debug("Embedding service not available, skipping vector search");
            return List.of();
        }

        try {
            // Generate query embedding
            float[] queryEmbedding = embeddingService.embed(query);
            if (queryEmbedding == null || queryEmbedding.length == 0) {
                return List.of();
            }

            // Convert to PostgreSQL vector format
            String vectorString = floatArrayToVectorString(queryEmbedding);

            // Perform vector similarity search
            String sql = String.format("""
                SELECT id, 1 - (%s <=> ?::vector) as similarity
                FROM %s
                WHERE %s IS NOT NULL
                AND 1 - (%s <=> ?::vector) >= ?
                ORDER BY %s <=> ?::vector
                LIMIT ?
                """, embeddingColumn, tableName, embeddingColumn, embeddingColumn, embeddingColumn);

            List<SearchResult> results = new ArrayList<>();
            jdbcTemplate.query(sql, rs -> {
                results.add(new SearchResult(rs.getLong("id"), rs.getDouble("similarity")));
            }, vectorString, vectorString, minSimilarity, vectorString, limit);

            return results;
        } catch (Exception e) {
            log.warn("Vector search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Combine results using Reciprocal Rank Fusion (RRF).
     * RRF score = sum(1 / (k + rank)) for each result set.
     */
    private List<SearchResult> combineWithRRF(
            List<SearchResult> keywordResults,
            List<SearchResult> vectorResults,
            double alpha
    ) {
        Map<Long, Double> combinedScores = new HashMap<>();

        // Calculate RRF scores for keyword results
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult result = keywordResults.get(i);
            double rrfScore = alpha / (RRF_K + i + 1);
            combinedScores.merge(result.id(), rrfScore, Double::sum);
        }

        // Calculate RRF scores for vector results
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult result = vectorResults.get(i);
            double rrfScore = (1 - alpha) / (RRF_K + i + 1);
            combinedScores.merge(result.id(), rrfScore, Double::sum);
        }

        // Sort by combined score
        return combinedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(e -> new SearchResult(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Convert float array to PostgreSQL vector string format.
     */
    private String floatArrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Search result record.
     */
    public record SearchResult(Long id, double score) {}
}
