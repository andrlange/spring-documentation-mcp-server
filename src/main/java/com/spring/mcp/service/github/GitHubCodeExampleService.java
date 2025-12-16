package com.spring.mcp.service.github;

import com.spring.mcp.config.GitHubProperties;
import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.repository.CodeExampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting and processing code examples from Spring GitHub repositories.
 *
 * This service discovers and processes Java/Kotlin source files that are referenced
 * in Spring documentation via the include-code:: directive.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubCodeExampleService {

    private final GitHubProperties gitHubProperties;
    private final GitHubDocumentationDiscoveryService discoveryService;
    private final GitHubContentFetchService contentFetchService;
    private final CodeExampleRepository codeExampleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Pattern for include-code:: directive in AsciiDoc.
     * Examples:
     * - include-code::MyConfiguration[]
     * - include-code::org.springframework.boot.docs.web.MyClass[]
     */
    private static final Pattern INCLUDE_CODE_PATTERN =
        Pattern.compile("include-code::([\\w.]+)\\[([^\\]]*)\\]");

    /**
     * Pattern for standard include:: directive that references Java files.
     * Example: include::example$MyClass.java[]
     */
    private static final Pattern INCLUDE_JAVA_PATTERN =
        Pattern.compile("include::([^\\[]+\\.java)\\[([^\\]]*)\\]");

    /**
     * Pattern for extracting @ tags from Java source code.
     * Used to extract metadata like @tag, @author, @since, etc.
     */
    private static final Pattern JAVADOC_TAG_PATTERN =
        Pattern.compile("@(\\w+)\\s+(.+?)(?=\\n\\s*[@*]|\\n\\s*\\*/)", Pattern.DOTALL);

    /**
     * Pattern for class/interface/record declaration.
     */
    private static final Pattern CLASS_PATTERN =
        Pattern.compile("(?:public\\s+)?(?:class|interface|record|enum)\\s+(\\w+)");

    /**
     * Extract and sync code examples from GitHub for a project version.
     *
     * @param projectVersion the project version to sync
     * @return number of code examples synced
     */
    @Transactional
    public int syncCodeExamples(ProjectVersion projectVersion) {
        String projectSlug = projectVersion.getProject().getSlug();
        String version = projectVersion.getVersion();

        log.info("Syncing code examples from GitHub for {} version {}", projectSlug, version);

        // First, discover all code example files
        Map<String, String> codeFiles = contentFetchService.fetchAllCodeExamples(
            projectSlug, version, discoveryService);

        if (codeFiles.isEmpty()) {
            log.info("No code examples discovered for {} version {}", projectSlug, version);
            return 0;
        }

        int savedCount = 0;
        int skippedCount = 0;

        for (Map.Entry<String, String> entry : codeFiles.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();

            try {
                CodeExample example = processCodeFile(projectVersion, filePath, content);

                if (example != null) {
                    // Check if already exists
                    if (codeExampleRepository.existsByVersionAndSourceUrl(projectVersion, example.getSourceUrl())) {
                        log.debug("Code example already exists: {}", example.getTitle());
                        skippedCount++;
                        continue;
                    }

                    try {
                        codeExampleRepository.save(example);
                        savedCount++;
                        log.debug("Saved code example: {}", example.getTitle());
                    } catch (DataAccessException | PersistenceException e) {
                        // Handle persistence errors gracefully
                        log.error("Database error saving code example '{}': {}", example.getTitle(), e.getMessage());

                        // Clear the entity manager to reset session state
                        // This prevents "null identifier" errors from corrupting subsequent operations
                        try {
                            entityManager.clear();
                            log.debug("Entity manager cleared after code example save failure");
                        } catch (Exception clearEx) {
                            log.warn("Failed to clear entity manager: {}", clearEx.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Error processing code file {}: {}", filePath, e.getMessage());
            }
        }

        log.info("Synced {} new code examples for {} version {} ({} skipped)",
                 savedCount, projectSlug, version, skippedCount);

        return savedCount;
    }

    /**
     * Extract code example references from AsciiDoc documentation content.
     *
     * @param adocContent the AsciiDoc content
     * @return list of code example references
     */
    public List<CodeExampleReference> extractReferences(String adocContent) {
        List<CodeExampleReference> references = new ArrayList<>();

        if (adocContent == null || adocContent.isBlank()) {
            return references;
        }

        // Extract include-code:: references
        Matcher includeCodeMatcher = INCLUDE_CODE_PATTERN.matcher(adocContent);
        while (includeCodeMatcher.find()) {
            String className = includeCodeMatcher.group(1);
            String attributes = includeCodeMatcher.group(2);
            references.add(new CodeExampleReference(className, "include-code", attributes));
        }

        // Extract include:: references for Java files
        Matcher includeJavaMatcher = INCLUDE_JAVA_PATTERN.matcher(adocContent);
        while (includeJavaMatcher.find()) {
            String filePath = includeJavaMatcher.group(1);
            String attributes = includeJavaMatcher.group(2);
            references.add(new CodeExampleReference(filePath, "include", attributes));
        }

        return references;
    }

    /**
     * Fetch a specific code example by class name reference.
     *
     * @param projectSlug the project slug
     * @param version the version
     * @param className the class name from include-code directive
     * @return the code example content, or null if not found
     */
    public Optional<String> fetchCodeExample(String projectSlug, String version, String className) {
        String tag = discoveryService.getGitTag(projectSlug, version);
        String repo = gitHubProperties.getApi().getOrganization() + "/" + projectSlug;

        // Try to find the source file
        String sourcePath = resolveSourcePath(projectSlug, className);

        if (sourcePath == null) {
            log.debug("Could not resolve source path for class: {}", className);
            return Optional.empty();
        }

        return contentFetchService.fetchRawContent(repo, sourcePath, tag);
    }

    /**
     * Process a code file and create a CodeExample entity.
     */
    private CodeExample processCodeFile(ProjectVersion projectVersion, String filePath, String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String projectSlug = projectVersion.getProject().getSlug();
        String version = projectVersion.getVersion();
        String tag = discoveryService.getGitTag(projectSlug, version);

        // Extract class name
        String className = extractClassName(content);
        if (className == null) {
            // Use filename as fallback
            className = extractFileNameWithoutExtension(filePath);
        }

        // Extract metadata
        String title = formatTitle(className, filePath);
        String description = extractDescription(content);
        String language = detectLanguage(filePath);
        String category = extractCategory(filePath);
        String[] tags = extractTags(filePath, content);

        // Build source URL
        String sourceUrl = buildGitHubSourceUrl(projectSlug, filePath, tag);

        return CodeExample.builder()
            .version(projectVersion)
            .title(title)
            .description(description)
            .codeSnippet(content)
            .language(language)
            .category(category)
            .tags(tags)
            .sourceUrl(sourceUrl)
            .build();
    }

    /**
     * Extract class name from Java source code.
     */
    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract description from Javadoc comment.
     */
    private String extractDescription(String content) {
        // Look for Javadoc comment at the start
        int javadocStart = content.indexOf("/**");
        if (javadocStart < 0) {
            return null;
        }

        int javadocEnd = content.indexOf("*/", javadocStart);
        if (javadocEnd < 0) {
            return null;
        }

        String javadoc = content.substring(javadocStart + 3, javadocEnd);

        // Extract the description (text before the first @tag)
        int firstTag = javadoc.indexOf("\n * @");
        if (firstTag < 0) {
            firstTag = javadoc.indexOf("\n* @");
        }

        String description = firstTag >= 0
            ? javadoc.substring(0, firstTag)
            : javadoc;

        // Clean up the description
        description = description
            .replaceAll("\\n\\s*\\*\\s*", " ")  // Remove line prefixes
            .replaceAll("\\s+", " ")            // Normalize whitespace
            .trim();

        return description.isEmpty() ? null : description;
    }

    /**
     * Resolve the source file path for a class name reference.
     */
    private String resolveSourcePath(String projectSlug, String className) {
        String codeExamplesPath = discoveryService.getCodeExamplesPath(projectSlug);
        if (codeExamplesPath == null) {
            return null;
        }

        // If className contains dots, it's a fully qualified name
        if (className.contains(".")) {
            String path = className.replace(".", "/");
            return codeExamplesPath + "/" + path + ".java";
        }

        // Otherwise, we need to search for it
        // For now, return null as we can't resolve without package info
        return null;
    }

    /**
     * Format a title from class name and file path.
     */
    private String formatTitle(String className, String filePath) {
        if (className != null) {
            // Add spaces before capital letters
            return className.replaceAll("(.)([A-Z])", "$1 $2");
        }

        // Use file name as fallback
        return formatFileNameAsTitle(filePath);
    }

    /**
     * Format file name as a title.
     */
    private String formatFileNameAsTitle(String filePath) {
        String fileName = extractFileNameWithoutExtension(filePath);

        // Replace underscores and hyphens with spaces
        String title = fileName.replaceAll("[_-]", " ");

        // Add spaces before capital letters
        title = title.replaceAll("(.)([A-Z])", "$1 $2");

        // Capitalize first letter
        if (!title.isEmpty()) {
            title = Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }

        return title;
    }

    /**
     * Extract file name without extension from path.
     */
    private String extractFileNameWithoutExtension(String filePath) {
        String fileName = filePath;

        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0) {
            fileName = fileName.substring(0, lastDot);
        }

        return fileName;
    }

    /**
     * Detect language from file path.
     */
    private String detectLanguage(String filePath) {
        String lowerPath = filePath.toLowerCase();

        if (lowerPath.endsWith(".java")) {
            return "java";
        } else if (lowerPath.endsWith(".kt") || lowerPath.endsWith(".kts")) {
            return "kotlin";
        } else if (lowerPath.endsWith(".groovy")) {
            return "groovy";
        } else if (lowerPath.endsWith(".xml")) {
            return "xml";
        } else if (lowerPath.endsWith(".yml") || lowerPath.endsWith(".yaml")) {
            return "yaml";
        } else if (lowerPath.endsWith(".properties")) {
            return "properties";
        } else if (lowerPath.endsWith(".json")) {
            return "json";
        }

        return "java"; // Default
    }

    /**
     * Extract category from file path.
     */
    private String extractCategory(String filePath) {
        // Extract category from path structure
        // e.g., "docs/web/servlet/MyClass.java" -> "web/servlet"

        String[] parts = filePath.split("/");
        List<String> categoryParts = new ArrayList<>();

        boolean foundDocs = false;
        for (String part : parts) {
            // Skip leading directories until we find a content directory
            if (part.equals("docs") || part.equals("java") || part.equals("kotlin")) {
                foundDocs = true;
                continue;
            }

            if (foundDocs && !part.contains(".")) {
                // This is a directory, add to category
                categoryParts.add(part);
            }
        }

        if (categoryParts.isEmpty()) {
            return "General";
        }

        // Format category
        return String.join(" / ", categoryParts.stream()
            .map(this::formatCategoryPart)
            .toList());
    }

    /**
     * Format a category part (capitalize, add spaces).
     */
    private String formatCategoryPart(String part) {
        // Replace hyphens/underscores with spaces
        part = part.replaceAll("[_-]", " ");

        // Capitalize first letter of each word
        String[] words = part.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    /**
     * Extract tags from file path and content.
     */
    private String[] extractTags(String filePath, String content) {
        Set<String> tags = new LinkedHashSet<>();

        // Add "github" tag
        tags.add("github");

        // Add language tag
        tags.add(detectLanguage(filePath));

        // Extract tags from path
        String[] pathParts = filePath.toLowerCase().split("/");
        for (String part : pathParts) {
            if (isRelevantTag(part)) {
                tags.add(part.replace("-", " "));
            }
        }

        // Extract Spring annotations from content
        if (content.contains("@SpringBootApplication")) {
            tags.add("spring boot");
        }
        if (content.contains("@RestController") || content.contains("@Controller")) {
            tags.add("web");
        }
        if (content.contains("@Service")) {
            tags.add("service");
        }
        if (content.contains("@Repository")) {
            tags.add("data");
        }
        if (content.contains("@Configuration")) {
            tags.add("configuration");
        }
        if (content.contains("@Test")) {
            tags.add("testing");
        }
        if (content.contains("@Async")) {
            tags.add("async");
        }
        if (content.contains("@Scheduled")) {
            tags.add("scheduling");
        }

        return tags.toArray(new String[0]);
    }

    /**
     * Check if a path part is a relevant tag.
     */
    private boolean isRelevantTag(String part) {
        // Exclude common directory names that aren't useful as tags
        Set<String> excludedParts = Set.of(
            "src", "main", "java", "kotlin", "test", "resources",
            "org", "springframework", "boot", "docs", "example", "examples",
            "com", "spring", "mcp", "modules", "root", "pages"
        );

        return !excludedParts.contains(part) &&
               part.length() > 2 &&
               !part.contains(".");
    }

    /**
     * Build GitHub source URL for a file.
     */
    private String buildGitHubSourceUrl(String projectSlug, String filePath, String ref) {
        String org = gitHubProperties.getApi().getOrganization();
        return String.format("https://github.com/%s/%s/blob/%s/%s",
                            org, projectSlug, ref, filePath);
    }

    /**
     * Record representing a code example reference found in documentation.
     */
    public record CodeExampleReference(
        String reference,
        String type,
        String attributes
    ) {}
}
