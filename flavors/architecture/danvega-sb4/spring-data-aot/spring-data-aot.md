---
unique-name: spring-data-aot
display-name: Spring Data AOT Repositories
category: ARCHITECTURE
pattern-name: Compile-Time Query Generation
description: Ahead-of-Time query processing moving query generation from runtime to compile-time for 50-70% faster startup and GraalVM native image support
tags: spring-data, aot, graalvm, native-image, performance, compile-time, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# Spring Data AOT Repositories

Spring Data AOT moves query processing from runtime to compile-time, delivering **50-70% faster startup**,
reduced memory usage, and catching repository errors before production.

## Overview

Spring Data Ahead-of-Time (AOT) compilation eliminates runtime reflection and query parsing by pre-generating
repository implementations during the build. This provides faster startup, better error detection, and
enables GraalVM native image compilation.

### Key Concepts

- **Compile-time query generation**: SQL generated during build, not at runtime
- **Build-time validation**: Catch typos and invalid property references before deployment
- **Faster startup**: Eliminates runtime reflection and query parsing
- **Lower memory usage**: Pre-compiled implementations are more efficient
- **Serverless optimized**: Particularly impactful for cold start constraints
- **GraalVM ready**: Better native image compatibility since all code paths are known at build time
- **Inspectable & debuggable**: View and debug pre-generated implementations in `target/spring-aot/`

## Resources

### GitHub Repository
https://github.com/danvega/spring-data-aot

### Video Tutorial

### Blog Post
https://www.danvega.dev/blog/spring-data-aot-repositories

### Official Documentation
- [Spring Data AOT](https://docs.spring.io/spring-data/commons/reference/aot.html)

## Example Usage

### Repository with Derived Queries
```java
public interface CoffeeRepository extends CrudRepository<Coffee, Long> {

    // AOT generates SQL at compile-time
    List<Coffee> findByNameContainingIgnoreCase(String name);

    List<Coffee> findByRoastLevelAndOrigin(String roastLevel, String origin);

    @Query("SELECT * FROM coffee WHERE price < :maxPrice ORDER BY price")
    List<Coffee> findAffordableCoffees(BigDecimal maxPrice);
}
```

### Repository with Temporal Queries
```java
public interface OrderRepository extends CrudRepository<Order, Long> {

    List<Order> findByOrderDateAfter(LocalDateTime date);

    @Query("""
        SELECT o.* FROM orders o
        JOIN order_item oi ON o.id = oi.order_id
        JOIN coffee c ON oi.coffee_id = c.id
        WHERE c.name = :coffeeName
        """)
    List<Order> findOrdersContainingCoffee(String coffeeName);
}
```

### AOT Validation Test
```java
@SpringBootTest
class AotRepositoryValidationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void validateAllRepositoryMethodsAreAotProcessed() {
        // Compares method signatures against AOT-generated metadata
        // Fails build if methods are skipped by AOT
        // Provides clear error messages for problematic methods
    }
}
```

### Build with AOT Processing
```bash
# AOT processing happens during package
./mvnw clean package

# View generated code
ls target/spring-aot/main/sources/
```

## Maven Setup

To enable AOT processing, add the `process-aot` goal to your `pom.xml`:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>process-aot</id>
            <goals>
                <goal>process-aot</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Without this configuration, repositories fall back to runtime reflection.

## How AOT Works

During `./mvnw clean package`:
1. AOT processor scans repository interfaces
2. Parses method names to generate SQL
3. Validates @Query annotations
4. Creates pre-compiled repository implementations
5. Generates JSON metadata for processed methods

## Critical Validation Gap

The AOT processor doesn't fail the build when encountering invalid methods like `findByNamme` (typo).
Instead, it logs errors and marks problematic methods for runtime processing. The application only
fails at startup—not ideal for catching issues early.

**Solution**: Implement test-based validation that compares declared repository methods against
AOT-generated metadata entries. This catches issues during the build/test phase rather than in production.
The `AotRepositoryValidationTest` in the example repository demonstrates this approach.

## Known Limitations

Some methods fall back to runtime reflection and cannot be AOT-processed:
- Value expressions requiring runtime evaluation
- Certain collection return types
- `ScrollPosition` parameters

The validation test approach helps identify when methods are unexpectedly falling back to reflection.

## Benefits Summary

| Aspect | Runtime (Traditional) | AOT (Compile-time) |
|--------|----------------------|-------------------|
| Query parsing | Every startup | Once at build |
| Error detection | Runtime exceptions | Build failures |
| Startup time | Slower | 50-70% faster |
| Memory usage | Higher (reflection) | Lower (pre-compiled) |
| Native images | Complex setup | Ready to go |
