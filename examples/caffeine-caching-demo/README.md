# Caffeine Caching Demo

> **Spring Boot 4.0.1 Tech Demo** showcasing high-performance in-memory caching with Caffeine

## Overview

This demonstration project showcases how to implement **in-memory caching** using [Caffeine](https://github.com/ben-manes/caffeine) with Spring Boot 4.0.1. It features a mock book repository with 1000 books and demonstrates cache behavior with real-time statistics.

**Key Features:**
- 1000 mock books with simulated database latency (100ms)
- Caffeine cache with 60-second TTL
- Real-time cache statistics dashboard
- Dark mode Thymeleaf UI (Spring.io inspired)
- No database required - pure in-memory demonstration

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 4.0.1 | Core framework (latest) |
| Java | 25 | Runtime with modern features |
| Caffeine | 3.2.x | High-performance caching library |
| Thymeleaf | 3.4.x | Server-side templating |
| Bootstrap | 5.3.x | CSS framework |
| Maven | 3.9.x | Build tool |

## Quick Start

```bash
# Navigate to project directory
cd examples/caffeine-caching-demo

# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Or run the JAR directly
java -jar target/caffeine-caching-demo-1.0.0.jar
```

**Access the application:** http://localhost:8090

## Project Structure

```
caffeine-caching-demo/
├── pom.xml
├── README.md
├── src/main/java/com/example/cache/
│   ├── CaffeineCachingDemoApplication.java
│   ├── config/
│   │   └── CacheConfig.java          # Caffeine configuration
│   ├── controller/
│   │   └── BookController.java       # Web controller
│   ├── model/
│   │   └── Book.java                 # Book record (Java 25)
│   ├── repository/
│   │   └── MockBookRepository.java   # 1000 mock books
│   └── service/
│       ├── BookService.java          # Cached business logic
│       └── CacheStatsService.java    # Statistics retrieval
└── src/main/resources/
    ├── application.yml
    ├── static/css/style.css          # Dark mode styling
    └── templates/books.html          # Main UI template
```

## How Caching Works

### What is Caffeine?

Caffeine is a high-performance, near-optimal caching library for Java. It's the successor to Guava's cache and provides:

- **High throughput**: Uses advanced concurrency techniques
- **Near-optimal hit rate**: W-TinyLFU eviction policy
- **Low latency**: Optimized for speed
- **Flexible configuration**: Size limits, time-based expiration, statistics

### Spring Boot Integration

Spring Boot 4.0.1 provides first-class support for Caffeine through `spring-boot-starter-cache`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### Cache Configuration

The cache is configured in `CacheConfig.java`:

```java
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String BOOK_SEARCH_CACHE = "bookSearchCache";
    public static final String BOOK_BY_ID_CACHE = "bookByIdCache";

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .maximumSize(500)              // Max 500 entries
            .expireAfterWrite(1, TimeUnit.MINUTES)  // 60-second TTL
            .recordStats();                // Enable statistics
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            BOOK_SEARCH_CACHE,
            BOOK_BY_ID_CACHE
        );
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
```

### Using @Cacheable Annotation

The `@Cacheable` annotation makes caching declarative:

```java
@Service
public class BookService {

    @Cacheable(value = "bookSearchCache", key = "#query")
    public List<Book> searchBooks(String query) {
        // This method is only called on cache miss
        return repository.search(query);  // 100ms simulated latency
    }

    @CacheEvict(value = {"bookSearchCache", "bookByIdCache"}, allEntries = true)
    public void clearCache() {
        // Cache cleared via annotation
    }
}
```

**How it works:**
1. First call with `query="fantasy"` → Cache MISS → Hits repository (100ms)
2. Second call with `query="fantasy"` → Cache HIT → Returns instantly (<1ms)
3. After 60 seconds → Entry expires → Next call is a MISS again

### Cache Statistics

Caffeine provides detailed statistics when `recordStats()` is enabled:

| Metric | Description |
|--------|-------------|
| **Request Count** | Total cache lookups |
| **Hit Count** | Successful cache retrievals |
| **Miss Count** | Cache misses requiring source lookup |
| **Hit Rate** | Percentage of hits (hit / request) |
| **Eviction Count** | Entries removed (TTL or size limit) |
| **Estimated Size** | Current cached entries |

## Configuration Options

### application.yml

```yaml
spring:
  cache:
    type: caffeine  # Use Caffeine as cache provider

server:
  port: 8090

management:
  endpoints:
    web:
      exposure:
        include: health,info,caches,metrics
```

### Caffeine Builder Options

```java
Caffeine.newBuilder()
    // Size-based eviction
    .maximumSize(500)           // Max entries
    .maximumWeight(10_000)      // Max total weight (with weigher)

    // Time-based expiration
    .expireAfterWrite(60, TimeUnit.SECONDS)   // TTL after write
    .expireAfterAccess(30, TimeUnit.SECONDS)  // TTL after last access
    .expireAfter(customExpiry)                // Custom expiration policy

    // Refresh
    .refreshAfterWrite(5, TimeUnit.MINUTES)   // Async refresh

    // Monitoring
    .recordStats()              // Enable statistics

    // Weak/Soft references
    .weakKeys()                 // Allow GC of keys
    .weakValues()               // Allow GC of values
    .softValues()               // Soft reference values
```

## Demonstrating Cache Behavior

### Test Scenario 1: Cache Miss → Cache Hit

1. Open http://localhost:8090
2. Search for "fantasy" → Note: ~100ms (MISS)
3. Search for "fantasy" again → Note: <10ms (HIT)
4. Watch statistics update in real-time

### Test Scenario 2: Cache Expiration

1. Search for "science" → First search (MISS)
2. Wait 60 seconds
3. Search for "science" again → Note: ~100ms (MISS - expired)

### Test Scenario 3: Manual Cache Clear

1. Perform several searches to populate cache
2. Click "Clear Cache" button
3. Perform same searches → All are MISSES

## Cache Patterns Demonstrated

### 1. Cache-Aside Pattern
```
Application → Check Cache → [HIT] → Return cached data
                         → [MISS] → Load from source → Store in cache → Return
```

### 2. Write-Through (not in this demo)
```
Application → Write to cache → Write to source → Return
```

### 3. Cache Eviction Strategies

**In this demo:**
- **Size-based**: Maximum 500 entries
- **Time-based**: 60-second TTL after write
- **Manual**: `@CacheEvict` for programmatic clearing

## Best Practices for Production

### 1. Choose Appropriate TTL
```java
// Short-lived data (sessions, temporary state)
.expireAfterWrite(5, TimeUnit.MINUTES)

// Slowly changing data (user profiles, configurations)
.expireAfterWrite(1, TimeUnit.HOURS)

// Rarely changing data (reference data)
.expireAfterWrite(24, TimeUnit.HOURS)
```

### 2. Set Reasonable Size Limits
```java
// Based on memory budget and entry size
.maximumSize(10_000)  // 10K entries

// Or use weight-based limits
.maximumWeight(100_000_000)  // 100MB total
.weigher((key, value) -> estimateSize(value))
```

### 3. Enable Statistics in Development
```java
// Always enable in dev/staging
.recordStats()

// Consider disabling in production for slight performance gain
// (statistics recording has minimal overhead)
```

### 4. Use Meaningful Cache Names
```java
// Good
"userProfileCache", "productCatalogCache", "searchResultsCache"

// Avoid
"cache1", "myCache", "temp"
```

### 5. Consider Async Loading
```java
Caffeine.newBuilder()
    .refreshAfterWrite(5, TimeUnit.MINUTES)
    .buildAsync(key -> loadFromDatabase(key));
```

## Spring MCP Server Tools Used

This demo was created using the Spring MCP Server documentation tools:

### Tools Consulted

1. **getLatestSpringBootVersion**
   - Retrieved Spring Boot 4.0.1 as the latest version

2. **getSpringBootLanguageRequirements**
   - Confirmed Java 25 compatibility with Spring Boot 4.0.1

3. **searchSpringDocs**
   - Searched for "caffeine caching spring boot" documentation
   - Found caching configuration guides

4. **initializrSearchDependencies**
   - Searched for cache-related dependencies
   - Identified `spring-boot-starter-cache` and `caffeine`

## Actuator Endpoints

The application exposes cache-related actuator endpoints:

```bash
# Health check
curl http://localhost:8090/actuator/health

# Cache information
curl http://localhost:8090/actuator/caches

# Cache metrics
curl http://localhost:8090/actuator/metrics/cache.gets
curl http://localhost:8090/actuator/metrics/cache.puts
```

## Troubleshooting

### Cache Not Working

1. **Check @EnableCaching**: Ensure it's present on a `@Configuration` class
2. **Check cache type**: Verify `spring.cache.type=caffeine` in application.yml
3. **Check annotations**: Ensure `@Cacheable` is on public methods (proxy limitation)

### Statistics Not Recording

1. **Enable recordStats()**: Must be called on Caffeine builder
2. **Cast to CaffeineCache**: Statistics only available through native cache

### Memory Issues

1. **Set size limits**: Always configure `maximumSize` or `maximumWeight`
2. **Monitor evictions**: High eviction count may indicate undersized cache
3. **Use weak/soft references**: For very large cached objects

## Further Reading

- [Caffeine GitHub](https://github.com/ben-manes/caffeine)
- [Spring Boot Caching Documentation](https://docs.spring.io/spring-boot/reference/io/caching.html)
- [Spring Framework Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Caffeine Wiki](https://github.com/ben-manes/caffeine/wiki)

## License

This demo is provided as-is for educational purposes.
