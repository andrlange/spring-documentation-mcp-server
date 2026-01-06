# Caffeine Caching Demo - Implementation Plan

> **Spring Boot 4.0.1 Tech Demo** showcasing in-memory caching with Caffeine

## Project Overview

A bleeding-edge Spring Boot 4.0.1 demonstration project showcasing Caffeine caching capabilities with a modern Thymeleaf-based dark mode UI. This is a pure tech demo without database dependencies.

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 4.0.1 | Core framework (latest) |
| Maven | 9.2.0 | Build tool |
| JDK | 25 | Runtime with Virtual Threads |
| Thymeleaf | 3.4.x | Server-side UI templates |
| Caffeine | 3.2.x | High-performance caching |
| Bootstrap | 5.3.x | CSS framework |
| Bootstrap Icons | 1.11.x | Icon library |

## Project Structure

```
examples/caffeine-caching-demo/
├── pom.xml
├── README.md
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/example/cache/
│   │   │   ├── CaffeineCachingDemoApplication.java
│   │   │   ├── config/
│   │   │   │   └── CacheConfig.java
│   │   │   ├── controller/
│   │   │   │   └── BookController.java
│   │   │   ├── model/
│   │   │   │   └── Book.java
│   │   │   ├── repository/
│   │   │   │   └── MockBookRepository.java
│   │   │   └── service/
│   │   │       ├── BookService.java
│   │   │       └── CacheStatsService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── static/
│   │       │   └── css/
│   │       │       └── style.css
│   │       └── templates/
│   │           └── books.html
│   └── test/
│       └── java/com/example/cache/
│           └── CaffeineCachingDemoApplicationTests.java
└── .gitignore
```

---

## Implementation Phases

### Phase 1: Project Setup (Maven + Spring Boot 4.0.1)

**Files to create:**
- `pom.xml` - Maven build configuration
- `CaffeineCachingDemoApplication.java` - Main application class
- `application.yml` - Application configuration

**pom.xml key dependencies:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.1</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>caffeine-caching-demo</artifactId>
    <version>1.0.0</version>
    <name>Caffeine Caching Demo</name>
    <description>Spring Boot 4.0.1 Caffeine Caching Demo</description>

    <properties>
        <java.version>25</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Caffeine Cache -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>

        <!-- DevTools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**application.yml:**
```yaml
spring:
  application:
    name: caffeine-caching-demo

  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=60s,recordStats

  thymeleaf:
    cache: false  # Disable for development

server:
  port: 8090

management:
  endpoints:
    web:
      exposure:
        include: health,info,caches,metrics
  endpoint:
    health:
      show-details: always
```

---

### Phase 2: Domain Model & Mock Repository

**Files to create:**
- `Book.java` - Book record (Java 25 features)
- `MockBookRepository.java` - In-memory repository with 1000 books

**Book.java (using Java 25 records):**
```java
package com.example.cache.model;

public record Book(
    Long id,
    String title,
    String author,
    String isbn,
    String genre,
    int publicationYear,
    String description
) {}
```

**MockBookRepository.java:**
```java
package com.example.cache.repository;

import com.example.cache.model.Book;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Repository
public class MockBookRepository {

    private final Map<Long, Book> books;

    private static final String[] GENRES = {
        "Fiction", "Science Fiction", "Fantasy", "Mystery",
        "Thriller", "Romance", "Horror", "Non-Fiction",
        "Biography", "History", "Science", "Technology"
    };

    private static final String[] AUTHORS = {
        "J.K. Rowling", "Stephen King", "George R.R. Martin",
        "Isaac Asimov", "Agatha Christie", "Dan Brown",
        "Neil Gaiman", "Terry Pratchett", "Brandon Sanderson",
        "Patrick Rothfuss", "Robert Jordan", "Jim Butcher"
    };

    public MockBookRepository() {
        this.books = LongStream.rangeClosed(1, 1000)
            .boxed()
            .collect(Collectors.toMap(
                id -> id,
                this::generateBook
            ));
    }

    private Book generateBook(Long id) {
        Random random = new Random(id);
        return new Book(
            id,
            "Book Title #" + id,
            AUTHORS[random.nextInt(AUTHORS.length)],
            generateIsbn(id),
            GENRES[random.nextInt(GENRES.length)],
            1950 + random.nextInt(75),
            "Description for book #" + id + "..."
        );
    }

    private String generateIsbn(Long id) {
        return String.format("978-0-%06d-%d", id, id % 10);
    }

    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    public Optional<Book> findById(Long id) {
        // Simulate slow database access
        simulateLatency();
        return Optional.ofNullable(books.get(id));
    }

    public List<Book> search(String query) {
        // Simulate slow database search
        simulateLatency();
        String lowerQuery = query.toLowerCase();
        return books.values().stream()
            .filter(book ->
                book.title().toLowerCase().contains(lowerQuery) ||
                book.author().toLowerCase().contains(lowerQuery) ||
                book.genre().toLowerCase().contains(lowerQuery) ||
                book.isbn().contains(query)
            )
            .collect(Collectors.toList());
    }

    private void simulateLatency() {
        try {
            Thread.sleep(100); // Simulate 100ms DB latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

### Phase 3: Caffeine Cache Configuration

**Files to create:**
- `CacheConfig.java` - Caffeine cache configuration with stats recording

**CacheConfig.java:**
```java
package com.example.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String BOOK_SEARCH_CACHE = "bookSearchCache";
    public static final String BOOK_BY_ID_CACHE = "bookByIdCache";

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats();
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

