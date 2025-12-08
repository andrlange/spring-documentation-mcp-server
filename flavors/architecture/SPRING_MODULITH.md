---
unique-name: spring-modulith
display-name: Spring Modulith
category: ARCHITECTURE
pattern-name: Spring Modulith Architecture Patterns and alignment to Hexagonal Architecture
description: Spring Modulith enables building large, maintainable Spring Boot systems using clear architectural boundaries without requiring microservices. It prevents monoliths from growing into a tightly coupled structure while keeping deployment and operations simple.
tags: monolith, microservices, modulith, spring, hexagonal, spring-boot-4, 4.0
---

# Spring Modulith — A Complete Guide
_Developer and LLM Reference Version (UTF-8, LF Formatting)_

---

## Table of Contents

- [Introduction](#introduction)
- [Microservices Fatigue](#microservices-fatigue)
- [Modular Monoliths](#modular-monoliths)
- [What is Spring Modulith?](#what-is-spring-modulith)
- [Core Architectural Concepts](#core-architectural-concepts)
- [Module Communication](#module-communication)
- [Installing Modulith](#installing-modulith)
- [Boundary Enforcement](#boundary-enforcement)
- [Documentation and Visualization](#documentation-and-visualization)
- [Runtime Features](#runtime-features)
- [Testing Strategy](#testing-strategy)
- [Migration to Microservices](#migration-to-microservices)
- [Hexagonal Architecture Alignment](#hexagonal-architecture-alignment)
- [Best Practices](#best-practices)
- [Anti-Patterns](#anti-patterns)
- [Code Templates](#code-templates)
- [Feature Summary](#feature-summary)
- [Conclusion](#conclusion)

---

## Introduction

Spring Modulith enables building large, maintainable Spring Boot systems using clear architectural boundaries without requiring microservices. It prevents monoliths from growing into a tightly coupled structure while keeping deployment and operations simple.

---

## Microservices Fatigue

Many organizations adopted microservices prematurely and encountered:

| Problem | Impact |
|--------|--------|
| Operational complexity | High infrastructure overhead |
| Distributed behavior | Latency, network failures |
| Eventual consistency | Higher cognitive load |
| Overlapping boundaries | Hard debugging and testing |

This led to a shift toward simpler deployment models while preserving architectural clarity.

---

## Modular Monoliths

A modular monolith is:

- A single deployable system
- Made of isolated business modules
- With explicit and enforceable dependency rules

It provides the organizational and architectural structure of microservices without requiring distributed deployment.

---

## What is Spring Modulith?

Spring Modulith is described as:

"An opinionated toolkit to build domain-driven, modular applications with Spring Boot."

Its main value is enforcing modular boundaries, documenting them, and providing runtime and testing support.

Capabilities include:

- Module detection and analysis
- Dependency validation
- Controlled module API exposure
- Event-driven module interaction
- Developer documentation and diagrams
- Support for future microservice extraction

---

## Core Architectural Concepts

### Application Modules

A module consists of:

- A public API
- Internal implementation code
- Optional domain events
- Explicit declared outbound dependencies

Example directory layout:

```
com.example.order
 ├── api
 ├── internal
 └── event
```

---

### Declaring Modules

Basic module declaration:

```java
@ApplicationModule
package com.example.order;
```

With metadata:

```java
@ApplicationModule(id="order", displayName="Order Management")
```

---

### Named Interfaces

Modules explicitly define what is visible externally:

```java
@NamedInterface("order-api")
package com.example.order.api;
```

Only code in a named interface may be used by other modules.

---

### Allowed Dependencies

Modules may declare allowed inbound or outbound dependencies:

```java
@ApplicationModule(allowedDependencies = "payment::api")
package com.example.order;
```

---

### Nested and Open Modules

- Nested modules allow deeper structuring of functionality.
- Open modules allow temporary access to internal code for systems being refactored.

---

## Module Communication

Supported patterns:

| Method | Use Case |
|--------|----------|
| Direct calls via public APIs | Strongly consistent workflows |
| Domain events | Decoupled asynchronous communication |

Example:

```java
publisher.publishEvent(new OrderPlaced(orderId));
```

Listener:

```java
@EventListener
void on(OrderPlaced event) { ... }
```

---

## Installing Modulith

Maven:

```xml
<dependency>
  <groupId>org.springframework.modulith</groupId>
  <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
```

Optional observability:

```xml
<dependency>
  <groupId>org.springframework.modulith</groupId>
  <artifactId>spring-modulith-starter-insight</artifactId>
</dependency>
```

---

## Boundary Enforcement

Spring Modulith uses ArchUnit under the hood.

Testing example:

```java
@ApplicationModuleTest
class ArchitectureTest {

  @Test
  void verify(ApplicationModules modules) {
    modules.verify();
  }
}
```

Any violation prevents merging via CI/CD.

---

## Documentation and Visualization

Spring Modulith can automatically generate:

- C4 diagrams
- Module dependency graphs
- HTML documentation

These help developers and tools understand architecture intent.

---

## Runtime Features

Includes:

- Ordered execution of module initialization
- Module-scoped Flyway migrations
- Observability hooks and span generation
- `/actuator/modulith` endpoint for module metadata

---

## Testing Strategy

| Test Type | Purpose |
|-----------|---------|
| Unit tests | Internal module logic |
| Slice tests | One module and its dependencies |
| Architecture tests | Boundary rule validation |
| End-to-end tests | Full system workflow |

---

## Migration to Microservices

Modules can evolve into services incrementally.

Steps:

```
Module → API Boundary → Independent Deployment → External Messaging
```

This avoids rewriting business logic.

---

## Hexagonal Architecture Alignment

Mapping:

| Hexagonal Construct | Modulith Component |
|--------------------|-------------------|
| Domain | Internal module code |
| Ports | Named Interfaces |
| Adapters | Infrastructure subpackages |
| Application Layer | Module public API |
| Eventing | Cross-module communication |

Spring Modulith adds:

- Enforcement
- Tooling
- Testing
- Runtime metadata

---

## Best Practices

- Model modules after business domains or bounded contexts
- Keep public APIs intentional and small
- Avoid unnecessary module dependencies
- Use domain events across business boundaries
- Enforce architecture rules in CI/CD

---

## Anti-Patterns

Do not:

- Create a shared DTO or util module
- Access another module’s internals
- Share persistence mappings across modules
- Use HTTP calls within the same JVM
- Treat module boundaries as optional

---

## Code Templates

Example service interface:

```java
@NamedInterface
public interface OrderService {
    OrderId place(CreateOrderCommand command);
}
```

Event listener:

```java
@EventListener
void on(OrderPlaced event) { ... }
```

Architecture enforcement:

```java
modules.verify();
```

---

## Feature Summary

Highlights:

- Structured modularity
- Asynchronous module communication
- Developer documentation
- Enforcement and testing suite
- Runtime module awareness
- Microservice extraction pathway

---

## Conclusion

Spring Modulith enables maintainable large-scale Spring Boot systems by enforcing modular boundaries, supporting event-driven communication, and providing future-proof architectural structure.

It merges the simplicity of a monolith with the clarity and maintainability often associated with microservices — without requiring distributed deployment.

