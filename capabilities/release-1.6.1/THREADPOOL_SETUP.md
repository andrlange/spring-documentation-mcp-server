⏺ Analysis Report: Threading Patterns in Spring MCP Server

Current State

I found three different threading patterns in the codebase:

| Location                      | Pattern                                  | Issues                                              |
  |-------------------------------|------------------------------------------|-----------------------------------------------------|
| BootstrapController.java:112  | new Thread(() -> {...}).start()          | Unmanaged, no pool, no monitoring                   |
| DocumentationIndexer.java:475 | Executors.newFixedThreadPool(...)        | Creates new pool per batch call, not Spring-managed |
| EmbeddingConfig.java:67-84    | ThreadPoolTaskExecutor with @EnableAsync | Proper Spring-managed executor                      |

Why Vanilla new Thread() is Problematic

1. No thread management: No visibility into thread count, no queuing, no rejection handling
2. Resource leaks: Each call creates a new thread - expensive operation
3. No monitoring: Spring Actuator won't track these threads
4. No graceful shutdown: On application shutdown, this thread is orphaned
5. Exception handling: Uncaught exceptions silently disappear (only logged)
6. Breaks Spring context: @Transactional, security context, and other thread-local Spring features don't propagate

Why Executors.newFixedThreadPool() in DocumentationIndexer is Also Problematic

- Creates a new ExecutorService per method call (line 475)
- Not Spring-managed, so graceful shutdown is manual and error-prone
- Should be a singleton bean

Virtual Threads Consideration (Java 21+)

Your project uses Java 25 and Spring Boot 3.5.7, both fully support virtual threads. Enabling virtual threads is the modern approach:

# application.yml
spring:
threads:
virtual:
enabled: true

Benefits of virtual threads:
- Lightweight (~1KB vs ~1MB for platform threads)
- Millions of concurrent threads possible
- Perfect for I/O-bound operations (like HTTP calls in bootstrap)
- Spring Boot 3.2+ auto-configures Tomcat/Jetty/Undertow to use virtual threads
- @Async methods automatically use virtual threads

Impact on existing code:
- ThreadPoolTaskExecutor would still work but becomes less necessary
- The vanilla new Thread() in BootstrapController would still be problematic (not using virtual threads unless explicitly created)
- Executors.newVirtualThreadPerTaskExecutor() replaces fixed thread pools

Recommendations

1. Enable virtual threads in application.yml (simple one-liner)
2. Refactor BootstrapController to use @Async:
   @Async
   public void bootstrapAllProjectsAsync() { ... }
3. Refactor DocumentationIndexer to use a Spring-managed executor bean instead of creating pools per call
4. With virtual threads enabled, the embeddingTaskExecutor can optionally be simplified or removed