---

### Phase 4: Service Layer with Caching

**Files to create:**
- `BookService.java` - Business logic with @Cacheable annotations
- `CacheStatsService.java` - Cache statistics retrieval

**BookService.java:**
```java
package com.example.cache.service;

import com.example.cache.config.CacheConfig;
import com.example.cache.model.Book;
import com.example.cache.repository.MockBookRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final MockBookRepository repository;

    public BookService(MockBookRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = CacheConfig.BOOK_SEARCH_CACHE, key = "#query")
    public List<Book> searchBooks(String query) {
        return repository.search(query);
    }

    @Cacheable(value = CacheConfig.BOOK_BY_ID_CACHE, key = "#id")
    public Optional<Book> findById(Long id) {
        return repository.findById(id);
    }

    public List<Book> findAll() {
        return repository.findAll();
    }

    @CacheEvict(value = {CacheConfig.BOOK_SEARCH_CACHE, CacheConfig.BOOK_BY_ID_CACHE}, allEntries = true)
    public void clearCache() {
        // Cache cleared
    }
}
```

**CacheStatsService.java:**
```java
package com.example.cache.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CacheStatsService {

    private final CacheManager cacheManager;

    public CacheStatsService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public record CacheStatistics(
        String cacheName,
        long hitCount,
        long missCount,
        double hitRate,
        long evictionCount,
        long requestCount,
        long estimatedSize
    ) {}

    public Map<String, CacheStatistics> getAllCacheStats() {
        Map<String, CacheStatistics> stats = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats cacheStats = nativeCache.stats();

                stats.put(cacheName, new CacheStatistics(
                    cacheName,
                    cacheStats.hitCount(),
                    cacheStats.missCount(),
                    cacheStats.hitRate(),
                    cacheStats.evictionCount(),
                    cacheStats.requestCount(),
                    nativeCache.estimatedSize()
                ));
            }
        }
        return stats;
    }

    public CacheStatistics getAggregatedStats() {
        var allStats = getAllCacheStats();

        long totalHits = allStats.values().stream().mapToLong(CacheStatistics::hitCount).sum();
        long totalMisses = allStats.values().stream().mapToLong(CacheStatistics::missCount).sum();
        long totalEvictions = allStats.values().stream().mapToLong(CacheStatistics::evictionCount).sum();
        long totalRequests = allStats.values().stream().mapToLong(CacheStatistics::requestCount).sum();
        long totalSize = allStats.values().stream().mapToLong(CacheStatistics::estimatedSize).sum();
        double hitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;

        return new CacheStatistics(
            "AGGREGATED",
            totalHits,
            totalMisses,
            hitRate,
            totalEvictions,
            totalRequests,
            totalSize
        );
    }
}
```

---

### Phase 5: Controller Layer

**Files to create:**
- `BookController.java` - Web controller for book search and cache management

