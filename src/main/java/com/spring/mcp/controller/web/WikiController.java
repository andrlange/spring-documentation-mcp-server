package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.WikiMigrationGuide;
import com.spring.mcp.model.entity.WikiReleaseNotes;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.service.wiki.WikiService;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for Wiki pages (Release Notes and Migration Guides).
 * Displays content from the Spring Boot GitHub wiki.
 *
 * @author Spring MCP Server
 * @version 1.7.0
 * @since 2026-01-06
 */
@Controller
@RequestMapping("/wiki")
@Slf4j
public class WikiController {

    private final WikiService wikiService;
    private final SpringBootVersionRepository springBootVersionRepository;

    // Flexmark parser and renderer for Markdown to HTML
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public WikiController(WikiService wikiService, SpringBootVersionRepository springBootVersionRepository) {
        this.wikiService = wikiService;
        this.springBootVersionRepository = springBootVersionRepository;

        // Initialize Flexmark
        MutableDataSet options = new MutableDataSet();
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Display wiki index page with release notes and migration guides.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String index(
            @RequestParam(required = false) String q,
            Model model) {

        log.debug("Wiki page: query={}", q);

        model.addAttribute("activePage", "wiki");
        model.addAttribute("pageTitle", "Wiki");

        // Get statistics
        WikiService.WikiStats stats = wikiService.getStatistics();
        model.addAttribute("stats", stats);

        // Get the actual latest GA Spring Boot version
        Optional<SpringBootVersion> latestGaVersion = springBootVersionRepository.findByIsCurrentTrue();
        model.addAttribute("latestGaVersion", latestGaVersion.map(SpringBootVersion::getVersion).orElse("N/A"));

        List<WikiReleaseNotes> releaseNotes;
        List<WikiMigrationGuide> migrationGuides;

        if (q != null && !q.isBlank()) {
            // Search mode
            model.addAttribute("query", q);
            releaseNotes = wikiService.searchReleaseNotes(q, 20);
            migrationGuides = wikiService.searchMigrationGuides(q, 20);
        } else {
            // List all
            releaseNotes = wikiService.findAllReleaseNotes();
            migrationGuides = wikiService.findAllMigrationGuides();
        }

        model.addAttribute("releaseNotes", releaseNotes);
        model.addAttribute("migrationGuides", migrationGuides);

        return "wiki/index";
    }

    /**
     * Display release notes for a specific version.
     */
    @GetMapping("/release-notes/{version}")
    @PreAuthorize("isAuthenticated()")
    public String viewReleaseNotes(
            @PathVariable String version,
            Model model) {

        log.debug("Viewing release notes for version: {}", version);

        model.addAttribute("activePage", "wiki");

        Optional<WikiReleaseNotes> releaseNotes = wikiService.findReleaseNotesByVersion(version);

        if (releaseNotes.isEmpty()) {
            model.addAttribute("error", "Release notes for version " + version + " not found.");
            return "error/404";
        }

        WikiReleaseNotes notes = releaseNotes.get();

        model.addAttribute("pageTitle", notes.getTitle());
        model.addAttribute("title", notes.getTitle());
        model.addAttribute("type", "release-notes");
        model.addAttribute("version", version);
        model.addAttribute("sourceUrl", notes.getSourceUrl());
        model.addAttribute("lastModified", notes.getWikiLastModified());

        // Convert Markdown to HTML
        String htmlContent = convertMarkdownToHtml(notes.getContentMarkdown());
        model.addAttribute("content", htmlContent);

        return "wiki/view";
    }

    /**
     * Display migration guide between two versions.
     */
    @GetMapping("/migration-guide/{from}/{to}")
    @PreAuthorize("isAuthenticated()")
    public String viewMigrationGuide(
            @PathVariable String from,
            @PathVariable String to,
            Model model) {

        log.debug("Viewing migration guide: {} -> {}", from, to);

        model.addAttribute("activePage", "wiki");

        Optional<WikiMigrationGuide> migrationGuide = wikiService.findMigrationGuide(from, to);

        if (migrationGuide.isEmpty()) {
            model.addAttribute("error", "Migration guide from " + from + " to " + to + " not found.");
            return "error/404";
        }

        WikiMigrationGuide guide = migrationGuide.get();

        model.addAttribute("pageTitle", guide.getTitle());
        model.addAttribute("title", guide.getTitle());
        model.addAttribute("type", "migration-guide");
        model.addAttribute("version", guide.getMigrationPath());
        model.addAttribute("fromVersion", from);
        model.addAttribute("toVersion", to);
        model.addAttribute("sourceUrl", guide.getSourceUrl());
        model.addAttribute("lastModified", guide.getWikiLastModified());

        // Convert Markdown to HTML
        String htmlContent = convertMarkdownToHtml(guide.getContentMarkdown());
        model.addAttribute("content", htmlContent);

        return "wiki/view";
    }

    /**
     * Convert Markdown content to HTML.
     */
    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        try {
            Node document = markdownParser.parse(markdown);
            return htmlRenderer.render(document);
        } catch (Exception e) {
            log.error("Error converting markdown to HTML: {}", e.getMessage());
            return "<pre>" + markdown + "</pre>";
        }
    }
}
