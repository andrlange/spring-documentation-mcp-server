package com.spring.mcp.service.javadoc.dto;

import java.util.List;

/**
 * DTO representing a parsed method from Javadoc.
 *
 * @param name              Method name
 * @param signature         Full method signature
 * @param returnType        Return type
 * @param parameters        List of method parameters
 * @param throwsList        List of exception types
 * @param summary           Short description
 * @param description       Full description
 * @param deprecated        Whether the method is deprecated
 * @param deprecatedMessage Deprecation message
 * @param annotations       List of annotations
 */
public record ParsedMethodDto(
        String name,
        String signature,
        String returnType,
        List<MethodParameterDto> parameters,
        List<String> throwsList,
        String summary,
        String description,
        boolean deprecated,
        String deprecatedMessage,
        List<String> annotations
) {
    /**
     * Create a builder for ParsedMethodDto.
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
     * Check if this method returns void.
     */
    public boolean isVoid() {
        return "void".equals(returnType);
    }

    public static class Builder {
        private String name;
        private String signature;
        private String returnType;
        private List<MethodParameterDto> parameters = List.of();
        private List<String> throwsList = List.of();
        private String summary;
        private String description;
        private boolean deprecated;
        private String deprecatedMessage;
        private List<String> annotations = List.of();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
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

        public Builder description(String description) {
            this.description = description;
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

        public Builder annotations(List<String> annotations) {
            this.annotations = annotations != null ? annotations : List.of();
            return this;
        }

        public ParsedMethodDto build() {
            return new ParsedMethodDto(name, signature, returnType, parameters, throwsList,
                    summary, description, deprecated, deprecatedMessage, annotations);
        }
    }
}