**BookController.java:**
```java
package com.example.cache.controller;

import com.example.cache.model.Book;
import com.example.cache.service.BookService;
import com.example.cache.service.CacheStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BookController {

    private final BookService bookService;
    private final CacheStatsService cacheStatsService;

    public BookController(BookService bookService, CacheStatsService cacheStatsService) {
        this.bookService = bookService;
        this.cacheStatsService = cacheStatsService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(required = false, defaultValue = "") String query,
            Model model
    ) {
        // Cache statistics
        model.addAttribute("cacheStats", cacheStatsService.getAllCacheStats());
        model.addAttribute("aggregatedStats", cacheStatsService.getAggregatedStats());

        // Book search
        List<Book> books;
        long startTime = System.currentTimeMillis();

        if (query.isBlank()) {
            books = bookService.findAll().stream().limit(50).toList();
        } else {
            books = bookService.searchBooks(query);
        }

        long duration = System.currentTimeMillis() - startTime;

        model.addAttribute("books", books);
        model.addAttribute("query", query);
        model.addAttribute("searchDuration", duration);
        model.addAttribute("resultCount", books.size());

        return "books";
    }

    @PostMapping("/cache/clear")
    public String clearCache() {
        bookService.clearCache();
        return "redirect:/?cleared=true";
    }
}
```

---

### Phase 6: Thymeleaf UI with Dark Mode

**Files to create:**
- `templates/books.html` - Main page with search and cache stats
- `static/css/style.css` - Dark mode styling (Spring.io inspired)

