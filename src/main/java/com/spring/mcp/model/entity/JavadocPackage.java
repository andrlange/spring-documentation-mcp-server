package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Java package from Javadoc documentation.
 * Stores package-level documentation and references all classes within.
 */
@Entity
@Table(name = "javadoc_packages",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_javadoc_package",
                columnNames = {"library_name", "version", "package_name"}
        ))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"classes"})
@EqualsAndHashCode(of = {"id", "libraryName", "version", "packageName"})
public class JavadocPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "library_name", nullable = false, length = 100)
    private String libraryName;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "package_name", nullable = false, length = 500)
    private String packageName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "javadocPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JavadocClass> classes = new ArrayList<>();

    /**
     * Add a class to this package.
     */
    public void addClass(JavadocClass javadocClass) {
        classes.add(javadocClass);
        javadocClass.setJavadocPackage(this);
    }

    /**
     * Remove a class from this package.
     */
    public void removeClass(JavadocClass javadocClass) {
        classes.remove(javadocClass);
        javadocClass.setJavadocPackage(null);
    }

    /**
     * Get the count of classes in this package.
     */
    public int getClassCount() {
        return classes.size();
    }

    /**
     * Get only interface types from this package.
     */
    public List<JavadocClass> getInterfaces() {
        return classes.stream()
                .filter(c -> c.getKind() == com.spring.mcp.model.enums.JavadocClassKind.INTERFACE)
                .toList();
    }

    /**
     * Get only class types (not interfaces, enums, etc.) from this package.
     */
    public List<JavadocClass> getClassesOnly() {
        return classes.stream()
                .filter(c -> c.getKind() == com.spring.mcp.model.enums.JavadocClassKind.CLASS)
                .toList();
    }

    /**
     * Get only enum types from this package.
     */
    public List<JavadocClass> getEnums() {
        return classes.stream()
                .filter(c -> c.getKind() == com.spring.mcp.model.enums.JavadocClassKind.ENUM)
                .toList();
    }

    /**
     * Get only annotation types from this package.
     */
    public List<JavadocClass> getAnnotations() {
        return classes.stream()
                .filter(c -> c.getKind() == com.spring.mcp.model.enums.JavadocClassKind.ANNOTATION)
                .toList();
    }

    /**
     * Get only record types from this package.
     */
    public List<JavadocClass> getRecords() {
        return classes.stream()
                .filter(c -> c.getKind() == com.spring.mcp.model.enums.JavadocClassKind.RECORD)
                .toList();
    }
}
