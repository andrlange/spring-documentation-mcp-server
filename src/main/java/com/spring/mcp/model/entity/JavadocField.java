package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a field from Javadoc documentation.
 * Stores field name, type, modifiers, and documentation.
 */
@Entity
@Table(name = "javadoc_fields",
        indexes = {
                @Index(name = "idx_javadoc_field_class", columnList = "class_id"),
                @Index(name = "idx_javadoc_field_name", columnList = "name")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"javadocClass"})
@EqualsAndHashCode(of = {"id"})
public class JavadocField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private JavadocClass javadocClass;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String type;

    @Column(length = 100)
    private String modifiers;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;

    @Column(name = "constant_value", columnDefinition = "TEXT")
    private String constantValue;

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
     * Check if this field is static.
     */
    public boolean isStatic() {
        return modifiers != null && modifiers.contains("static");
    }

    /**
     * Check if this field is final.
     */
    public boolean isFinal() {
        return modifiers != null && modifiers.contains("final");
    }

    /**
     * Check if this field is a constant (static final).
     */
    public boolean isConstant() {
        return isStatic() && isFinal();
    }

    /**
     * Get a formatted declaration for display.
     */
    public String getDisplayDeclaration() {
        StringBuilder sb = new StringBuilder();
        if (modifiers != null && !modifiers.isBlank()) {
            sb.append(modifiers).append(" ");
        }
        sb.append(type).append(" ").append(name);
        if (constantValue != null && !constantValue.isBlank()) {
            sb.append(" = ").append(constantValue);
        }
        return sb.toString();
    }
}
