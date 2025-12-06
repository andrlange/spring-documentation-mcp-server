---
unique-name: null-safety
display-name: JSpecify Null Safety
category: ARCHITECTURE
pattern-name: Null Safety Enforcement
description: First-class JSpecify null safety annotations support enabling compile-time null checking and better IDE integration
tags: null-safety, jspecify, nullable, nullmarked, kotlin-interop, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# JSpecify Null Safety

Spring Framework 7 introduces first-class support for **JSpecify null safety annotations**, enabling compile-time null
checking and better IDE support for null safety across your codebase.

## Overview

JSpecify provides standardized null safety annotations that work with static analysis tools. Spring Framework 7 fully
embraces these annotations, allowing you to mark entire packages as null-safe with `@NullMarked`.

### Key Concepts

- **Package-level `@NullMarked`**: Declare entire packages as null-safe by default
- **`@Nullable` for exceptions**: Explicitly mark fields/parameters that can be null
- **Compile-time safety**: Catch null pointer issues before runtime
- **IDE integration**: Better autocomplete and warnings in your IDE
- **Kotlin interoperability**: Works seamlessly with Kotlin's null safety

## Resources

### GitHub Repository
https://github.com/danvega/coffeeshop

### Video Tutorial
https://youtu.be/QlGnaRoujL8

### Blog Post
https://www.danvega.dev/blog/spring-boot-4-null-safety

### Official Documentation
- [JSpecify](https://jspecify.dev/)
- [Spring Framework Null Safety](https://docs.spring.io/spring-framework/reference/core/null-safety.html)

## Example Usage

```java
// package-info.java
@NullMarked
package dev.danvega.demo;

import org.jspecify.annotations.NullMarked;
```

```java
// UserService.java
public class UserService {

    // Return type is non-null by default (from @NullMarked)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }

    // Explicitly mark nullable parameters
    public List<User> search(@Nullable String name) {
        if (name == null) {
            return userRepository.findAll();
        }
        return userRepository.findByNameContaining(name);
    }
}
```
