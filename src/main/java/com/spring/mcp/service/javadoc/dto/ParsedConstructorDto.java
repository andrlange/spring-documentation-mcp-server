package com.spring.mcp.service.javadoc.dto;

import java.util.List;

/**
 * DTO representing a parsed constructor from Javadoc.
 *
 * @param signature   Full constructor signature
 * @param parameters  List of constructor parameters
 * @param throwsList  List of exception types
 * @param summary     Short description
 * @param deprecated  Whether the constructor is deprecated
 * @param annotations List of annotations
 */
public record ParsedConstructorDto(
        String signature,
        List<MethodParameterDto> parameters,
        List<String> throwsList,
        String summary,
        boolean deprecated,
        List<String> annotations
) {
    /**
     * Create a builder for ParsedConstructorDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get parameter count.
     */
    public int parameterCount() {
        return parameters != null ? parameters.size() : 0;
    }

    /**
     * Check if this is the default (no-arg) constructor.
     */
    public boolean isDefaultConstructor() {
        return parameters == null || parameters.isEmpty();
    }

    public static class Builder {
        private String signature;
        private List<MethodParameterDto> parameters = List.of();
        private List<String> throwsList = List.of();
        private String summary;
        private boolean deprecated;
        private List<String> annotations = List.of();

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder parameters(List<MethodParameterDto> parameters) {
            this.parameters = parameters != null ? parameters : List.of();
            return this;
        }

        public Builder throwsList(List<String> throwsList) {
            this.throwsList = throwsList != null ? throwsList : List.of();
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public Builder annotations(List<String> annotations) {
            this.annotations = annotations != null ? annotations : List.of();
            return this;
        }

        public ParsedConstructorDto build() {
            return new ParsedConstructorDto(signature, parameters, throwsList, summary, deprecated, annotations);
        }
    }
}
