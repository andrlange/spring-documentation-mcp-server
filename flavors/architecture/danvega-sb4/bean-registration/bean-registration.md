---
unique-name: bean-registration
display-name: Programmatic Bean Registration
category: ARCHITECTURE
pattern-name: Dynamic Bean Registration
description: Clean and simple programmatic bean registration at runtime using the BeanRegistrar interface with modern API support
tags: bean-registration, beanregistrar, runtime, aot, graalvm, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# Programmatic Bean Registration

Spring Framework 7 introduces the `BeanRegistrar` interface, providing a **clean and simple way to register
beans programmatically** at runtime.

## Overview

Programmatic bean registration allows you to dynamically register beans based on configuration, environment conditions,
or other runtime factors. The new `BeanRegistrar` interface replaces the verbose `BeanDefinitionRegistryPostProcessor` approach.

### Key Concepts

- **Runtime flexibility**: Register different beans based on conditions
- **Modern API**: Lambda expressions and method chaining
- **Type-safe**: Full IDE support and compile-time checking
- **AOT compatible**: Works with GraalVM native images
- **Environment access**: Read properties to make registration decisions

## Resources

### GitHub Repository
https://github.com/danvega/sb4-bean-registrar

### Video Tutorial
https://youtu.be/yh760wTFL_4

### Blog Post
https://www.danvega.dev/blog/programmatic-bean-registration

### Official Documentation
- [Programmatic Bean Registration](https://docs.spring.io/spring-framework/reference/core/beans/java/programmatic-bean-registration.html)

## Example Usage

### BeanRegistrar Implementation
```java
public class MessageServiceRegistrar implements BeanRegistrar {
    @Override
    public void register(BeanRegistry registry, Environment env) {
        String messageType = env.getProperty("app.message-type", "email");

        switch (messageType.toLowerCase()) {
            case "email" -> registry.registerBean("messageService",
                EmailMessageService.class,
                spec -> spec.description("Email service via BeanRegistrar"));
            case "sms" -> registry.registerBean("messageService",
                SmsMessageService.class,
                spec -> spec.description("SMS service via BeanRegistrar"));
        }
    }
}
```

### Configuration
```java
@Configuration
@Import(MessageServiceRegistrar.class)
public class ModernConfig {
    // Other bean definitions here
}
```

### Comparison

| Approach | Complexity | Flexibility |
|----------|------------|-------------|
| `@Component` / `@Bean` | Low | Low |
| `BeanDefinitionRegistryPostProcessor` | High | High |
| **BeanRegistrar** (Spring 7) | Low | High |
