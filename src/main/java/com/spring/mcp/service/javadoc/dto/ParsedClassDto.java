package com.spring.mcp.service.javadoc.dto;

import com.spring.mcp.model.enums.JavadocClassKind;

import java.util.List;

/**
 * DTO representing a parsed class/interface/enum from Javadoc.
 *
 * @param fqcn              Fully qualified class name
 * @param simpleName        Simple class name
 * @param kind              Type (CLASS, INTERFACE, ENUM, ANNOTATION, RECORD)
 * @param modifiers         Access modifiers
 * @param summary           Short description
 * @param description       Full description
 * @param superClass        Superclass FQCN
 * @param interfaces        List of implemented interfaces
 * @param sourceUrl         URL to the source documentation
 * @param deprecated        Whether the class is deprecated
 * @param deprecatedMessage Deprecation message
 * @param methods           Parsed methods
 * @param fields            Parsed fields
 * @param constructors      Parsed constructors
 * @param annotations       List of class annotations
 */
public record ParsedClassDto(
        String fqcn,
        String simpleName,
        JavadocClassKind kind,
        String modifiers,
        String summary,
        String description,
        String superClass,
        List<String> interfaces,
        String sourceUrl,
        boolean deprecated,
        String deprecatedMessage,
        List<ParsedMethodDto> methods,
        List<ParsedFieldDto> fields,
        List<ParsedConstructorDto> constructors,
        List<String> annotations
) {
    /**
     * Create a builder for ParsedClassDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the package name from the FQCN.
     */
    public String packageName() {
        if (fqcn == null) return null;
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot > 0 ? fqcn.substring(0, lastDot) : "";
    }

    /**
     * Get method count.
     */
    public int methodCount() {
        return methods != null ? methods.size() : 0;
    }

    /**
     * Get field count.
     */
    public int fieldCount() {
        return fields != null ? fields.size() : 0;
    }

    /**
     * Get constructor count.
     */
    public int constructorCount() {
        return constructors != null ? constructors.size() : 0;
    }

    public static class Builder {
        private String fqcn;
        private String simpleName;
        private JavadocClassKind kind = JavadocClassKind.CLASS;
        private String modifiers;
        private String summary;
        private String description;
        private String superClass;
        private List<String> interfaces = List.of();
        private String sourceUrl;
        private boolean deprecated;
        private String deprecatedMessage;
        private List<ParsedMethodDto> methods = List.of();
        private List<ParsedFieldDto> fields = List.of();
        private List<ParsedConstructorDto> constructors = List.of();
        private List<String> annotations = List.of();

        public Builder fqcn(String fqcn) {
            this.fqcn = fqcn;
            return this;
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder kind(JavadocClassKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder modifiers(String modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder superClass(String superClass) {
            this.superClass = superClass;
            return this;
        }

        public Builder interfaces(List<String> interfaces) {
            this.interfaces = interfaces != null ? interfaces : List.of();
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder deprecatedMessage(String deprecatedMessage) {
            this.deprecatedMessage = deprecatedMessage;
            return this;
        }

        public Builder methods(List<ParsedMethodDto> methods) {
            this.methods = methods != null ? methods : List.of();
            return this;
        }

        public Builder fields(List<ParsedFieldDto> fields) {
            this.fields = fields != null ? fields : List.of();
            return this;
        }

        public Builder constructors(List<ParsedConstructorDto> constructors) {
            this.constructors = constructors != null ? constructors : List.of();
            return this;
        }

        public Builder annotations(List<String> annotations) {
            this.annotations = annotations != null ? annotations : List.of();
            return this;
        }

        public ParsedClassDto build() {
            return new ParsedClassDto(fqcn, simpleName, kind, modifiers, summary, description,
                    superClass, interfaces, sourceUrl, deprecated, deprecatedMessage,
                    methods, fields, constructors, annotations);
        }
    }
}
