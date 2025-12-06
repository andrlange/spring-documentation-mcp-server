---
unique-name: auto-configuration
display-name: Modular Auto-Configuration
category: ARCHITECTURE
pattern-name: Modular Dependency Management
description: Modular auto-configuration approach where auto-configurations are split into separate focused modules for explicit dependency management
tags: auto-configuration, modular, migration, dependencies, starters, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# Modular Auto-Configuration

Spring Boot 4 introduces **modular auto-configuration** - a breaking change where auto-configurations are split into separate,
focused modules instead of being bundled together.

## Overview

Auto-configurations are now split into individual modules, giving you control over exactly what gets included in your application. This reduces footprint but requires more explicit dependency management.

### Key Concepts

- **Breaking change**: Features that "just worked" before now require explicit dependencies
- **Modular approach**: Add only the auto-configuration modules you need
- **Classic compatibility**: `spring-boot-autoconfigure-classic` maintains Spring Boot 3.x behavior
- **Smaller footprint**: Better for microservices and cloud deployments
- **Explicit dependencies**: Clearer dependency management

## Resources

### Official Documentation
- [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#classic-starters)

## Migration Options

### Option 1: Classic Module (Quick Migration)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure-classic</artifactId>
</dependency>
```

### Option 2: Modular (Recommended for New Projects)
```xml
<!-- Only add what you need -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-restclient</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure-data-jpa</artifactId>
</dependency>
```

## Common Migration Issues

| Missing Feature | Add This Dependency |
|-----------------|---------------------|
| RestClient | `spring-boot-starter-restclient` |
| JPA/Hibernate | `spring-boot-autoconfigure-data-jpa` |
| Security | `spring-boot-autoconfigure-security` |
