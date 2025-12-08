package com.spring.mcp.repository;

import com.spring.mcp.model.entity.JavadocPackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for JavadocPackage entities.
 * Manages package-level Javadoc documentation.
 */
@Repository
public interface JavadocPackageRepository extends JpaRepository<JavadocPackage, Long> {

    /**
     * Find all packages for a library and version.
     */
    List<JavadocPackage> findByLibraryNameAndVersion(String libraryName, String version);

    /**
     * Find a specific package by library, version, and package name.
     */
    Optional<JavadocPackage> findByLibraryNameAndVersionAndPackageName(
            String libraryName, String version, String packageName);

    /**
     * Check if a package exists.
     */
    boolean existsByLibraryNameAndVersionAndPackageName(
            String libraryName, String version, String packageName);

    /**
     * Find all packages for a library (all versions).
     */
    List<JavadocPackage> findByLibraryName(String libraryName);

    /**
     * Find distinct library names.
     */
    @Query("SELECT DISTINCT jp.libraryName FROM JavadocPackage jp ORDER BY jp.libraryName")
    List<String> findDistinctLibraryNames();

    /**
     * Find distinct versions for a library.
     */
    @Query("SELECT DISTINCT jp.version FROM JavadocPackage jp WHERE jp.libraryName = :libraryName ORDER BY jp.version DESC")
    List<String> findVersionsByLibraryName(@Param("libraryName") String libraryName);

    /**
     * Count packages for a library version.
     */
    long countByLibraryNameAndVersion(String libraryName, String version);

    /**
     * Delete all packages for a library version (used for re-sync).
     */
    @Modifying
    @Query("DELETE FROM JavadocPackage jp WHERE jp.libraryName = :libraryName AND jp.version = :version")
    void deleteByLibraryNameAndVersion(@Param("libraryName") String libraryName, @Param("version") String version);

    /**
     * Check if any documentation exists for a library version.
     */
    boolean existsByLibraryNameAndVersion(String libraryName, String version);

    /**
     * Search packages by name pattern.
     */
    @Query("SELECT jp FROM JavadocPackage jp WHERE jp.libraryName = :libraryName AND jp.version = :version AND jp.packageName LIKE %:pattern% ORDER BY jp.packageName")
    List<JavadocPackage> searchByPackageName(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("pattern") String pattern);

    /**
     * Full-text search on packages using tsvector.
     */
    @Query(value = """
        SELECT jp.* FROM javadoc_packages jp
        WHERE jp.library_name = :libraryName
        AND jp.version = :version
        AND jp.indexed_content @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank_cd(jp.indexed_content, plainto_tsquery('english', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<JavadocPackage> searchByKeyword(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("query") String query,
            @Param("limit") int limit);

    /**
     * Find packages with pagination.
     */
    Page<JavadocPackage> findByLibraryNameAndVersion(String libraryName, String version, Pageable pageable);

}
