---
unique-name: api-versioning
display-name: API Versioning
category: ARCHITECTURE
pattern-name: API Version Management
description: First-class API versioning support with version attribute in request mappings for backward compatible API evolution
tags: api-versioning, rest-api, backward-compatibility, media-type, deprecation, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# API Versioning

Spring Framework 7 introduces **first-class API versioning support** with the new `version` attribute in request mappings,
making it easy to evolve your APIs while maintaining backward compatibility.

## Overview

API versioning allows you to evolve your REST APIs over time while supporting older clients.
Spring Framework 7 provides built-in support for multiple versioning strategies including media type parameter versioning.

### Key Concepts

- **Media type parameter versioning**: `Accept: application/json;version=1.0`
- **RFC-compliant deprecation**: Deprecation, Sunset, and Link headers
- **Version evolution**: Seamlessly add new response formats
- **Backward compatibility**: V1 clients continue to work while V2 adds features
- **Configurable via `WebMvcConfigurer`**: Use `configureApiVersioning()` method

## Resources

### GitHub Repository
https://github.com/danvega/api-users

### Video Tutorial
https://youtu.be/qjo2tYf01xo

### Blog Post
https://www.danvega.dev/blog/spring-boot-4-api-versioning

### Official Documentation
- [Spring Framework Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html)

## Example Usage

### Controller with Versioning
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping(version = "1.0")
    public UserDTOv1 getUserV1(@PathVariable Long id) {
        return new UserDTOv1(id, user.getName());
    }

    @GetMapping(version = "2.0")
    public UserDTOv2 getUserV2(@PathVariable Long id) {
        return new UserDTOv2(id, user.getFirstName(), user.getLastName());
    }
}
```

### Configuration
```java
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useMediaTypeParameterVersioning();
    }
}
```

### Client Usage
```bash
# Version 1.0 - basic response
curl -H "Accept: application/json;version=1.0" http://localhost:8080/api/users/1

# Version 2.0 - enhanced response
curl -H "Accept: application/json;version=2.0" http://localhost:8080/api/users/1
```

### Version Evolution Example

**V1 Response** (deprecated):
```json
{"id": 1, "name": "Dan Vega"}
```

**V2 Response** (current):
```json
{"id": 1, "firstName": "Dan", "lastName": "Vega"}
```

## Related Features

- [HTTP Interface Clients](../http_interface_clients/) - Consume versioned APIs
- [Resilience](../resilience/) - Add fault tolerance to API endpoints
