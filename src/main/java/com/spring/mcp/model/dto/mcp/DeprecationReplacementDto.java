package com.spring.mcp.model.dto.mcp;

import com.spring.mcp.model.entity.DeprecationReplacement;

/**
 * DTO for deprecation replacement information in MCP responses.
 */
public record DeprecationReplacementDto(
    String deprecatedClass,
    String deprecatedMethod,
    String replacementClass,
    String replacementMethod,
    String deprecatedSince,
    String removedIn,
    String migrationNotes,
    String codeBefore,
    String codeAfter
) {
    /**
     * Create from entity
     */
    public static DeprecationReplacementDto from(DeprecationReplacement entity) {
        return new DeprecationReplacementDto(
            entity.getDeprecatedClass(),
            entity.getDeprecatedMethod(),
            entity.getReplacementClass(),
            entity.getReplacementMethod(),
            entity.getDeprecatedSince(),
            entity.getRemovedIn(),
            entity.getMigrationNotes(),
            entity.getCodeBefore(),
            entity.getCodeAfter()
        );
    }

    /**
     * Return when no replacement found
     */
    public static DeprecationReplacementDto notFound(String className, String methodName) {
        return new DeprecationReplacementDto(
            className, methodName, null, null, null, null,
            "No replacement information found for this class/method.", null, null
        );
    }
}
