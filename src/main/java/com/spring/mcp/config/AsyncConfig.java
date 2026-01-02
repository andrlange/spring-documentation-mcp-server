package com.spring.mcp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Central configuration for async operations using virtual threads.
 * <p>
 * With virtual threads enabled (spring.threads.virtual.enabled=true),
 * this configuration provides lightweight, scalable async execution
 * for all @Async annotated methods.
 * <p>
 * Virtual threads are ideal for I/O-bound operations like:
 * - HTTP calls to external APIs
 * - Database operations
 * - File I/O
 * - Documentation fetching and indexing
 *
 * @author Spring MCP Server
 * @version 1.6.1
 * @since 2026-01-02
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Default async executor using virtual threads.
     * <p>
     * Virtual threads are lightweight (~1KB vs ~1MB for platform threads)
     * and can scale to millions of concurrent tasks. They are automatically
     * managed by the JVM and don't require traditional thread pool sizing.
     *
     * @return executor that creates virtual threads per task
     */
    @Bean(name = "virtualThreadExecutor")
    @Primary
    public Executor virtualThreadExecutor() {
        log.info("Configuring virtual thread executor for async operations");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Default executor for @Async methods when no qualifier is specified.
     *
     * @return the virtual thread executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return virtualThreadExecutor();
    }

    /**
     * Exception handler for uncaught exceptions in async methods.
     * <p>
     * Logs the exception with method details for debugging.
     *
     * @return the exception handler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Uncaught exception in async method '{}': {}",
                    method.getName(), throwable.getMessage(), throwable);
        };
    }

    /**
     * Named executor for general-purpose async tasks.
     * <p>
     * Use with @Async("taskExecutor") for explicit executor selection.
     *
     * @return virtual thread executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("Configuring task executor with virtual threads");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Executor specifically for documentation indexing operations.
     * <p>
     * Use with @Async("indexingExecutor") for documentation processing.
     *
     * @return virtual thread executor for indexing
     */
    @Bean(name = "indexingExecutor")
    public Executor indexingExecutor() {
        log.info("Configuring indexing executor with virtual threads");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Executor for bootstrap operations.
     * <p>
     * Use with @Async("bootstrapExecutor") for bootstrap tasks.
     *
     * @return virtual thread executor for bootstrap
     */
    @Bean(name = "bootstrapExecutor")
    public Executor bootstrapExecutor() {
        log.info("Configuring bootstrap executor with virtual threads");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
