package com.spring.mcp.repository;

import com.spring.mcp.model.entity.JavadocConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for JavadocConstructor entities.
 * Manages constructor-level Javadoc documentation.
 */
@Repository
public interface JavadocConstructorRepository extends JpaRepository<JavadocConstructor, Long> {

    /**
     * Find all constructors for a class.
     */
    List<JavadocConstructor> findByJavadocClassId(Long classId);

    /**
     * Find deprecated constructors in a class.
     */
    List<JavadocConstructor> findByJavadocClassIdAndDeprecatedTrue(Long classId);

    /**
     * Find the default (no-arg) constructor for a class.
     */
    @Query(value = """
        SELECT jcon.* FROM javadoc_constructors jcon
        WHERE jcon.class_id = :classId
        AND (jcon.parameters IS NULL OR jsonb_array_length(jcon.parameters) = 0)
        LIMIT 1
        """, nativeQuery = true)
    Optional<JavadocConstructor> findDefaultConstructor(@Param("classId") Long classId);

    /**
     * Find constructors by parameter count.
     */
    @Query(value = """
        SELECT jcon.* FROM javadoc_constructors jcon
        WHERE jcon.class_id = :classId
        AND jsonb_array_length(COALESCE(jcon.parameters, '[]'::jsonb)) = :paramCount
        """, nativeQuery = true)
    List<JavadocConstructor> findByParameterCount(
            @Param("classId") Long classId,
            @Param("paramCount") int paramCount);

    /**
     * Find constructors for a class FQCN.
     */
    @Query("""
        SELECT jcon FROM JavadocConstructor jcon
        JOIN jcon.javadocClass jc
        WHERE jc.fqcn = :fqcn
        """)
    List<JavadocConstructor> findByClassFqcn(@Param("fqcn") String fqcn);

    /**
     * Find constructors for a specific library, version, and class.
     */
    @Query("""
        SELECT jcon FROM JavadocConstructor jcon
        JOIN jcon.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.fqcn = :fqcn
        """)
    List<JavadocConstructor> findByLibraryVersionAndClass(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("fqcn") String fqcn);

    /**
     * Find constructors with a specific annotation.
     */
    @Query(value = """
        SELECT jcon.* FROM javadoc_constructors jcon
        JOIN javadoc_classes jc ON jcon.class_id = jc.id
        JOIN javadoc_packages jp ON jc.package_id = jp.id
        WHERE jp.library_name = :libraryName
        AND jp.version = :version
        AND :annotation = ANY(jcon.annotations)
        """, nativeQuery = true)
    List<JavadocConstructor> findByAnnotation(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("annotation") String annotation);

    /**
     * Count constructors for a class.
     */
    long countByJavadocClassId(Long classId);

    /**
     * Count constructors for a library version.
     */
    @Query("""
        SELECT COUNT(jcon) FROM JavadocConstructor jcon
        JOIN jcon.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        """)
    long countByLibraryAndVersion(
            @Param("libraryName") String libraryName,
            @Param("version") String version);
}
