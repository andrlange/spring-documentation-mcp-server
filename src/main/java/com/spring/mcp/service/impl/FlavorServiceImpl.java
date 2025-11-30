package com.spring.mcp.service.impl;

import com.spring.mcp.model.dto.flavor.CategoryStatsDto;
import com.spring.mcp.model.dto.flavor.FlavorDto;
import com.spring.mcp.model.dto.flavor.FlavorSummaryDto;
import com.spring.mcp.model.entity.Flavor;
import com.spring.mcp.model.enums.FlavorCategory;
import com.spring.mcp.repository.FlavorRepository;
import com.spring.mcp.service.FlavorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of FlavorService.
 *
 * @author Spring MCP Server
 * @version 1.2.0
 * @since 2025-11-30
 */
@Service
@Transactional
public class FlavorServiceImpl implements FlavorService {

    private static final Logger log = LoggerFactory.getLogger(FlavorServiceImpl.class);

    private final FlavorRepository flavorRepository;

    public FlavorServiceImpl(FlavorRepository flavorRepository) {
        this.flavorRepository = flavorRepository;
    }

    @Override
    public FlavorDto create(FlavorDto dto, String username) {
        log.info("Creating new flavor: {} by user: {}", dto.getUniqueName(), username);

        if (flavorRepository.existsByUniqueName(dto.getUniqueName())) {
            throw new IllegalArgumentException("Flavor with name '" + dto.getUniqueName() + "' already exists");
        }

        Flavor flavor = mapToEntity(dto);
        flavor.setCreatedBy(username);
        flavor.setUpdatedBy(username);

        Flavor saved = flavorRepository.save(flavor);
        log.info("Created flavor with id: {}", saved.getId());
        return mapToDto(saved);
    }

