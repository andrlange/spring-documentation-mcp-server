---
unique-name: otel
display-name: OpenTelemetry Integration
category: ARCHITECTURE
pattern-name: Distributed Tracing
description: Production-ready observability with automatic instrumentation of HTTP requests, database calls, and log correlation via OTLP
tags: opentelemetry, observability, tracing, metrics, logging, otlp, grafana, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# OpenTelemetry Integration

Spring Boot 4 introduces the official **spring-boot-starter-opentelemetry** for production-ready observability
with automatic instrumentation of HTTP requests, database calls, and log correlation.

## Overview

The new OpenTelemetry starter simplifies observability by providing automatic instrumentation out of the box.
Spring Boot internally uses Micrometer but exports all telemetry via OTLP (OpenTelemetry Protocol) to any
compatible backend like Grafana, Jaeger, or Zipkin.

### Key Concepts

- **Single dependency**: `spring-boot-starter-opentelemetry` replaces complex setup
- **Automatic instrumentation**: HTTP server/client, JDBC, and more
- **Log correlation**: Automatic trace/span ID injection into logs
- **OTLP export**: Works with any OpenTelemetry-compatible backend
- **Production-ready**: Official Spring support, no alpha dependencies

## Resources

### GitHub Repository
https://github.com/danvega/ot

### Video Tutorial

### Blog Post

### Official Documentation
- [OpenTelemetry with Spring Boot](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot)
- [OpenTelemetry](https://opentelemetry.io/)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)

## Example Usage

### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

### Configuration
```yaml
# application.yml
spring:
  application:
    name: my-service

management:
  tracing:
    sampling:
      probability: 1.0  # 100% for dev, lower for production
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

### Automatic Log Correlation
Logs automatically include trace context:
```
2025-11-18 10:30:45 INFO [traceId=abc123, spanId=def456] Processing request...
```

### Custom Spans with @Observed
```java
@Service
public class OrderService {

    @Observed(name = "order.process", contextualName = "processOrder")
    public Order processOrder(OrderRequest request) {
        // Automatically creates a span for this method
        return orderRepository.save(createOrder(request));
    }
}
```

### Controller Example
```java
@RestController
public class GreetingController {

    @GetMapping("/greet/{name}")
    public String greet(@PathVariable String name) throws InterruptedException {
        // Automatically traced
        Thread.sleep(50); // Simulated work
        return "Hello, " + name + "!";
    }

    @GetMapping("/slow")
    public String slow() throws InterruptedException {
        Thread.sleep(500);
        return "Finally done!";
    }
}
```

## Automatic Instrumentation

The starter automatically instruments:

| Component | What's Traced |
|-----------|--------------|
| HTTP Server | All incoming requests |
| RestTemplate | Outgoing HTTP calls |
| WebClient | Reactive HTTP calls |
| JDBC | Database queries |
| Logs | Trace/span ID injection |

## Running with Grafana LGTM Stack

```bash
# Start observability stack
docker-compose up -d

# Run your application
./mvnw spring-boot:run

# View traces in Grafana
open http://localhost:3000
```
