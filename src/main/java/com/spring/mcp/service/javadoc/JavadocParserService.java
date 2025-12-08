package com.spring.mcp.service.javadoc;

import com.spring.mcp.model.enums.JavadocClassKind;
import com.spring.mcp.service.javadoc.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing Javadoc HTML pages using Jsoup.
 * Supports both Java 8 and Java 11+ Javadoc formats.
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocParserService {

    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("^(.+?)\\s+(\\w+)\\s*\\((.*)\\)");
    private static final Pattern THROWS_PATTERN = Pattern.compile("throws\\s+(.+)$");

    /**
     * Parse a package-summary.html page.
     *
     * @param html    The HTML content
     * @param baseUrl The base URL for resolving links
     * @return Parsed package DTO
     */
    public ParsedPackageDto parsePackageSummary(String html, String baseUrl) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(baseUrl);

        var builder = ParsedPackageDto.builder().sourceUrl(baseUrl);

        // Extract package name - try multiple selectors
        String packageName = extractPackageName(doc);
        builder.packageName(packageName);

        // Extract description
        Element descBlock = doc.selectFirst(".package-description .block, .block, #package-description");
        if (descBlock != null) {
            builder.description(cleanHtml(descBlock.html()));
            builder.summary(extractFirstSentence(descBlock.text()));
        }

        // Extract class list
        List<String> classes = new ArrayList<>();
        Elements classLinks = doc.select(
                ".type-summary a[href], " +
                ".summary-table a[href], " +
                "table.typeSummary a[href], " +
                "table.classes a[href], " +
                ".memberSummary a[href]"
        );

        for (Element link : classLinks) {
            String href = link.attr("href");
            if (href.endsWith(".html") && !href.contains("#")) {
                String className = href.replace(".html", "").replace("/", ".");
                if (packageName != null && !packageName.isEmpty()) {
                    classes.add(packageName + "." + simpleClassName(className));
                } else {
                    classes.add(className);
                }
            }
        }
        builder.classes(classes);

        log.debug("Parsed package '{}' with {} classes", packageName, classes.size());
        return builder.build();
    }

    /**
     * Parse a class page.
     *
     * @param html    The HTML content
     * @param baseUrl The base URL
     * @return Parsed class DTO
     */
    public ParsedClassDto parseClassPage(String html, String baseUrl) {
        Document doc = Jsoup.parse(html);
        doc.setBaseUri(baseUrl);

        var builder = ParsedClassDto.builder().sourceUrl(baseUrl);

        // Extract class info from title/header
        String title = doc.title();
        parseClassTitle(doc, builder);

        // Extract class description
        Element descBlock = doc.selectFirst(".class-description .block, .description .block, .type-signature + .block");
        if (descBlock != null) {
            builder.description(cleanHtml(descBlock.html()));
            builder.summary(extractFirstSentence(descBlock.text()));
        }

        // Check for deprecation
        Element deprecatedBlock = doc.selectFirst(".deprecation-block, .deprecatedContent, .deprecated");
        if (deprecatedBlock != null) {
            builder.deprecated(true);
            Element deprecatedComment = deprecatedBlock.selectFirst(".deprecation-comment, .deprecatedComment");
            if (deprecatedComment != null) {
                builder.deprecatedMessage(deprecatedComment.text());
            }
        }

        // Extract inheritance
        parseInheritance(doc, builder);

        // Extract annotations
        builder.annotations(extractAnnotations(doc));

        // Extract methods
        builder.methods(extractMethods(doc));

        // Extract fields
        builder.fields(extractFields(doc));

        // Extract constructors
        builder.constructors(extractConstructors(doc));

        ParsedClassDto result = builder.build();
        log.debug("Parsed class '{}' with {} methods, {} fields, {} constructors",
                result.simpleName(), result.methodCount(), result.fieldCount(), result.constructorCount());
        return result;
    }

    /**
     * Extract package name from document.
     */
    private String extractPackageName(Document doc) {
        // Java 11+ format
        Element packageElement = doc.selectFirst(".package-hierarchy-label, .about-language");
        if (packageElement != null) {
            return packageElement.text().replace("Package ", "").trim();
        }

        // From title
        String title = doc.title();
        if (title != null && title.contains(" ")) {
            return title.split(" ")[0].trim();
        }

        // From URL path
        Element canonical = doc.selectFirst("link[rel=canonical]");
        if (canonical != null) {
            String href = canonical.attr("href");
            return extractPackageFromUrl(href);
        }

        return "";
    }

    /**
     * Parse class title and extract name, kind, modifiers.
     */
    private void parseClassTitle(Document doc, ParsedClassDto.Builder builder) {
        // Try Java 11+ format - look for specific class name element first
        Element classNameElement = doc.selectFirst(".type-signature .element-name, .type-signature .type-name-label");
        if (classNameElement != null) {
            String simpleName = classNameElement.text().trim();
            builder.simpleName(simpleName);

            // Get modifiers from separate span
            Element modifiersElement = doc.selectFirst(".type-signature .modifiers");
            if (modifiersElement != null) {
                String modifiersText = modifiersElement.text().trim();
                parseModifiersAndKind(modifiersText, builder);
            }
            return;
        }

        // Fallback: try the whole type-signature or type-name-link
        Element typeSignature = doc.selectFirst(".type-signature, .type-name-link");
        if (typeSignature != null) {
            String text = typeSignature.text();
            parseTypeDeclaration(text, builder);
            return;
        }

        // Java 8 format - from title
        String title = doc.title();
        if (title != null) {
            // Title format: "ClassName (Package Name)"
            String[] parts = title.split("\\s*\\(");
            if (parts.length > 0) {
                String classInfo = parts[0].trim();
                parseTypeDeclaration(classInfo, builder);
            }
        }

        // Extract from header as last resort
        Element header = doc.selectFirst("h1.title, h1");
        if (header != null) {
            String headerText = header.text();
            if (headerText.contains(" ")) {
                String[] words = headerText.split("\\s+");
                builder.simpleName(words[words.length - 1]);
            }
        }
    }

    /**
     * Parse modifiers and determine class kind from modifiers text.
     */
    private void parseModifiersAndKind(String modifiersText, ParsedClassDto.Builder builder) {
        if (modifiersText == null || modifiersText.isBlank()) return;

        String lower = modifiersText.toLowerCase();

        // Determine kind
        if (lower.contains("interface")) {
            builder.kind(JavadocClassKind.INTERFACE);
        } else if (lower.contains("enum")) {
            builder.kind(JavadocClassKind.ENUM);
        } else if (lower.contains("@interface") || lower.contains("annotation")) {
            builder.kind(JavadocClassKind.ANNOTATION);
        } else if (lower.contains("record")) {
            builder.kind(JavadocClassKind.RECORD);
        } else {
            builder.kind(JavadocClassKind.CLASS);
        }

        // Extract access modifiers
        StringBuilder modifiers = new StringBuilder();
        String[] tokens = modifiersText.split("\\s+");
        for (String token : tokens) {
            String t = token.toLowerCase();
            if (t.equals("public") || t.equals("protected") || t.equals("private") ||
                    t.equals("abstract") || t.equals("final") || t.equals("static") ||
                    t.equals("sealed") || t.equals("non-sealed")) {
                if (!modifiers.isEmpty()) modifiers.append(" ");
                modifiers.append(token);
            }
        }
        if (!modifiers.isEmpty()) {
            builder.modifiers(modifiers.toString());
        }
    }

    /**
     * Parse type declaration (e.g., "public class ChatClient").
     */
    private void parseTypeDeclaration(String declaration, ParsedClassDto.Builder builder) {
        if (declaration == null || declaration.isBlank()) return;

        String lower = declaration.toLowerCase();

        // Determine kind
        if (lower.contains("interface")) {
            builder.kind(JavadocClassKind.INTERFACE);
        } else if (lower.contains("enum")) {
            builder.kind(JavadocClassKind.ENUM);
        } else if (lower.contains("@interface") || lower.contains("annotation")) {
            builder.kind(JavadocClassKind.ANNOTATION);
        } else if (lower.contains("record")) {
            builder.kind(JavadocClassKind.RECORD);
        } else {
            builder.kind(JavadocClassKind.CLASS);
        }

        // Extract modifiers
        StringBuilder modifiers = new StringBuilder();
        String[] tokens = declaration.split("\\s+");
        String simpleName = null;

        for (String token : tokens) {
            String t = token.toLowerCase();
            if (t.equals("public") || t.equals("protected") || t.equals("private") ||
                    t.equals("abstract") || t.equals("final") || t.equals("static") ||
                    t.equals("sealed") || t.equals("non-sealed")) {
                if (!modifiers.isEmpty()) modifiers.append(" ");
                modifiers.append(token);
            } else if (!t.equals("class") && !t.equals("interface") && !t.equals("enum") &&
                    !t.equals("@interface") && !t.equals("record") && !token.startsWith("<")) {
                simpleName = token;
            }
        }

        if (simpleName != null) {
            builder.simpleName(simpleName);
            // Try to build FQCN from breadcrumb or package info
        }
        builder.modifiers(modifiers.isEmpty() ? null : modifiers.toString());
    }

    /**
     * Parse inheritance (extends, implements).
     */
    private void parseInheritance(Document doc, ParsedClassDto.Builder builder) {
        // Extract extends
        Element extendsElement = doc.selectFirst(".extends-implements .type-signature a, .inheritance a");
        if (extendsElement != null) {
            builder.superClass(extendsElement.text());
        }

        // Extract implements
        List<String> interfaces = new ArrayList<>();
        Elements implementsLinks = doc.select(".extends-implements .type-signature a, dd.implements a");
        for (Element link : implementsLinks) {
            String text = link.text();
            if (!text.equals(builder.build().superClass())) {
                interfaces.add(text);
            }
        }
        builder.interfaces(interfaces);
    }

    /**
     * Extract class-level annotations.
     */
    private List<String> extractAnnotations(Document doc) {
        List<String> annotations = new ArrayList<>();
        Elements annotationElements = doc.select(".type-signature .annotations a, .annotations a");
        for (Element ann : annotationElements) {
            String text = ann.text();
            if (text.startsWith("@")) {
                annotations.add(text);
            }
        }
        return annotations;
    }

    /**
     * Extract methods from the class page.
     */
    private List<ParsedMethodDto> extractMethods(Document doc) {
        List<ParsedMethodDto> methods = new ArrayList<>();

        // Try Java 11+ format
        Elements methodDetails = doc.select("#method-detail .member-list > li, .method-details .member-list > li");
        if (methodDetails.isEmpty()) {
            // Java 8 format
            methodDetails = doc.select(".methods .blockList > li");
        }

        for (Element methodElement : methodDetails) {
            try {
                ParsedMethodDto method = parseMethodElement(methodElement);
                if (method != null) {
                    methods.add(method);
                }
            } catch (Exception e) {
                log.debug("Failed to parse method element: {}", e.getMessage());
            }
        }

        // If no detailed methods, try summary table
        if (methods.isEmpty()) {
            Elements summaryRows = doc.select("#method-summary .summary-table .col-second, .memberSummary td.colSecond");
            for (Element row : summaryRows) {
                Element link = row.selectFirst("a");
                if (link != null) {
                    methods.add(ParsedMethodDto.builder()
                            .name(link.text())
                            .signature(row.text())
                            .build());
                }
            }
        }

        return methods;
    }

    /**
     * Parse a single method element.
     */
    private ParsedMethodDto parseMethodElement(Element element) {
        var builder = ParsedMethodDto.builder();

        // Get method signature
        Element signature = element.selectFirst(".member-signature, .signature, pre");
        if (signature != null) {
            String sigText = signature.text();
            builder.signature(sigText);
            parseMethodSignature(sigText, builder);
        }

        // Get description
        Element block = element.selectFirst(".block");
        if (block != null) {
            builder.description(cleanHtml(block.html()));
            builder.summary(extractFirstSentence(block.text()));
        }

        // Check deprecation
        Element deprecatedBlock = element.selectFirst(".deprecation-block");
        if (deprecatedBlock != null) {
            builder.deprecated(true);
            Element comment = deprecatedBlock.selectFirst(".deprecation-comment");
            if (comment != null) {
                builder.deprecatedMessage(comment.text());
            }
        }

        // Extract parameters from @param tags
        builder.parameters(extractParameters(element));

        // Extract throws
        builder.throwsList(extractThrows(element));

        // Extract annotations
        Elements annotations = element.select(".annotations a");
        List<String> annList = new ArrayList<>();
        for (Element ann : annotations) {
            annList.add(ann.text());
        }
        builder.annotations(annList);

        return builder.build();
    }

    /**
     * Parse method signature to extract name, return type, etc.
     */
    private void parseMethodSignature(String signature, ParsedMethodDto.Builder builder) {
        if (signature == null) return;

        // Clean up signature
        signature = signature.replaceAll("\\s+", " ").trim();

        // Extract return type and method name
        Matcher matcher = SIGNATURE_PATTERN.matcher(signature);
        if (matcher.find()) {
            String beforeName = matcher.group(1).trim();
            String methodName = matcher.group(2);

            builder.name(methodName);

            // Return type is usually the last word before method name
            String[] parts = beforeName.split("\\s+");
            if (parts.length > 0) {
                builder.returnType(parts[parts.length - 1]);
            }
        }
    }

    /**
     * Extract parameters from @param tags.
     */
    private List<MethodParameterDto> extractParameters(Element element) {
        List<MethodParameterDto> params = new ArrayList<>();

        Elements paramTags = element.select("dt:contains(Parameters:) + dd, dl.notes dt:contains(Parameters) ~ dd");
        for (Element dd : paramTags) {
            String text = dd.text();
            if (text.contains(" - ")) {
                String[] parts = text.split(" - ", 2);
                String nameAndType = parts[0].trim();
                String desc = parts.length > 1 ? parts[1].trim() : null;

                // Try to split name and type
                String name = nameAndType;
                String type = null;
                if (nameAndType.contains(" ")) {
                    String[] np = nameAndType.split("\\s+", 2);
                    type = np[0];
                    name = np[1];
                }
                params.add(new MethodParameterDto(name, type, desc));
            }
        }

        return params;
    }

    /**
     * Extract throws from signature or tags.
     */
    private List<String> extractThrows(Element element) {
        List<String> throwsList = new ArrayList<>();

        Elements throwsTags = element.select("dt:contains(Throws:) + dd a, dl.notes dt:contains(Throws) ~ dd a");
        for (Element a : throwsTags) {
            throwsList.add(a.text());
        }

        return throwsList;
    }

    /**
     * Extract fields from the class page.
     */
    private List<ParsedFieldDto> extractFields(Document doc) {
        List<ParsedFieldDto> fields = new ArrayList<>();

        // Try field detail section
        Elements fieldDetails = doc.select("#field-detail .member-list > li, .field-details .member-list > li");
        if (fieldDetails.isEmpty()) {
            fieldDetails = doc.select(".fields .blockList > li");
        }

        for (Element fieldElement : fieldDetails) {
            try {
                ParsedFieldDto field = parseFieldElement(fieldElement);
                if (field != null) {
                    fields.add(field);
                }
            } catch (Exception e) {
                log.debug("Failed to parse field element: {}", e.getMessage());
            }
        }

        return fields;
    }

    /**
     * Parse a single field element.
     */
    private ParsedFieldDto parseFieldElement(Element element) {
        var builder = ParsedFieldDto.builder();

        Element signature = element.selectFirst(".member-signature, pre");
        if (signature != null) {
            String sigText = signature.text();
            parseFieldSignature(sigText, builder);
        }

        Element block = element.selectFirst(".block");
        if (block != null) {
            builder.summary(extractFirstSentence(block.text()));
        }

        Element deprecatedBlock = element.selectFirst(".deprecation-block");
        if (deprecatedBlock != null) {
            builder.deprecated(true);
        }

        return builder.build();
    }

    /**
     * Parse field signature.
     */
    private void parseFieldSignature(String signature, ParsedFieldDto.Builder builder) {
        if (signature == null) return;

        String[] parts = signature.trim().split("\\s+");
        if (parts.length >= 2) {
            StringBuilder modifiers = new StringBuilder();
            String type = null;
            String name = null;

            for (String part : parts) {
                String lower = part.toLowerCase();
                if (lower.equals("public") || lower.equals("protected") || lower.equals("private") ||
                        lower.equals("static") || lower.equals("final") || lower.equals("transient") ||
                        lower.equals("volatile")) {
                    if (!modifiers.isEmpty()) modifiers.append(" ");
                    modifiers.append(part);
                } else if (type == null) {
                    type = part;
                } else {
                    name = part.replace(";", "");
                }
            }

            builder.modifiers(modifiers.isEmpty() ? null : modifiers.toString());
            builder.type(type);
            builder.name(name);
        }
    }

    /**
     * Extract constructors from the class page.
     */
    private List<ParsedConstructorDto> extractConstructors(Document doc) {
        List<ParsedConstructorDto> constructors = new ArrayList<>();

        Elements constructorDetails = doc.select("#constructor-detail .member-list > li, .constructor-details .member-list > li");
        if (constructorDetails.isEmpty()) {
            constructorDetails = doc.select(".constructors .blockList > li");
        }

        for (Element ctorElement : constructorDetails) {
            try {
                ParsedConstructorDto ctor = parseConstructorElement(ctorElement);
                if (ctor != null) {
                    constructors.add(ctor);
                }
            } catch (Exception e) {
                log.debug("Failed to parse constructor element: {}", e.getMessage());
            }
        }

        return constructors;
    }

    /**
     * Parse a single constructor element.
     */
    private ParsedConstructorDto parseConstructorElement(Element element) {
        var builder = ParsedConstructorDto.builder();

        Element signature = element.selectFirst(".member-signature, pre");
        if (signature != null) {
            builder.signature(signature.text());
        }

        Element block = element.selectFirst(".block");
        if (block != null) {
            builder.summary(extractFirstSentence(block.text()));
        }

        Element deprecatedBlock = element.selectFirst(".deprecation-block");
        if (deprecatedBlock != null) {
            builder.deprecated(true);
        }

        builder.parameters(extractParameters(element));
        builder.throwsList(extractThrows(element));

        return builder.build();
    }

    /**
     * Clean HTML content, converting to readable text.
     */
    public String cleanHtml(String html) {
        if (html == null) return null;

        return Jsoup.parse(html)
                .text()
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Extract first sentence from text.
     */
    public String extractFirstSentence(String text) {
        if (text == null || text.isBlank()) return null;

        // Find first period followed by space or end
        int dotIndex = text.indexOf(". ");
        if (dotIndex > 0 && dotIndex < 300) {
            return text.substring(0, dotIndex + 1).trim();
        }

        // If no period, take first 200 chars
        if (text.length() > 200) {
            return text.substring(0, 200).trim() + "...";
        }

        return text.trim();
    }

    /**
     * Extract package name from URL.
     */
    private String extractPackageFromUrl(String url) {
        if (url == null) return "";
        // Remove base and file, get package path
        String path = url.replaceAll("^.*/api/", "").replace("/package-summary.html", "");
        return path.replace("/", ".");
    }

    /**
     * Get simple class name from path.
     */
    private String simpleClassName(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }
}
