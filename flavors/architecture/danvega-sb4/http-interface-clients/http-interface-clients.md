---
unique-name: http-interface-clients
display-name: HTTP Interface Clients
category: ARCHITECTURE
pattern-name: Declarative HTTP Client
description: Zero-boilerplate declarative HTTP clients using @ImportHttpServices and @HttpExchange interfaces for type-safe REST consumption
tags: http-client, rest-client, declarative, httpexchange, type-safe, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# HTTP Interface Clients

Spring Boot 4 introduces the `@ImportHttpServices` annotation, enabling **zero-boilerplate declarative HTTP clients**
using `@HttpExchange` interfaces.

## Overview

HTTP Interface Clients allow you to define REST clients as simple interfaces with declarative annotations.
Spring Boot 4's `@ImportHttpServices` eliminates all the manual configuration previously required.

### Key Concepts

- **Zero configuration**: `@ImportHttpServices` handles all bean registrations automatically
- **Declarative interfaces**: Define HTTP calls as method signatures with `@HttpExchange`
- **Type-safe**: Full compile-time checking with Java records
- **Clean architecture**: Separate interface definitions from implementation

## Resources

### GitHub Repository
https://github.com/danvega/sb4-http-interfaces

### Video Tutorial
https://youtu.be/TEd5e4Thu7M

### Blog Post
`
### Official Documentation
- [Spring Framework HTTP Interface](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-service-client)

## Example Usage

### Before (Manual Configuration)
```java
@Bean
public TodoService todoService(RestClient.Builder restClientBuilder) {
    var restClient = restClientBuilder
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();

    var adapter = RestClientAdapter.create(restClient);
    var factory = HttpServiceProxyFactory.builderFor(adapter).build();
    return factory.createClient(TodoService.class);
}
```

### After (Spring Boot 4)
```java
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(TodoService.class)
public class HttpClientConfig {
    // That's it!
}
```

### Interface Definition
```java
@HttpExchange(url = "https://jsonplaceholder.typicode.com", accept = "application/json")
public interface TodoService {
    @GetExchange("/todos")
    List<Todo> getAllTodos();

    @GetExchange("/todos/{id}")
    Todo getTodoById(@PathVariable Long id);

    @PostExchange("/todos")
    Todo createTodo(@RequestBody Todo todo);
}
```
