package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a constructor from Javadoc documentation.
 * Stores constructor signature, parameters, and documentation.
 */
@Entity
@Table(name = "javadoc_constructors",
        indexes = {
                @Index(name = "idx_javadoc_constructor_class", columnList = "class_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"javadocClass"})
@EqualsAndHashCode(of = {"id"})
public class JavadocConstructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private JavadocClass javadocClass;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String signature;

    /**
     * Parameters as JSON array of objects with name, type, description.
     * Example: [{"name": "id", "type": "Long", "description": "The entity ID"}]
     */
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<Map<String, String>> parameters = new ArrayList<>();

    @Column(name = "throws_list", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> throwsList = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;

    @Column(name = "annotations", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> annotations = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Get the fully qualified class name of the owning class.
     */
    public String getClassFqcn() {
        return javadocClass != null ? javadocClass.getFqcn() : null;
    }

    /**
     * Get the simple name of the class (used as constructor name).
     */
    public String getClassName() {
        return javadocClass != null ? javadocClass.getSimpleName() : null;
    }

    /**
     * Get parameter count.
     */
    public int getParameterCount() {
        return parameters != null ? parameters.size() : 0;
    }

    /**
     * Check if this constructor has a specific annotation.
     */
    public boolean hasAnnotation(String annotationName) {
        return annotations != null && annotations.stream()
                .anyMatch(a -> a.equals(annotationName) || a.endsWith("." + annotationName) || a.equals("@" + annotationName));
    }

    /**
     * Check if this is the default (no-arg) constructor.
     */
    public boolean isDefaultConstructor() {
        return parameters == null || parameters.isEmpty();
    }

    /**
     * Get a formatted signature for display.
     */
    public String getDisplaySignature() {
        if (signature != null) {
            return signature;
        }
        StringBuilder sb = new StringBuilder();
        String className = getClassName();
        sb.append(className != null ? className : "Unknown").append("(");
        if (parameters != null && !parameters.isEmpty()) {
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                Map<String, String> param = parameters.get(i);
                sb.append(param.getOrDefault("type", "?")).append(" ").append(param.getOrDefault("name", "arg" + i));
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
