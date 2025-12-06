---
unique-name: jackson3
display-name: Jackson 3 Support
category: ARCHITECTURE
pattern-name: JSON Serialization
description: Jackson 3 integration with immutable builder-based configuration, ISO-8601 date defaults, and JSON Views for response filtering
tags: jackson, json, serialization, json-views, iso-8601, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# Jackson 3 Support

Spring Boot 4 introduces **Jackson 3 support** with new architecture, improved defaults, and the modern `hint()` method
for cleaner JSON view filtering.

## Overview

Jackson 3 brings significant improvements including immutable builder-based configuration, ISO-8601 date defaults,
unchecked exceptions for better lambda/stream integration, and new `tools.jackson` packages. Spring Boot 4 provides
seamless integration with auto-configured `JsonMapper` beans.

### Key Concepts

- **Jackson 3 architecture**: New `tools.jackson` packages with immutable builder-based config
- **ISO-8601 defaults**: Dates serialize as ISO strings instead of numeric timestamps
- **Unchecked exceptions**: Better lambda/stream integration
- **`hint()` method**: Clean client-side request filtering (replaces `MappingJacksonValue`)
- **JSON Views**: Eliminate DTO proliferation with `@JsonView` hierarchies
- **Compatibility bridge**: Jackson 3.0.1 databind with jackson-annotations 2.20

## Resources

### GitHub Repository
https://github.com/danvega/donut-shop

### Video Tutorial
https://youtu.be/4cvP_qroLH4

### Blog Post

### Official Documentation
- [Introducing Jackson 3 Support in Spring](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring)
- [Jackson Project](https://github.com/FasterXML/jackson)

## Example Usage

### JSON View Hierarchy
```java
public class Views {
    public interface Summary {}
    public interface Public extends Summary {}
    public interface Internal extends Public {}
}
```

### Model with Views
```java
public record Donut(
    @JsonView(Views.Summary.class) Long id,
    @JsonView(Views.Summary.class) String name,
    @JsonView(Views.Public.class) String description,
    @JsonView(Views.Public.class) BigDecimal price,
    @JsonView(Views.Internal.class) Integer stockCount,
    @JsonView(Views.Internal.class) LocalDateTime createdAt
) {}
```

### Controller with Views
```java
@RestController
@RequestMapping("/api/donuts")
public class DonutController {

    @GetMapping
    @JsonView(Views.Summary.class)
    public List<Donut> getSummary() {
        return donutService.findAll();
    }

    @GetMapping("/public")
    @JsonView(Views.Public.class)
    public List<Donut> getPublic() {
        return donutService.findAll();
    }

    @GetMapping("/internal")
    @JsonView(Views.Internal.class)
    public List<Donut> getInternal() {
        return donutService.findAll();
    }
}
```

### Configuration Properties
```properties
# application.properties
spring.jackson.use-jackson2-defaults=false
spring.jackson.serialization.indent-output=true
```

## What's New in Jackson 3

| Feature | Jackson 2 | Jackson 3 |
|---------|-----------|-----------|
| Date format | Numeric timestamp | ISO-8601 string |
| Exceptions | Checked | Unchecked |
| Configuration | Mutable | Immutable builders |
| Packages | `com.fasterxml` | `tools.jackson` |
