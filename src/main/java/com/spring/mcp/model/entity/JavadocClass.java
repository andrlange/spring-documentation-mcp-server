package com.spring.mcp.model.entity;

import com.spring.mcp.model.enums.JavadocClassKind;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity representing a Java class, interface, enum, annotation, or record
 * from Javadoc documentation. Contains all class-level documentation and
 * references to methods, fields, and constructors.
 */
@Entity
@Table(name = "javadoc_classes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_javadoc_class",
                columnNames = {"package_id", "fqcn"}
        ))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"javadocPackage", "methods", "fields", "constructors"})
@EqualsAndHashCode(of = {"id", "fqcn"})
public class JavadocClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private JavadocPackage javadocPackage;

    @Column(nullable = false, length = 500)
    private String fqcn;

    @Column(name = "simple_name", nullable = false, length = 255)
    private String simpleName;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JavadocClassKind kind;

    @Column(length = 100)
    private String modifiers;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "super_class", length = 500)
    private String superClass;

    @Column(name = "interfaces", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> interfaces = new ArrayList<>();

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;

    @Column(name = "deprecated_message", columnDefinition = "TEXT")
    private String deprecatedMessage;

    @Column(name = "annotations", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> annotations = new ArrayList<>();

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "javadocClass", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<JavadocMethod> methods = new HashSet<>();

    @OneToMany(mappedBy = "javadocClass", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<JavadocField> fields = new HashSet<>();

    @OneToMany(mappedBy = "javadocClass", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<JavadocConstructor> constructors = new HashSet<>();

    /**
     * Add a method to this class.
     */
    public void addMethod(JavadocMethod method) {
        methods.add(method);
        method.setJavadocClass(this);
    }

    /**
     * Add a field to this class.
     */
    public void addField(JavadocField field) {
        fields.add(field);
        field.setJavadocClass(this);
    }

    /**
     * Add a constructor to this class.
     */
    public void addConstructor(JavadocConstructor constructor) {
        constructors.add(constructor);
        constructor.setJavadocClass(this);
    }

    /**
     * Get the library name from the parent package.
     */
    public String getLibraryName() {
        return javadocPackage != null ? javadocPackage.getLibraryName() : null;
    }

    /**
     * Get the version from the parent package.
     */
    public String getVersion() {
        return javadocPackage != null ? javadocPackage.getVersion() : null;
    }

    /**
     * Get the package name from the parent package.
     */
    public String getPackageName() {
        return javadocPackage != null ? javadocPackage.getPackageName() : null;
    }

    /**
     * Check if this class implements a specific interface.
     */
    public boolean implementsInterface(String interfaceName) {
        return interfaces != null && interfaces.stream()
                .anyMatch(i -> i.equals(interfaceName) || i.endsWith("." + interfaceName));
    }

    /**
     * Check if this class has a specific annotation.
     */
    public boolean hasAnnotation(String annotationName) {
        return annotations != null && annotations.stream()
                .anyMatch(a -> a.equals(annotationName) || a.endsWith("." + annotationName) || a.equals("@" + annotationName));
    }

    /**
     * Get method count including inherited (if tracked in metadata).
     */
    public int getMethodCount() {
        return methods.size();
    }

    /**
     * Get field count.
     */
    public int getFieldCount() {
        return fields.size();
    }

    /**
     * Get constructor count.
     */
    public int getConstructorCount() {
        return constructors.size();
    }
}