**templates/books.html:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Caffeine Caching Demo - Spring Boot 4.0.1</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link th:href="@{/css/style.css}" rel="stylesheet">
</head>
<body class="dark-theme">
    <div class="container-fluid py-4">
        <!-- Header -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="d-flex align-items-center justify-content-between">
                    <div>
                        <h1 class="text-spring-green mb-0">
                            <i class="bi bi-lightning-charge-fill"></i> Caffeine Caching Demo
                        </h1>
                        <p class="text-muted mb-0">Spring Boot 4.0.1 | Caffeine | JDK 25</p>
                    </div>
                    <form th:action="@{/cache/clear}" method="post">
                        <button type="submit" class="btn btn-outline-danger">
                            <i class="bi bi-trash3"></i> Clear Cache
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <!-- Cache Statistics -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card stats-card">
                    <div class="card-header">
                        <i class="bi bi-speedometer2"></i> Cache Statistics
                        <span class="badge bg-spring-green ms-2">Live</span>
                    </div>
                    <div class="card-body">
                        <div class="row g-4">
                            <!-- Aggregated Stats -->
                            <div class="col-md-2">
                                <div class="stat-box">
                                    <div class="stat-value text-spring-green"
                                         th:text="${aggregatedStats.requestCount()}">0</div>
                                    <div class="stat-label">Total Requests</div>
                                </div>
                            </div>
                            <div class="col-md-2">
                                <div class="stat-box">
                                    <div class="stat-value text-success"
                                         th:text="${aggregatedStats.hitCount()}">0</div>
                                    <div class="stat-label">Cache Hits</div>
                                </div>
                            </div>
                            <div class="col-md-2">
                                <div class="stat-box">
                                    <div class="stat-value text-warning"
                                         th:text="${aggregatedStats.missCount()}">0</div>
                                    <div class="stat-label">Cache Misses</div>
                                </div>
                            </div>
                            <div class="col-md-2">
                                <div class="stat-box">
                                    <div class="stat-value"
                                         th:text="${#numbers.formatPercent(aggregatedStats.hitRate(), 1, 1)}">0%</div>
                                    <div class="stat-label">Hit Rate</div>
                                </div>
                            </div>
                            <div class="col-md-2">
                                <div class="stat-box">
                                    <div class="stat-value text-info"
                                         th:text="${aggregatedStats.estimatedSize()}">0</div>
                                    <div class="stat-label">Cached Entries</div>
                                </div>
                            </div>
                            <div class="col-md-2">
                                <div class="stat-box">
                                    <div class="stat-value text-danger"
                                         th:text="${aggregatedStats.evictionCount()}">0</div>
                                    <div class="stat-label">Evictions</div>
                                </div>
                            </div>
                        </div>

                        <!-- Per-Cache Stats -->
                        <div class="row mt-4">
                            <div class="col-12">
                                <h6 class="text-muted mb-3">Per-Cache Breakdown</h6>
                                <div class="table-responsive">
                                    <table class="table table-dark table-hover mb-0">
                                        <thead>
                                            <tr>
                                                <th>Cache Name</th>
                                                <th>Requests</th>
                                                <th>Hits</th>
                                                <th>Misses</th>
                                                <th>Hit Rate</th>
                                                <th>Size</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr th:each="entry : ${cacheStats}">
                                                <td><code th:text="${entry.key}">cacheName</code></td>
                                                <td th:text="${entry.value.requestCount()}">0</td>
                                                <td class="text-success" th:text="${entry.value.hitCount()}">0</td>
                                                <td class="text-warning" th:text="${entry.value.missCount()}">0</td>
                                                <td th:text="${#numbers.formatPercent(entry.value.hitRate(), 1, 1)}">0%</td>
                                                <td th:text="${entry.value.estimatedSize()}">0</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Search Section -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-body">
                        <form th:action="@{/}" method="get" class="row g-3 align-items-end">
                            <div class="col-md-10">
                                <label for="searchQuery" class="form-label">
                                    <i class="bi bi-search text-spring-green"></i> Search Books
                                </label>
                                <input type="text"
                                       class="form-control form-control-lg"
                                       id="searchQuery"
                                       name="query"
                                       th:value="${query}"
                                       placeholder="Search by title, author, genre, or ISBN...">
                            </div>
                            <div class="col-md-2">
                                <button type="submit" class="btn btn-spring-green btn-lg w-100">
                                    <i class="bi bi-search"></i> Search
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <!-- Search Results Info -->
        <div class="row mb-3">
            <div class="col-12">
                <div class="d-flex justify-content-between align-items-center">
                    <span class="text-muted">
                        <i class="bi bi-book"></i>
                        Found <strong th:text="${resultCount}">0</strong> books
                        <span th:if="${not #strings.isEmpty(query)}">
                            for "<span th:text="${query}">query</span>"
                        </span>
                    </span>
                    <span class="badge"
                          th:classappend="${searchDuration < 10} ? 'bg-success' : 'bg-warning'">
                        <i class="bi bi-stopwatch"></i>
                        <span th:text="${searchDuration}">0</span>ms
                        <span th:if="${searchDuration < 10}">(cached)</span>
                    </span>
                </div>
            </div>
        </div>

        <!-- Book Results -->
        <div class="row">
            <div class="col-12">
                <div class="card">
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-dark table-hover mb-0">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Title</th>
                                        <th>Author</th>
                                        <th>Genre</th>
                                        <th>Year</th>
                                        <th>ISBN</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr th:each="book : ${books}">
                                        <td th:text="${book.id()}">#</td>
                                        <td>
                                            <strong th:text="${book.title()}">Title</strong>
                                        </td>
                                        <td th:text="${book.author()}">Author</td>
                                        <td>
                                            <span class="badge bg-secondary" th:text="${book.genre()}">Genre</span>
                                        </td>
                                        <td th:text="${book.publicationYear()}">Year</td>
                                        <td><code th:text="${book.isbn()}">ISBN</code></td>
                                    </tr>
                                    <tr th:if="${#lists.isEmpty(books)}">
                                        <td colspan="6" class="text-center text-muted py-4">
                                            <i class="bi bi-inbox"></i> No books found
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Footer -->
        <div class="row mt-4">
            <div class="col-12 text-center text-muted">
                <small>
                    <i class="bi bi-lightning-charge"></i>
                    Caffeine Cache TTL: 60 seconds |
                    <i class="bi bi-database"></i>
                    1000 Mock Books
                </small>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

