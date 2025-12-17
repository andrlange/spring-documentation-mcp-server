package com.spring.mcp.repository;

import com.spring.mcp.model.entity.JavadocClass;
import com.spring.mcp.model.enums.JavadocClassKind;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for JavadocClass entities.
 * Manages class/interface/enum documentation with full-text search capabilities.
 */
@Repository
public interface JavadocClassRepository extends JpaRepository<JavadocClass, Long> {

    /**
     * Find a class by fully qualified class name (any version).
     */
    List<JavadocClass> findByFqcn(String fqcn);

    /**
     * Find classes in a package.
     */
    List<JavadocClass> findByJavadocPackageId(Long packageId);

    /**
     * Find a specific class by library, version, and FQCN.
     */
    @Query("""
        SELECT jc FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.fqcn = :fqcn
        """)
    Optional<JavadocClass> findByLibraryVersionAndFqcn(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("fqcn") String fqcn);

    /**
     * Find a specific class by library, version, and FQCN with all members eagerly loaded.
     * Use this when you need to access methods, fields, and constructors.
     */
    @Query("""
        SELECT DISTINCT jc FROM JavadocClass jc
        JOIN FETCH jc.javadocPackage jp
        LEFT JOIN FETCH jc.methods
        LEFT JOIN FETCH jc.fields
        LEFT JOIN FETCH jc.constructors
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.fqcn = :fqcn
        """)
    Optional<JavadocClass> findByLibraryVersionAndFqcnWithMembers(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("fqcn") String fqcn);

    /**
     * Find classes by simple name (any version).
     */
    List<JavadocClass> findBySimpleName(String simpleName);

    /**
     * Find classes by simple name for a specific library and version.
     */
    @Query("""
        SELECT jc FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.simpleName = :simpleName
        """)
    List<JavadocClass> findByLibraryVersionAndSimpleName(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("simpleName") String simpleName);

    /**
     * Find classes by kind (CLASS, INTERFACE, ENUM, etc.).
     */
    List<JavadocClass> findByJavadocPackageIdAndKind(Long packageId, JavadocClassKind kind);

    /**
     * Find deprecated classes.
     */
    @Query("""
        SELECT jc FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.deprecated = true
        """)
    List<JavadocClass> findDeprecatedClasses(
            @Param("libraryName") String libraryName,
            @Param("version") String version);

    /**
     * Full-text search on classes using tsvector.
     * Note: This returns classes without eager-loaded package - use searchByKeywordWithPackage for that.
     */
    @Query(value = """
        SELECT jc.* FROM javadoc_classes jc
        JOIN javadoc_packages jp ON jc.package_id = jp.id
        WHERE jp.library_name = :libraryName
        AND jp.version = :version
        AND jc.indexed_content @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank_cd(jc.indexed_content, plainto_tsquery('english', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<JavadocClass> searchByKeyword(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("query") String query,
            @Param("limit") int limit);

    /**
     * Full-text search on classes with eager-loaded package.
     * Uses JPQL with LIKE for compatibility and JOIN FETCH for package.
     */
    @Query("""
        SELECT DISTINCT jc FROM JavadocClass jc
        JOIN FETCH jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND (LOWER(jc.fqcn) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(jc.simpleName) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(jc.summary) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(jc.description) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY jc.simpleName
        """)
    List<JavadocClass> searchByKeywordWithPackage(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("query") String query,
            Pageable pageable);

    /**
     * Full-text search across all libraries and versions.
     * Note: This returns classes without eager-loaded package - use searchByKeywordGlobalWithPackage for that.
     */
    @Query(value = """
        SELECT jc.* FROM javadoc_classes jc
        JOIN javadoc_packages jp ON jc.package_id = jp.id
        WHERE jc.indexed_content @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank_cd(jc.indexed_content, plainto_tsquery('english', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<JavadocClass> searchByKeywordGlobal(
            @Param("query") String query,
            @Param("limit") int limit);

    /**
     * Full-text search across all libraries and versions with eager-loaded package.
     * Uses JPQL with LIKE for compatibility and JOIN FETCH for package.
     */
    @Query("""
        SELECT DISTINCT jc FROM JavadocClass jc
        JOIN FETCH jc.javadocPackage jp
        WHERE LOWER(jc.fqcn) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(jc.simpleName) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(jc.summary) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(jc.description) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY jc.simpleName
        """)
    List<JavadocClass> searchByKeywordGlobalWithPackage(
            @Param("query") String query,
            Pageable pageable);

    /**
     * Search by class name pattern.
     */
    @Query("""
        SELECT jc FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND (jc.fqcn LIKE %:pattern% OR jc.simpleName LIKE %:pattern%)
        ORDER BY jc.simpleName
        """)
    List<JavadocClass> searchByNamePattern(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("pattern") String pattern);

    /**
     * Find classes that implement a specific interface.
     */
    @Query(value = """
        SELECT jc.* FROM javadoc_classes jc
        JOIN javadoc_packages jp ON jc.package_id = jp.id
        WHERE jp.library_name = :libraryName
        AND jp.version = :version
        AND :interfaceName = ANY(jc.interfaces)
        """, nativeQuery = true)
    List<JavadocClass> findByImplementedInterface(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("interfaceName") String interfaceName);

    /**
     * Find classes with a specific annotation.
     */
    @Query(value = """
        SELECT jc.* FROM javadoc_classes jc
        JOIN javadoc_packages jp ON jc.package_id = jp.id
        WHERE jp.library_name = :libraryName
        AND jp.version = :version
        AND :annotation = ANY(jc.annotations)
        """, nativeQuery = true)
    List<JavadocClass> findByAnnotation(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("annotation") String annotation);

    /**
     * Count classes for a library version.
     */
    @Query("""
        SELECT COUNT(jc) FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        """)
    long countByLibraryAndVersion(
            @Param("libraryName") String libraryName,
            @Param("version") String version);

    /**
     * Count classes by kind for a library version.
     */
    @Query("""
        SELECT COUNT(jc) FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.kind = :kind
        """)
    long countByLibraryVersionAndKind(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("kind") JavadocClassKind kind);

    /**
     * Find classes with pagination.
     */
    @Query("""
        SELECT jc FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        ORDER BY jc.fqcn
        """)
    Page<JavadocClass> findByLibraryAndVersion(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            Pageable pageable);

    /**
     * Find a class with all its methods, fields, and constructors eagerly loaded.
     */
    @Query("""
        SELECT DISTINCT jc FROM JavadocClass jc
        LEFT JOIN FETCH jc.methods
        LEFT JOIN FETCH jc.fields
        LEFT JOIN FETCH jc.constructors
        WHERE jc.id = :id
        """)
    Optional<JavadocClass> findByIdWithMembers(@Param("id") Long id);

    /**
     * Find classes by package ID.
     */
    List<JavadocClass> findByJavadocPackage_Id(Long packageId);

    /**
     * Alias for findByJavadocPackage_Id for convenience.
     */
    default List<JavadocClass> findByPackageId(Long packageId) {
        return findByJavadocPackage_Id(packageId);
    }

    /**
     * Check if a class exists for a specific library, version, and FQCN.
     */
    @Query("""
        SELECT CASE WHEN COUNT(jc) > 0 THEN true ELSE false END
        FROM JavadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.fqcn = :fqcn
        """)
    boolean existsByLibraryVersionAndFqcn(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("fqcn") String fqcn);

}
