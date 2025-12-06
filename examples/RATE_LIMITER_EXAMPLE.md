# Rate Limiter Example - Summary

The Rate Limiter Example demonstrates the **difference between rate limiting and concurrency limiting** in Spring Boot 4. Learn why `@ConcurrencyLimit` queues requests while proper rate limiting REJECTS them with HTTP 429.

> **Note**: During the first run, the Spring MCP Server was down, which led to using Resilience4j initially. This example documents the iterative discovery process.

## Created: `examples/basic/rate-limiter-demo/`

### Project Structure
```
rate-limiter-demo/
├── build.gradle                    # Spring Boot 4.0.0, Java 25
├── settings.gradle
├── README.md                       # Full documentation
├── assets/
│   └── example-01.png              # Screenshot
├── src/main/java/com/example/ratelimiter/
│   ├── RateLimiterDemoApplication.java
│   ├── service/
│   │   └── RateLimiterService.java     # Token Bucket algorithm
│   ├── controller/
│   │   └── RateLimiterController.java  # API endpoints with HTTP 429
│   └── model/
│       └── ApiResponse.java            # Response record
└── src/main/resources/
    ├── application.yml             # Port 8089
    ├── static/css/style.css        # Dark Spring.io theme
    └── templates/index.html        # Interactive demo UI
```

### Quick Start
```bash
cd examples/basic/rate-limiter-demo
./gradlew bootRun                 # Start app on port 8089
```

**Demo UI**: http://localhost:8089

**API Endpoints**:
```bash
# Send request (may succeed or be rejected)
curl -X POST http://localhost:8089/api/request

# 15 concurrent requests (8 succeed, 7 get HTTP 429)
for i in {1..15}; do curl -s -X POST http://localhost:8089/api/request & done; wait

# Check statistics
curl http://localhost:8089/api/stats
```

---

## How the Spring MCP Server Was Used

This example showcases an **iterative discovery process** - the initial approach was incorrect, and user feedback led to the correct solution.

| Tool/Flavor Used | Purpose | Outcome |
|------------------|---------|---------|
| `searchFlavors("Core Resilience")` | Find resilience patterns | Found "Core Resilience Features" flavor |
| `getFlavorByName("resilience")` | Get implementation guide | Learned about @ConcurrencyLimit |
| `getSpringBootLanguageRequirements("4.0.0")` | Confirm Java version | Java 25 required |
| User Testing + Feedback | Validate behavior | Discovered @ConcurrencyLimit queues, doesn't reject |

### Key Discovery: Rate Limiting vs Concurrency Limiting

The Spring MCP Server's flavor correctly documented `@ConcurrencyLimit`, but testing revealed:

| What We Needed | What @ConcurrencyLimit Does |
|----------------|----------------------------|
| REJECT excess requests | QUEUE excess requests |
| Return HTTP 429 immediately | Wait until slot available |
| Rate limiting | Concurrency limiting |

**Solution**: Custom Token Bucket rate limiter in pure Java.

```java
@Service
public class RateLimiterService {
    private final int limitPerSecond = 8;
    private final AtomicInteger availableTokens;

    public synchronized boolean tryAcquire() {
        refillTokens();
        if (availableTokens.get() > 0) {
            availableTokens.decrementAndGet();
            return true;   // ALLOWED
        }
        return false;      // REJECTED with HTTP 429
    }
}
```

---

## Prompts Used

This demo required **three prompts** (not one-shot):

### Prompt 1: Initial Request
```
create a simple rate limiter app based on spring boot 4.0 using the embedded
resilience patterns of spring boot 4 to show case using rate limiters...
```

### Prompt 2: Correction - Use Native Resilience
```
it is not what I expected because in spring boot 4 we should not need to import
resilience4j to use rate limiter. use spring to read the architecture pattern...
```

### Prompt 3: UI Fix - Behavior Correction
```
the ui does not work as expected. sending 15 requests works but should be blocked.
the start and stop test does not make sense. also when making 15 calls a second
7 should be blocked!
```

---

## Features Implemented

- **Token Bucket Rate Limiter** - Pure Java, no external dependencies
- **HTTP 429 Rejection** - Excess requests immediately rejected
- **Dark Theme UI** - Interactive Thymeleaf UI with test buttons
- **Real-time Statistics** - Track successful vs rejected requests
- **Live Request Log** - Color-coded log showing HTTP status codes
- **Code Preview** - Implementation visible in UI

---

## Rate Limiting Test Results

| Button | Requests Sent | Successful (200) | Rejected (429) |
|--------|---------------|------------------|----------------|
| 5 requests | 5 | 5 | 0 |
| 8 requests | 8 | 8 | 0 |
| 15 requests | 15 | 8 | **7** |

---

## Files Created

| File | Description |
|------|-------------|
| `RateLimiterService.java` | Token Bucket algorithm implementation |
| `RateLimiterController.java` | API endpoints with HTTP 429 rejection |
| `ApiResponse.java` | Response record with requestId, success, message |
| `index.html` | Interactive dark-themed demo UI |
| `style.css` | Spring.io inspired dark theme |

---

## Key Lesson Learned

**`@ConcurrencyLimit`** and **Rate Limiting** are different patterns:

| Pattern | Behavior | Use Case |
|---------|----------|----------|
| Concurrency Limiting | Queue excess requests | Control parallelism |
| Rate Limiting | REJECT excess requests | Protect from overload |

Spring Framework 7's native `@ConcurrencyLimit` is for concurrency control (semaphore-style). For proper rate limiting with HTTP 429 rejection, a custom Token Bucket implementation is needed.

---

*Generated: 2025-12-06*