**static/css/style.css:**
```css
:root {
    --spring-green: #6db33f;
    --spring-green-dark: #5fa134;
    --spring-bg-dark: #1b1f23;
    --spring-card-bg: #1f2326;
    --spring-nav-bg: #2a2e31;
    --spring-border-dark: #373a3d;
    --spring-hover-bg: #2d3135;
    --text-primary: #ffffff;
    --text-secondary: #a0a0a0;
}

body.dark-theme {
    background-color: var(--spring-bg-dark);
    color: var(--text-primary);
}

.text-spring-green {
    color: var(--spring-green) !important;
}

.bg-spring-green {
    background-color: var(--spring-green) !important;
}

.btn-spring-green {
    background-color: var(--spring-green);
    border-color: var(--spring-green);
    color: #fff;
}

.btn-spring-green:hover {
    background-color: var(--spring-green-dark);
    border-color: var(--spring-green-dark);
    color: #fff;
}

.card {
    background-color: var(--spring-card-bg);
    border: 1px solid var(--spring-border-dark);
    border-radius: 0.5rem;
}

.card-header {
    background-color: var(--spring-nav-bg);
    border-bottom: 1px solid var(--spring-border-dark);
    color: var(--text-primary);
    font-weight: 600;
}

.stats-card .card-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.stat-box {
    text-align: center;
    padding: 1rem;
    background-color: var(--spring-nav-bg);
    border-radius: 0.5rem;
    border: 1px solid var(--spring-border-dark);
}

.stat-value {
    font-size: 2rem;
    font-weight: 700;
    line-height: 1;
    margin-bottom: 0.25rem;
}

.stat-label {
    font-size: 0.75rem;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.form-control {
    background-color: var(--spring-card-bg);
    border: 1px solid var(--spring-border-dark);
    color: var(--text-primary);
}

.form-control:focus {
    background-color: var(--spring-card-bg);
    border-color: var(--spring-green);
    color: var(--text-primary);
    box-shadow: 0 0 0 0.2rem rgba(109, 179, 63, 0.25);
}

.form-control::placeholder {
    color: var(--text-secondary);
}

.table-dark {
    --bs-table-bg: var(--spring-card-bg);
    --bs-table-border-color: var(--spring-border-dark);
    --bs-table-hover-bg: var(--spring-hover-bg);
}

.table-dark thead th {
    background-color: var(--spring-nav-bg);
    border-bottom: 2px solid var(--spring-border-dark);
}

code {
    color: var(--spring-green);
    background-color: var(--spring-bg-dark);
    padding: 0.125rem 0.375rem;
    border-radius: 0.25rem;
}
```

---

### Phase 7: README Documentation

**Files to create:**
- `README.md` - Comprehensive documentation explaining caching concepts

The README should include:
1. Project overview and purpose
2. Technology stack table
3. Quick start guide
4. How caching works with Caffeine
5. Cache configuration explanation
6. Spring Boot 4.0.1 caching features
7. Code examples with annotations
8. Testing caching behavior
9. Best practices for production
10. Spring MCP Server tools used

---

### Phase 8: Testing & Validation

**Files to create:**
- `CaffeineCachingDemoApplicationTests.java` - Basic smoke tests

**Test scenarios:**
1. Application context loads successfully
2. Cache manager is configured correctly
3. Search results are cached
4. Cache statistics are recorded
5. Cache eviction works after 60 seconds

---

## File Checklist

| Phase | File | Status |
|-------|------|--------|
| 1 | `pom.xml` | Pending |
| 1 | `CaffeineCachingDemoApplication.java` | Pending |
| 1 | `application.yml` | Pending |
| 1 | Maven wrapper files | Pending |
| 2 | `Book.java` | Pending |
| 2 | `MockBookRepository.java` | Pending |
| 3 | `CacheConfig.java` | Pending |
| 4 | `BookService.java` | Pending |
| 4 | `CacheStatsService.java` | Pending |
| 5 | `BookController.java` | Pending |
| 6 | `books.html` | Pending |
| 6 | `style.css` | Pending |
| 7 | `README.md` | Pending |
| 8 | `CaffeineCachingDemoApplicationTests.java` | Pending |

---

## Ralph Wiggum Loop Command

After reading and approving this plan, execute the following command to start implementation:

```bash
"/Users/andreas/.claude/plugins/cache/claude-plugins-official/ralph-wiggum/dbc4a7733cd4/scripts/setup-ralph-loop.sh" \
  "Implement the Caffeine Caching Demo as specified in examples/CAFFEIN_CAHING_DEMO.md. Create all files in examples/caffeine-caching-demo/ directory. Follow phases 1-8 exactly. Use Maven 9.2.0, Spring Boot 4.0.1, JDK 25, Thymeleaf 3.4 with dark mode UI. Mock 1000 books, implement Caffeine caching with 60-second TTL, show cache statistics at top of page." \
  --max-iterations 5 \
  --completion-promise "DONE"
```

---

## Success Criteria

1. Application starts without errors on port 8090
2. 1000 mock books are generated on startup
3. Search results are cached for 60 seconds
4. Cache statistics display correctly at page top
5. Dark mode UI renders properly
6. "Clear Cache" button works
7. Search duration shows cache effectiveness (<10ms = cached)
8. README.md is comprehensive and accurate
