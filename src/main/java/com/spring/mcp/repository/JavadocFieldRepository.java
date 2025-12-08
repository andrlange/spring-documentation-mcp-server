package com.spring.mcp.repository;

import com.spring.mcp.model.entity.JavadocField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for JavadocField entities.
 * Manages field-level Javadoc documentation.
 */
@Repository
public interface JavadocFieldRepository extends JpaRepository<JavadocField, Long> {

    /**
     * Find all fields for a class.
     */
    List<JavadocField> findByJavadocClassId(Long classId);

    /**
     * Find a field by name in a class.
     */
    Optional<JavadocField> findByJavadocClassIdAndName(Long classId, String name);

    /**
     * Find fields by name for a specific class FQCN.
     */
    @Query("""
        SELECT jf FROM JavadocField jf
        JOIN jf.javadocClass jc
        WHERE jc.fqcn = :fqcn
        AND jf.name = :fieldName
        """)
    Optional<JavadocField> findByClassFqcnAndName(
            @Param("fqcn") String fqcn,
            @Param("fieldName") String fieldName);

    /**
     * Find deprecated fields in a class.
     */
    List<JavadocField> findByJavadocClassIdAndDeprecatedTrue(Long classId);

    /**
     * Find constant fields (static final).
     */
    @Query("""
        SELECT jf FROM JavadocField jf
        WHERE jf.javadocClass.id = :classId
        AND jf.modifiers LIKE '%static%'
        AND jf.modifiers LIKE '%final%'
        """)
    List<JavadocField> findConstantFields(@Param("classId") Long classId);

    /**
     * Find fields by type.
     */
    List<JavadocField> findByJavadocClassIdAndType(Long classId, String type);

    /**
     * Search fields by name pattern.
     */
    @Query("""
        SELECT jf FROM JavadocField jf
        JOIN jf.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jf.name LIKE %:pattern%
        ORDER BY jf.name
        """)
    List<JavadocField> searchByNamePattern(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("pattern") String pattern);

    /**
     * Count fields for a class.
     */
    long countByJavadocClassId(Long classId);

    /**
     * Count fields for a library version.
     */
    @Query("""
        SELECT COUNT(jf) FROM JavadocField jf
        JOIN jf.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        """)
    long countByLibraryAndVersion(
            @Param("libraryName") String libraryName,
            @Param("version") String version);
}
