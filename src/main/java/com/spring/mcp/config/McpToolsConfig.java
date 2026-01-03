package com.spring.mcp.config;

import com.spring.mcp.service.tools.FlavorGroupTools;
import com.spring.mcp.service.tools.FlavorTools;
import com.spring.mcp.service.tools.InitializrTools;
import com.spring.mcp.service.tools.JavadocTools;
import com.spring.mcp.service.tools.LanguageEvolutionTools;
import com.spring.mcp.service.tools.MigrationTools;
import com.spring.mcp.service.tools.SpringDocumentationTools;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class that explicitly creates MCP tool specifications.
 * <p>
 * This is necessary because Spring AI's auto-configuration creates the toolSpecs bean
 * during the auto-configuration phase, before application beans are created.
 * By explicitly depending on the tool beans, we force them to be created first
 * and provide the specifications as a separate bean that gets merged.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class McpToolsConfig {

    /**
     * Creates MCP tool specifications for all tool beans.
     * <p>
     * By explicitly depending on each tool bean as a constructor/method parameter,
     * Spring ensures those beans are created before this method is called.
     * The returned list is merged with the (empty) list from Spring AI's auto-configuration.
     *
     * @param springDocumentationTools Spring documentation tools
     * @param flavorTools Flavor tools
     * @param flavorGroupTools Flavor group tools
     * @param javadocTools Javadoc tools
     * @param initializrTools Initializr tools
     * @param migrationTools Migration tools
     * @param languageEvolutionTools Language evolution tools
     * @return List of SyncToolSpecification for all tools
     */
    @Bean("customToolSpecs")
    public List<SyncToolSpecification> customToolSpecs(
            SpringDocumentationTools springDocumentationTools,
            FlavorTools flavorTools,
            FlavorGroupTools flavorGroupTools,
            JavadocTools javadocTools,
            InitializrTools initializrTools,
            MigrationTools migrationTools,
            LanguageEvolutionTools languageEvolutionTools
    ) {
        List<Object> tools = List.of(
                springDocumentationTools,
                flavorTools,
                flavorGroupTools,
                javadocTools,
                initializrTools,
                migrationTools,
                languageEvolutionTools
        );

        log.info("Creating custom tool specifications for {} tool beans", tools.size());

        List<SyncToolSpecification> specs = SyncMcpAnnotationProviders.toolSpecifications(tools);

        log.info("Created {} tool specifications", specs.size());
        specs.forEach(spec -> log.debug("Tool: {}", spec.tool().name()));

        return specs;
    }
}
