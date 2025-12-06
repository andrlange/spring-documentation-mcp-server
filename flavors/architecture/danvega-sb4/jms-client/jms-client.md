---
unique-name: jms-client
display-name: JMS Client API
category: ARCHITECTURE
pattern-name: Message-Driven Architecture
description: Modern fluent JmsClient API for type-safe JMS messaging with Apache ActiveMQ Artemis including 7 essential messaging patterns
tags: jms, messaging, activemq, artemis, async, request-reply, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# JMS Client API

Spring Framework 6.1+ introduces the modern **JmsClient API**, providing a fluent, type-safe approach to JMS messaging
with Apache ActiveMQ Artemis.

## Overview

The new `JmsClient` API offers a modern alternative to `JmsTemplate` with a fluent, self-documenting approach,
automatic type conversion, and flexible QoS configuration. This demo showcases 7 essential messaging patterns
through an order management system.

### Key Concepts - 7 Messaging Patterns

1. **Basic Send**: Fire-and-forget message delivery
2. **Quality of Service (QoS)**: TTL, priority, and delivery delays
3. **Custom Headers**: Message metadata for routing and filtering
4. **Synchronous Receive**: Pull messages with configurable timeout
5. **Receive and Convert**: Type-safe message retrieval
6. **Request-Reply**: RPC-style synchronous communication
7. **Reusable Operation Handle**: Performance optimization through preconfiguration

## Resources

### GitHub Repository
https://github.com/danvega/jms-orders

### Video Tutorial
[Add YouTube URL]

### Blog Post
[Add Blog URL]

### Official Documentation
- [Spring Boot JMS](https://docs.spring.io/spring-boot/reference/messaging/jms.html)
- [Apache ActiveMQ Artemis](https://activemq.apache.org/components/artemis/)

## Example Usage

### JmsClient - Basic Send
```java
@Service
public class OrderMessagingService {

    private final JmsClient jmsClient;

    public OrderMessagingService(JmsClient jmsClient) {
        this.jmsClient = jmsClient;
    }

    public void sendOrder(Order order) {
        jmsClient.send("orders.queue")
            .withBody(order);
    }
}
```

### JmsClient - Quality of Service
```java
public void sendPriorityOrder(Order order) {
    jmsClient.send("orders.queue")
        .withPriority(9)
        .withTimeToLive(Duration.ofMinutes(5))
        .withBody(order);
}
```

### JmsClient - Request-Reply
```java
public OrderConfirmation processOrder(Order order) {
    return jmsClient.requestAndReceive("orders.queue")
        .withBody(order)
        .convertTo(OrderConfirmation.class);
}
```

### Consumer with @JmsListener
```java
@Component
public class OrderConsumer {

    @JmsListener(destination = "orders.queue")
    public void processOrder(Order order) {
        // Process the order
    }
}
```

### Running with Docker
```bash
docker-compose up -d              # Start ActiveMQ Artemis
./mvnw spring-boot:run            # Run application
```

Access ActiveMQ Console: http://localhost:8161 (admin/admin)
