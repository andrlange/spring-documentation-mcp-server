package com.spring.mcp.service.javadoc;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.javadoc.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for storing and retrieving Javadoc data from the database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mcp.features.javadocs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JavadocStorageService {

    private final JavadocPackageRepository packageRepository;
    private final JavadocClassRepository classRepository;
    private final JavadocMethodRepository methodRepository;
    private final JavadocFieldRepository fieldRepository;
    private final JavadocConstructorRepository constructorRepository;

    /**
     * Save a package if it doesn't exist, otherwise return the existing one.
     * Javadocs are immutable per version, so existing packages are never updated.
     *
     * @param dto         Parsed package DTO
     * @param libraryName Library name (e.g., "spring-ai")
     * @param version     Library version (e.g., "1.1.1")
     * @return Saved or existing package entity
     */
    @Transactional
    public JavadocPackage savePackage(ParsedPackageDto dto, String libraryName, String version) {
        // Check if package already exists - if so, return it without updating
        Optional<JavadocPackage> existing = packageRepository
                .findByLibraryNameAndVersionAndPackageName(libraryName, version, dto.packageName());

        if (existing.isPresent()) {
            log.trace("Package already exists, skipping: {}/{}/{}", libraryName, version, dto.packageName());
            return existing.get();
        }

        // Create new package
        JavadocPackage pkg = new JavadocPackage();
        pkg.setLibraryName(libraryName);
        pkg.setVersion(version);
        pkg.setPackageName(dto.packageName());
        pkg.setSummary(dto.summary());
        pkg.setDescription(dto.description());
        pkg.setSourceUrl(dto.sourceUrl());

        log.debug("Creating new package: {}/{}/{}", libraryName, version, dto.packageName());
        return packageRepository.save(pkg);
    }

    /**
     * Save a class with all its members if it doesn't exist.
     * Javadocs are immutable per version, so existing classes are never updated.
     *
     * @param dto     Parsed class DTO
     * @param pkg     Parent package entity
     * @return Saved class entity, or null if class is invalid or already exists
     */
    @Transactional
    public JavadocClass saveClass(ParsedClassDto dto, JavadocPackage pkg) {
        // Validate class has a valid name
        String simpleName = dto.simpleName();
        if (simpleName == null || simpleName.isBlank() ||
            simpleName.contains("-") || simpleName.equals(pkg.getPackageName())) {
            log.debug("Skipping invalid class: simpleName='{}' in package '{}'",
                    simpleName, pkg.getPackageName());
            return null;
        }

        // Build FQCN if not complete
        String fqcn = dto.fqcn();
        if (fqcn == null || !fqcn.contains(".")) {
            fqcn = pkg.getPackageName() + "." + simpleName;
        }

        // Check if class already exists - if so, skip it entirely
        if (classRepository.existsByLibraryVersionAndFqcn(pkg.getLibraryName(), pkg.getVersion(), fqcn)) {
            log.trace("Class already exists, skipping: {}/{}/{}", pkg.getLibraryName(), pkg.getVersion(), fqcn);
            return null;
        }

        // Create new class
        JavadocClass cls = new JavadocClass();
        cls.setJavadocPackage(pkg);
        cls.setFqcn(fqcn);
        cls.setSimpleName(dto.simpleName());
        cls.setKind(dto.kind());
        cls.setModifiers(dto.modifiers());
        cls.setSummary(dto.summary());
        cls.setDescription(dto.description());
        cls.setSuperClass(dto.superClass());
        cls.setInterfaces(new ArrayList<>(dto.interfaces()));
        cls.setSourceUrl(dto.sourceUrl());
        cls.setDeprecated(dto.deprecated());
        cls.setDeprecatedMessage(dto.deprecatedMessage());
        cls.setAnnotations(new ArrayList<>(dto.annotations()));

        // Save class first to get ID
        cls = classRepository.save(cls);

        // Add methods
        for (ParsedMethodDto methodDto : dto.methods()) {
            JavadocMethod method = createMethod(methodDto, cls);
            cls.getMethods().add(method);
        }

        // Add fields
        for (ParsedFieldDto fieldDto : dto.fields()) {
            JavadocField field = createField(fieldDto, cls);
            cls.getFields().add(field);
        }

        // Add constructors
        for (ParsedConstructorDto ctorDto : dto.constructors()) {
            JavadocConstructor ctor = createConstructor(ctorDto, cls);
            cls.getConstructors().add(ctor);
        }

        log.debug("Created new class: {}/{}/{}", pkg.getLibraryName(), pkg.getVersion(), fqcn);
        return classRepository.save(cls);
    }

    /**
     * Create a method entity from DTO.
     */
    private JavadocMethod createMethod(ParsedMethodDto dto, JavadocClass cls) {
        JavadocMethod method = new JavadocMethod();
        method.setJavadocClass(cls);
        method.setName(dto.name());
        method.setSignature(dto.signature());
        method.setReturnType(dto.returnType());
        method.setSummary(dto.summary());
        method.setDescription(dto.description());
        method.setDeprecated(dto.deprecated());
        method.setDeprecatedMessage(dto.deprecatedMessage());
        method.setThrowsList(new ArrayList<>(dto.throwsList()));
        method.setAnnotations(new ArrayList<>(dto.annotations()));

        // Convert parameters to JSON-compatible format
        List<Map<String, String>> params = new ArrayList<>();
        for (MethodParameterDto p : dto.parameters()) {
            Map<String, String> param = new HashMap<>();
            param.put("name", p.name());
            param.put("type", p.type());
            param.put("description", p.description());
            params.add(param);
        }
        method.setParameters(params);

        return method;
    }

    /**
     * Create a field entity from DTO.
     */
    private JavadocField createField(ParsedFieldDto dto, JavadocClass cls) {
        JavadocField field = new JavadocField();
        field.setJavadocClass(cls);
        field.setName(dto.name());
        field.setType(dto.type());
        field.setModifiers(dto.modifiers());
        field.setSummary(dto.summary());
        field.setDeprecated(dto.deprecated());
        field.setConstantValue(dto.constantValue());
        return field;
    }

    /**
     * Create a constructor entity from DTO.
     */
    private JavadocConstructor createConstructor(ParsedConstructorDto dto, JavadocClass cls) {
        JavadocConstructor ctor = new JavadocConstructor();
        ctor.setJavadocClass(cls);
        ctor.setSignature(dto.signature());
        ctor.setSummary(dto.summary());
        ctor.setDeprecated(dto.deprecated());
        ctor.setThrowsList(new ArrayList<>(dto.throwsList()));
        ctor.setAnnotations(new ArrayList<>(dto.annotations()));

        // Convert parameters to JSON-compatible format
        List<Map<String, String>> params = new ArrayList<>();
        for (MethodParameterDto p : dto.parameters()) {
            Map<String, String> param = new HashMap<>();
            param.put("name", p.name());
            param.put("type", p.type());
            param.put("description", p.description());
            params.add(param);
        }
        ctor.setParameters(params);

        return ctor;
    }

    /**
     * Clear all data for a library version.
     *
     * @param libraryName Library name
     * @param version     Version
     * @return Number of packages deleted
     */
    @Transactional
    public int clearLibraryVersion(String libraryName, String version) {
        List<JavadocPackage> packages = packageRepository.findByLibraryNameAndVersion(libraryName, version);
        int count = packages.size();
        if (count > 0) {
            packageRepository.deleteByLibraryNameAndVersion(libraryName, version);
            log.info("Cleared {} packages for {}/{}", count, libraryName, version);
        }
        return count;
    }

    /**
     * Check if documentation exists for a library version.
     */
    public boolean existsForVersion(String libraryName, String version) {
        return packageRepository.existsByLibraryNameAndVersion(libraryName, version);
    }

    /**
     * Check if a specific class exists for a library version.
     *
     * @param libraryName Library name
     * @param version     Version
     * @param fqcn        Fully qualified class name
     * @return true if the class exists
     */
    public boolean classExists(String libraryName, String version, String fqcn) {
        return classRepository.existsByLibraryVersionAndFqcn(libraryName, version, fqcn);
    }

    /**
     * Check if a specific package exists for a library version.
     * If the package exists, it means we've already synced it completely
     * since Javadocs are immutable per version.
     *
     * @param libraryName Library name
     * @param version     Version
     * @param packageName Package name
     * @return true if the package exists
     */
    public boolean packageExists(String libraryName, String version, String packageName) {
        return packageRepository.existsByLibraryNameAndVersionAndPackageName(libraryName, version, packageName);
    }

    /**
     * Get statistics for a library version.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getVersionStats(String libraryName, String version) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("packages", packageRepository.countByLibraryNameAndVersion(libraryName, version));
        stats.put("classes", classRepository.countByLibraryAndVersion(libraryName, version));
        stats.put("methods", methodRepository.countByLibraryAndVersion(libraryName, version));
        stats.put("fields", fieldRepository.countByLibraryAndVersion(libraryName, version));
        stats.put("constructors", constructorRepository.countByLibraryAndVersion(libraryName, version));
        return stats;
    }

    /**
     * Get all library names.
     */
    @Transactional(readOnly = true)
    public List<String> getLibraryNames() {
        return packageRepository.findDistinctLibraryNames();
    }

    /**
     * Get all versions for a library.
     */
    @Transactional(readOnly = true)
    public List<String> getVersions(String libraryName) {
        return packageRepository.findVersionsByLibraryName(libraryName);
    }
}
