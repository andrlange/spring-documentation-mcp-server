package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a method from Javadoc documentation.
 * Stores method signature, parameters, return type, and documentation.
 */
@Entity
@Table(name = "javadoc_methods",
        indexes = {
                @Index(name = "idx_javadoc_method_class", columnList = "class_id"),
                @Index(name = "idx_javadoc_method_name", columnList = "name"),
                @Index(name = "idx_javadoc_method_class_name", columnList = "class_id, name")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"javadocClass"})
@EqualsAndHashCode(of = {"id"})
public class JavadocMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private JavadocClass javadocClass;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String signature;

    @Column(name = "return_type", length = 500)
    private String returnType;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;

    @Column(name = "deprecated_message", columnDefinition = "TEXT")
    private String deprecatedMessage;

    @Column(name = "annotations", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> annotations = new ArrayList<>();

    /**
     * Additional metadata such as type parameters, modifiers, default values.
     */
    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

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
     * Get parameter count.
     */
    public int getParameterCount() {
        return parameters != null ? parameters.size() : 0;
    }

    /**
     * Check if this method has a specific annotation.
     */
    public boolean hasAnnotation(String annotationName) {
        return annotations != null && annotations.stream()
                .anyMatch(a -> a.equals(annotationName) || a.endsWith("." + annotationName) || a.equals("@" + annotationName));
    }

    /**
     * Check if this method throws a specific exception.
     */
    public boolean throwsException(String exceptionName) {
        return throwsList != null && throwsList.stream()
                .anyMatch(e -> e.equals(exceptionName) || e.endsWith("." + exceptionName));
    }

    /**
     * Get a formatted signature for display.
     */
    public String getDisplaySignature() {
        if (signature != null) {
            return signature;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        if (parameters != null && !parameters.isEmpty()) {
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                Map<String, String> param = parameters.get(i);
                sb.append(param.getOrDefault("type", "?")).append(" ").append(param.getOrDefault("name", "arg" + i));
            }
        }
        sb.append(")");
        if (returnType != null && !returnType.equals("void")) {
            sb.append(": ").append(returnType);
        }
        return sb.toString();
    }
}