    @Override
    public FlavorDto update(Long id, FlavorDto dto, String username) {
        log.info("Updating flavor id: {} by user: {}", id, username);

        Flavor existing = flavorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + id));

        // Check unique name if changed
        if (!existing.getUniqueName().equals(dto.getUniqueName())
            && flavorRepository.existsByUniqueName(dto.getUniqueName())) {
            throw new IllegalArgumentException("Flavor with name '" + dto.getUniqueName() + "' already exists");
        }

        existing.setUniqueName(dto.getUniqueName());
        existing.setDisplayName(dto.getDisplayName());
        existing.setCategory(dto.getCategory());
        existing.setPatternName(dto.getPatternName());
        existing.setContent(dto.getContent());
        existing.setDescription(dto.getDescription());
        existing.setTags(dto.getTags() != null ? dto.getTags() : new ArrayList<>());
        existing.setMetadata(dto.getMetadata() != null ? dto.getMetadata() : new HashMap<>());
        existing.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        existing.setUpdatedBy(username);

        return mapToDto(flavorRepository.save(existing));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting flavor id: {}", id);
        flavorRepository.deleteById(id);
    }

    @Override
    public void toggleActive(Long id, boolean active, String username) {
        log.info("Toggling flavor id: {} to active: {} by user: {}", id, active, username);

        Flavor flavor = flavorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + id));
        flavor.setIsActive(active);
        flavor.setUpdatedBy(username);
        flavorRepository.save(flavor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FlavorDto> findById(Long id) {
        return flavorRepository.findById(id).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FlavorDto> findByUniqueName(String uniqueName) {
        return flavorRepository.findByUniqueNameAndIsActiveTrue(uniqueName).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlavorDto> findAll() {
        return flavorRepository.findAllOrdered().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlavorDto> findAllActive() {
        return flavorRepository.findAllActiveOrdered().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlavorDto> findByCategory(FlavorCategory category) {
        return flavorRepository.findByCategoryAndIsActiveTrue(category).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlavorSummaryDto> search(String query, FlavorCategory category, List<String> tags, int limit) {
        List<Flavor> results;

        if (query != null && !query.isBlank()) {
            if (category != null) {
                results = flavorRepository.searchByQueryAndCategory(query, category.name(), limit);
            } else {
                results = flavorRepository.searchByQuery(query, limit);
            }
        } else if (category != null) {
            results = flavorRepository.findByCategoryAndIsActiveTrue(category);
        } else if (tags != null && !tags.isEmpty()) {
            results = flavorRepository.findByTagsContaining(tags.toArray(new String[0]));
        } else {
            results = flavorRepository.findAllActiveOrdered();
        }

        return results.stream()
            .limit(limit)
            .map(this::mapToSummaryDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlavorDto> searchByTags(List<String> tags) {
        return flavorRepository.findByTagsContaining(tags.toArray(new String[0])).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlavorDto> findArchitectureByTechnologies(List<String> slugs) {
        return flavorRepository.findArchitectureByTechnologySlugs(slugs.toArray(new String[0])).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlavorDto> findComplianceByRules(List<String> rules) {
        return flavorRepository.findComplianceByRules(rules.toArray(new String[0])).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FlavorDto> findAgentConfigurationByUseCase(String useCase) {
        return flavorRepository.findAgentByUseCase(useCase).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FlavorDto> findInitializationByUseCase(String useCase) {
        return flavorRepository.findInitializationByUseCase(useCase).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FlavorCategory, Long> getCategoryCounts() {
        Map<FlavorCategory, Long> counts = new EnumMap<>(FlavorCategory.class);
        for (FlavorCategory category : FlavorCategory.values()) {
            counts.put(category, flavorRepository.countByCategoryAndIsActiveTrue(category));
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryStatsDto getStatistics() {
        Map<FlavorCategory, Long> categoryCounts = getCategoryCounts();
        long totalActive = flavorRepository.countByIsActiveTrue();
        long totalInactive = flavorRepository.countByIsActiveFalse();
        return CategoryStatsDto.builder()
            .totalActive(totalActive)
            .totalInactive(totalInactive)
            .categoryCounts(categoryCounts)
            .build();
    }

    @Override
    public FlavorDto importFromMarkdown(String content, FlavorCategory category, String username) {
        log.info("Importing markdown flavor for category: {} by user: {}", category, username);

        // Extract title from first H1
        String title = extractTitle(content);
        String uniqueName = generateUniqueName(title);
        String description = extractDescription(content);

        FlavorDto dto = FlavorDto.builder()
            .uniqueName(uniqueName)
            .displayName(title)
            .category(category)
            .content(content)
            .description(description)
            .isActive(false) // Imported as draft
            .tags(new ArrayList<>())
            .metadata(new HashMap<>())
            .build();

        return create(dto, username);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportToMarkdown(Long id) {
        return flavorRepository.findById(id)
            .map(Flavor::getContent)
            .orElseThrow(() -> new IllegalArgumentException("Flavor not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUniqueNameAvailable(String uniqueName, Long excludeId) {
        Optional<Flavor> existing = flavorRepository.findByUniqueName(uniqueName);
        return existing.isEmpty() || existing.get().getId().equals(excludeId);
    }

    // Helper methods
    private String extractTitle(String content) {
        Pattern pattern = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Untitled Flavor";
    }

    private String extractDescription(String content) {
        // Get first paragraph after title
        String[] lines = content.split("\n");
        StringBuilder desc = new StringBuilder();
        boolean foundTitle = false;

        for (String line : lines) {
            if (line.startsWith("# ")) {
                foundTitle = true;
                continue;
            }
            if (foundTitle && !line.isBlank() && !line.startsWith("#")) {
                desc.append(line.trim());
                if (desc.length() > 200) break;
            }
        }
        return desc.toString();
    }

    private String generateUniqueName(String title) {
        String base = title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        if (base.length() > 50) {
            base = base.substring(0, 50);
        }

        String uniqueName = base;
        int counter = 1;
        while (flavorRepository.existsByUniqueName(uniqueName)) {
            uniqueName = base + "-" + counter++;
        }
        return uniqueName;
    }

    private FlavorDto mapToDto(Flavor entity) {
        return FlavorDto.builder()
            .id(entity.getId())
            .uniqueName(entity.getUniqueName())
            .displayName(entity.getDisplayName())
            .category(entity.getCategory())
            .patternName(entity.getPatternName())
            .content(entity.getContent())
            .description(entity.getDescription())
            .contentHash(entity.getContentHash())
            .tags(entity.getTags())
            .metadata(entity.getMetadata())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    private FlavorSummaryDto mapToSummaryDto(Flavor entity) {
        return FlavorSummaryDto.builder()
            .id(entity.getId())
            .uniqueName(entity.getUniqueName())
            .displayName(entity.getDisplayName())
            .category(entity.getCategory())
            .patternName(entity.getPatternName())
            .description(entity.getDescription())
            .tags(entity.getTags())
            .isActive(entity.getIsActive())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private Flavor mapToEntity(FlavorDto dto) {
        return Flavor.builder()
            .uniqueName(dto.getUniqueName())
            .displayName(dto.getDisplayName())
            .category(dto.getCategory())
            .patternName(dto.getPatternName())
            .content(dto.getContent())
            .description(dto.getDescription())
            .tags(dto.getTags() != null ? dto.getTags() : new ArrayList<>())
            .metadata(dto.getMetadata() != null ? dto.getMetadata() : new HashMap<>())
            .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
            .build();
    }
}
