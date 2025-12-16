package com.spring.mcp.aspect;

import com.spring.mcp.service.monitoring.McpMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect to intercept all MCP tool calls and record metrics.
 * Intercepts methods annotated with @McpTool from spring-ai-community.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class McpToolMonitoringAspect {

    private final McpMonitoringService monitoringService;

    /**
     * Intercepts all methods annotated with @McpTool and records metrics.
     */
    @Around("@annotation(org.springaicommunity.mcp.annotation.McpTool)")
    public Object monitorToolCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = extractToolName(joinPoint);
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;

        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable e) {
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            try {
                monitoringService.recordToolCall(toolName, durationMs, success, errorMessage);
                log.debug("Recorded tool call: {} ({}ms, success={})", toolName, durationMs, success);
            } catch (Exception e) {
                log.warn("Failed to record tool call metrics for {}: {}", toolName, e.getMessage());
            }
        }
    }

    /**
     * Extract tool name from the method signature.
     * Uses the method name as the tool name.
     */
    private String extractToolName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getName();
    }
}
