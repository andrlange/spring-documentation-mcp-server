---
unique-name: rest-test-client
display-name: REST Test Client
category: ARCHITECTURE
pattern-name: Unified API Testing
description: Unified testing tool providing one consistent API for all REST API tests from isolated unit tests to full end-to-end integration tests
tags: testing, rest-test-client, unit-test, integration-test, mockmvc, spring-boot-4, 4.0
---

> source: Dan Vega - https://github.com/danvega/sb4

# REST Test Client

Spring Framework 7 introduces **RestTestClient**, a unified testing tool providing one consistent API for all types
of REST API tests - from isolated unit tests to full end-to-end integration tests.

## Overview

`RestTestClient` replaces the need to switch between `MockMvc`, `WebTestClient`, and `TestRestTemplate` with a single,
fluent API that works across all testing scenarios. It offers readable syntax and easy migration from legacy tools.

### Key Concepts - 5 Testing Approaches

1. **bindToController**: Isolated controller testing without Spring context
2. **bindToMockMvc**: Controller testing with Spring MVC features (validation, security)
3. **bindToApplicationContext**: Full application testing with real services/database
4. **bindToServer**: Complete end-to-end testing including HTTP layer
5. **bindToRouterFunction**: Functional/reactive endpoint testing

## Resources

### GitHub Repository
https://github.com/danvega/rest-test-client

### Video Tutorial
https://youtu.be/dPM8n0uNhes

### Blog Post
https://www.danvega.dev/blog/spring-framework-7-rest-test-client

### Official Documentation
- [Spring Framework Testing](https://docs.spring.io/spring-framework/reference/testing/resttestclient.html)

## Example Usage

### bindToController - Unit Test
```java
@ExtendWith(MockitoExtension.class)
class TodoControllerTest {

    @Mock
    private TodoService todoService;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToController(new TodoController(todoService)).build();
    }

    @Test
    void shouldGetAllTodos() {
        when(todoService.findAll()).thenReturn(List.of(
            new Todo(1L, "Learn Spring", false)
        ));

        client.get()
            .uri("/api/todos")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].title").isEqualTo("Learn Spring");
    }
}
```

### bindToMockMvc - With Spring MVC Features
```java
@WebMvcTest(TodoController.class)
class TodoControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @Test
    void shouldValidateInput() {
        RestTestClient client = RestTestClient.bindToMockMvc(mockMvc).build();

        client.post()
            .uri("/api/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new Todo(null, "", false))
            .exchange()
            .expectStatus().isBadRequest();
    }
}
```

### bindToServer - End-to-End Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldCreateAndRetrieveTodo() {
        RestTestClient client = RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        client.post()
            .uri("/api/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new Todo(null, "Integration test", false))
            .exchange()
            .expectStatus().isCreated();
    }
}
```

## Test Pyramid Best Practices

| Test Type | Approach | Speed | Realism |
|-----------|----------|-------|---------|
| Unit | bindToController | Fast | Low |
| Integration | bindToMockMvc / bindToApplicationContext | Medium | Medium |
| E2E | bindToServer | Slow | High |

Maximize unit tests for speed, balance with integration tests, minimize E2E tests for stability.
