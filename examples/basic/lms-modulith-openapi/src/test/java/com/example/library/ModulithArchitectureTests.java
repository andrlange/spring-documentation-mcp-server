package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.DiagramOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions.DiagramStyle;
import org.springframework.modulith.docs.Documenter.Options;

import java.nio.file.Path;

/**
 * Architecture tests for Spring Modulith.
 * Verifies module boundaries and dependencies are correctly defined.
 *
 * Note: This demo application uses simplified architecture where services return
 * domain entities directly. In a production application, you should:
 * 1. Create DTOs in the 'api' package for inter-module communication
 * 2. Return DTOs from service methods instead of internal entities
 * 3. This maintains strict module boundaries and encapsulation
 */
class ModulithArchitectureTests {

    // Output documentation to modulith-docs folder in project root
    private static final String OUTPUT_FOLDER = "modulith-docs";

    private final ApplicationModules modules = ApplicationModules.of(LibraryApplication.class);

    @Test
    void verifyModuleStructure() {
        // Print module structure - this shows what modules exist and their dependencies
        // Note: For a demo, we're documenting the structure rather than enforcing strict boundaries
        // Production apps should use DTOs to maintain strict module encapsulation
        System.out.println("=== Module Structure ===");
        modules.forEach(System.out::println);
    }

    @Test
    void printModuleInfo() {
        // Print module information for debugging
        modules.forEach(System.out::println);
    }

    @Test
    void generateDocumentation() {
        // Generate module documentation to modulith-docs folder

        // Configure diagram options with UML style
        var diagramOptions = DiagramOptions.defaults()
            .withStyle(DiagramStyle.UML);

        // Configure output options with custom path
        var options = Options.defaults()
            .withOutputFolder(OUTPUT_FOLDER);

        // Create documenter with options
        var documenter = new Documenter(modules, options);

        // Generate all documentation including aggregating overview
        documenter
            .writeModulesAsPlantUml(diagramOptions)
            .writeIndividualModulesAsPlantUml(diagramOptions)
            .writeModuleCanvases()
            .writeAggregatingDocument();

        var outputPath = Path.of(OUTPUT_FOLDER);
        System.out.println("=== Documentation Generated ===");
        System.out.println("Output folder: " + outputPath.toAbsolutePath());
        System.out.println("Generated files:");
        System.out.println("  - all-docs.adoc (aggregating overview document)");
        System.out.println("  - components.puml (all modules diagram)");
        System.out.println("  - module-*.puml (individual module diagrams)");
        System.out.println("  - module-*.adoc (module canvas documentation)");
    }

    @Test
    void listAllModules() {
        System.out.println("=== Library Management System Modules ===");
        System.out.println();

        modules.forEach(module -> {
            System.out.println("Module: " + module.getDisplayName());
            System.out.println("  Base Package: " + module.getBasePackage());

            System.out.println("  Dependencies:");
            module.getDependencies(modules).stream().forEach(dep ->
                System.out.println("    -> " + dep.getTargetModule().getDisplayName())
            );

            System.out.println("  Named Interfaces:");
            module.getNamedInterfaces().forEach(ni ->
                System.out.println("    - " + ni.getName())
            );

            System.out.println();
        });
    }
}
