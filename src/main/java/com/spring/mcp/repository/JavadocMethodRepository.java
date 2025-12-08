package com.spring.mcp.repository;

import com.spring.mcp.model.entity.JavadocMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for JavadocMethod entities.
 * Manages method-level Javadoc documentation.
 */
@Repository
public interface JavadocMethodRepository extends JpaRepository<JavadocMethod, Long> {

    /**
     * Find all methods for a class.
     */
    List<JavadocMethod> findByJavadocClassId(Long classId);

    /**
     * Find methods by name in a class.
     */
    List<JavadocMethod> findByJavadocClassIdAndName(Long classId, String name);

    /**
     * Find methods by name for a specific class FQCN.
     */
    @Query("""
        SELECT jm FROM JavadocMethod jm
        JOIN jm.javadocClass jc
        WHERE jc.fqcn = :fqcn
        AND jm.name = :methodName
        """)
    List<JavadocMethod> findByClassFqcnAndName(
            @Param("fqcn") String fqcn,
            @Param("methodName") String methodName);

    /**
     * Find methods by name for a specific library, version, and class.
     */
    @Query("""
        SELECT jm FROM JavadocMethod jm
        JOIN jm.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jc.fqcn = :fqcn
        AND jm.name = :methodName
        """)
    List<JavadocMethod> findByLibraryVersionClassAndName(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("fqcn") String fqcn,
            @Param("methodName") String methodName);

    /**
     * Find deprecated methods in a class.
     */
    List<JavadocMethod> findByJavadocClassIdAndDeprecatedTrue(Long classId);

    /**
     * Find all deprecated methods for a library version.
     */
    @Query("""
        SELECT jm FROM JavadocMethod jm
        JOIN jm.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jm.deprecated = true
        """)
    List<JavadocMethod> findDeprecatedMethods(
            @Param("libraryName") String libraryName,
            @Param("version") String version);

    /**
     * Search methods by name pattern.
     */
    @Query("""
        SELECT jm FROM JavadocMethod jm
        JOIN jm.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jm.name LIKE %:pattern%
        ORDER BY jm.name
        """)
    List<JavadocMethod> searchByNamePattern(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("pattern") String pattern);

    /**
     * Find methods with a specific annotation.
     */
    @Query(value = """
        SELECT jm.* FROM javadoc_methods jm
        JOIN javadoc_classes jc ON jm.class_id = jc.id
        JOIN javadoc_packages jp ON jc.package_id = jp.id
        WHERE jp.library_name = :libraryName
        AND jp.version = :version
        AND :annotation = ANY(jm.annotations)
        """, nativeQuery = true)
    List<JavadocMethod> findByAnnotation(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("annotation") String annotation);

    /**
     * Find methods by return type.
     */
    @Query("""
        SELECT jm FROM JavadocMethod jm
        JOIN jm.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        AND jm.returnType = :returnType
        """)
    List<JavadocMethod> findByReturnType(
            @Param("libraryName") String libraryName,
            @Param("version") String version,
            @Param("returnType") String returnType);

    /**
     * Count methods for a class.
     */
    long countByJavadocClassId(Long classId);

    /**
     * Count methods for a library version.
     */
    @Query("""
        SELECT COUNT(jm) FROM JavadocMethod jm
        JOIN jm.javadocClass jc
        JOIN jc.javadocPackage jp
        WHERE jp.libraryName = :libraryName
        AND jp.version = :version
        """)
    long countByLibraryAndVersion(
            @Param("libraryName") String libraryName,
            @Param("version") String version);
}
