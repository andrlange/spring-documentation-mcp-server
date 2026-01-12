# Documentation MCP Tools Test Results

**Generated:** 2026-01-12 13:48:08

**Base URL:** http://localhost:7777

**Tools Tested:** 12

---

## Use Case 1: App Modernization (Spring Boot 2.x/3.x to 4.x)

This use case covers migrating existing applications to Spring Boot 4.x.

### listSpringProjects

#### Test 1: List all Spring projects to understand the ecosystem

**Parameters:** `{}`

**Result:**

```json
{
  "count": 56,
  "executionTimeMs": 8,
  "projects": [
    {
      "name": "Spring Framework",
      "slug": "spring-framework",
      "description": "The Spring Framework provides a comprehensive programming and configuration model",
      "homepage": "https://spring.io/projects/spring-framework",
      "github": "https://github.com/spring-projects/spring-framework"
    },
    {
      "name": "Spring Data",
      "slug": "spring-data",
      "description": "Spring Data provides a familiar and consistent Spring-based programming model for data access",
      "homepage": "https://spring.io/projects/spring-data",
      "github": "https://github.com/spring-projects/spring-data"
    },
    {
      "name": "Spring Security",
      "slug": "spring-security",
      "description": "Spring Security is a framework that provides authentication, authorization and protection",
      "homepage": "https://spring.io/projects/spring-security",
      "github": "https://github.com/spring-projects/spring-security"
    },
    {
      "name": "Spring Batch",
      "slug": "spring-batch",
      "description": "Spring Batch provides reusable functions that are essential in processing large volumes of records, including logging/tracing, transaction management, job processing statistics, job restart, skip, and resource management.",
      "homepage": "https://spring.io/projects/spring-batch",
      "github": "https://github.com/spring-projects/spring-batch"
    },
    {
      "name": "Spring AMQP",
      "slug": "spring-amqp",
      "description": "Spring AMQP",
      "homepage": "https://spring.io/projects/spring-amqp",
      "github": "https://github.com/spring-projects/spring-amqp"
    },
    {
      "name": "Spring Cloud Bus",
      "slug": "spring-cloud-bus",
      "description": "Spring Cloud Bus",
      "homepage": "https://spring.io/projects/spring-cloud-bus",
      "github": "https://github.com/spring-projects/spring-cloud-bus"
    },
    {
      "name": "Spring Cloud Circuit Brea...
```

---

### getSpringVersions

#### Test 1: Get Spring Boot versions to plan migration

**Parameters:** `{"project": "spring-boot"}`

**Result:**

```json
{
  "project": "Spring Boot",
  "slug": "spring-boot",
  "description": "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications",
  "versions": [
    {
      "version": "4.0.2.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2026-01-02"
    },
    {
      "version": "4.0.2-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "4.0.1-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "4.0.1.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "4.0.1",
      "type": "GA",
      "isLatest": false,
      "isDefault": true,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "4.0.1.RELEASE",
      "type": "GA",
      "isLatest": true,
      "isDefault": false,
      "releaseDate": "2026-01-03"
    },
    {
      "version": "4.0.0.RELEASE",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "4.0.0",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "3.5.10-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "3.5.10.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2026-01-02"
    },
    {
      "version": "3.5.9-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "3.5.9.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": fal...
```

#### Test 2: Get Spring Framework versions for compatibility

**Parameters:** `{"project": "spring-framework"}`

**Result:**

```json
{
  "project": "Spring Framework",
  "slug": "spring-framework",
  "description": "The Spring Framework provides a comprehensive programming and configuration model",
  "versions": [
    {
      "version": "7.0.3-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "7.0.2-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "7.0.2",
      "type": "GA",
      "isLatest": true,
      "isDefault": true,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "7.0.1",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "7.0.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "6.2.16-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2024-11-01"
    },
    {
      "version": "6.2.15",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2024-11-01"
    },
    {
      "version": "6.2.15-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2024-11-01"
    },
    {
      "version": "6.2.14",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2024-11-01"
    },
    {
      "version": "6.2.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "6.1.21",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2023-11-01"
    },
    {
      "version": "6.1.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "6....
```

---

### listSpringBootVersions

#### Test 1: List all GA versions to understand upgrade path

**Parameters:** `{"state": "GA", "limit": 10}`

**Result:**

```json
{
  "filters": {
    "state": "GA",
    "limit": 10
  },
  "enterpriseSubscriptionEnabled": true,
  "totalFound": 9,
  "returnedResults": 9,
  "executionTimeMs": 7,
  "versions": [
    {
      "id": 1,
      "version": "3.4.12",
      "majorVersion": 3,
      "minorVersion": 4,
      "patchVersion": 12,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2024-11-01",
      "ossSupportEnd": "2025-12-01",
      "enterpriseSupportEnd": "2026-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/3.4/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/3.4/api/java/index.html",
      "ossSupportActive": false,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 3,
      "version": "3.5.8",
      "majorVersion": 3,
      "minorVersion": 5,
      "patchVersion": 8,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2025-05-01",
      "ossSupportEnd": "2026-06-01",
      "enterpriseSupportEnd": "2032-06-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/3.5/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/3.5/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 7,
      "version": "2.7.x",
      "majorVersion": 2,
      "minorVersion": 7,
      "patchVersion": 0,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2022-05-01",
      "ossSupportEnd": "2023-06-01",
      "enterpriseSupportEnd": "2029-06-01",
      "referenceDocUrl": "",
      "apiDocUrl": "",
      "ossSupportActive": false,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 8,
      "version": "3.3.x",
      "majorVersion": 3,
      "minorVersion": 3,
      "patchVersion": 0,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2...
```

---

### filterSpringBootVersionsBySupport

#### Test 1: Find supported versions for migration target

**Parameters:** `{"supportActive": true, "limit": 10}`

**Result:**

```json
{
  "filters": {
    "supportActive": "true",
    "limit": 10
  },
  "enterpriseSubscriptionEnabled": true,
  "supportDateUsed": "enterpriseSupportEnd",
  "totalFound": 10,
  "executionTimeMs": 7,
  "versions": [
    {
      "id": 13,
      "version": "4.0.2-SNAPSHOT",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 2,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 12,
      "version": "4.0.1",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 1,
      "state": "GA",
      "isCurrent": true,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 5,
      "version": "4.0.0",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 0,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.0/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.0/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 11,
...
```

---

### searchSpringDocs

#### Test 1: Search for migration documentation

**Parameters:** `{"query": "migration Spring Boot 4", "limit": 5}`

**Result:**

```json
{
  "query": "migration Spring Boot 4",
  "filters": {
    "project": "all",
    "version": "all",
    "docType": "all"
  },
  "totalResults": 5,
  "returnedResults": 5,
  "executionTimeMs": 125,
  "results": [
    {
      "id": 3224,
      "title": "Servlet Migrations",
      "url": "https://github.com/spring-projects/spring-security/blob/7.0.x/docs/modules/ROOT/pages/migration/servlet/index.adoc",
      "description": "If you have already performed the xref:migration/index.adoc[initial migration steps] for your Servlet application, you're now ready to perform steps specific to Servlet applications.",
      "project": "Spring Security",
      "projectSlug": "spring-security",
      "version": "7.0.x",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 1531,
      "title": "Servlet Migrations",
      "url": "https://github.com/spring-projects/spring-security/blob/7.0.0/docs/modules/ROOT/pages/migration/servlet/index.adoc",
      "description": "If you have already performed the xref:migration/index.adoc[initial migration steps] for your Servlet application, you're now ready to perform steps specific to Servlet applications.",
      "project": "Spring Security",
      "projectSlug": "spring-security",
      "version": "7.0.0",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 1341,
      "title": "Servlet Migrations",
      "url": "https://github.com/spring-projects/spring-security/blob/7.0.2/docs/modules/ROOT/pages/migration/servlet/index.adoc",
      "description": "If you have already performed the xref:migration/index.adoc[initial migration steps] for your Servlet application, you're now ready to perform steps specific to Servlet applications.",
      "project": "Spring Security",
      "projectSlug": "spring-security",
      "version": "7.0.2",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 3051,
      "title": "Aut...
```

#### Test 2: Search for breaking changes

**Parameters:** `{"query": "breaking changes Spring Boot 4", "limit": 5}`

**Result:**

```json
{
  "query": "breaking changes Spring Boot 4",
  "filters": {
    "project": "all",
    "version": "all",
    "docType": "all"
  },
  "totalResults": 5,
  "returnedResults": 5,
  "executionTimeMs": 85,
  "results": [
    {
      "id": 1675,
      "title": "Change History",
      "url": "https://github.com/spring-projects/spring-amqp/blob/v4.0.0/src/reference/antora/modules/ROOT/pages/appendix/change-history.adoc",
      "description": "This section describes changes that have been made as versions have changed.",
      "project": "Spring AMQP",
      "projectSlug": "spring-amqp",
      "version": "4.0.0",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 1589,
      "title": "Change History",
      "url": "https://github.com/spring-projects/spring-amqp/blob/v4.0.1/src/reference/antora/modules/ROOT/pages/appendix/change-history.adoc",
      "description": "This section describes changes that have been made as versions have changed.",
      "project": "Spring AMQP",
      "projectSlug": "spring-amqp",
      "version": "4.0.1",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 2579,
      "title": "Changes between 4.3 and 5.0",
      "url": "https://github.com/spring-projects/spring-integration/blob/v7.0.1/src/reference/antora/modules/ROOT/pages/changes-4.3-5.0.adoc",
      "description": "See the https://github.com/spring-projects/spring-integration/wiki/Spring-Integration-4.3-to-5.0-Migration-Guide[Migration Guide] for important changes that might affect your applications. You can find migration guides for all versions back to 2.1 on the https://github.com/spring-projects/spring-integration/wiki[wiki].",
      "project": "Spring Integration",
      "projectSlug": "spring-integration",
      "version": "7.0.1",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 2216,
      "title": "Changes between 4.3 and 5.0",
      "url":...
```

#### Test 3: Search for deprecated features

**Parameters:** `{"query": "deprecated features Spring Boot 3", "limit": 5}`

**Result:**

```json
{
  "query": "deprecated features Spring Boot 3",
  "filters": {
    "project": "all",
    "version": "all",
    "docType": "all"
  },
  "totalResults": 5,
  "returnedResults": 5,
  "executionTimeMs": 88,
  "results": [
    {
      "id": 170,
      "title": "Deprecated Application Properties",
      "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.0/documentation/spring-boot-docs/src/docs/antora/modules/appendix/pages/deprecated-application-properties/index.adoc",
      "description": "The following deprecated properties can be specified inside your `application.properties` file, inside your `application.yaml` file, or as command line switches. Support for these properties will be removed in a future release and should you should migrate away from them.",
      "project": "Spring Boot",
      "projectSlug": "spring-boot",
      "version": "4.0.0",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 2931,
      "title": "Deprecated Application Properties",
      "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.1/documentation/spring-boot-docs/src/docs/antora/modules/appendix/pages/deprecated-application-properties/index.adoc",
      "description": "The following deprecated properties can be specified inside your `application.properties` file, inside your `application.yaml` file, or as command line switches. Support for these properties will be removed in a future release and should you should migrate away from them.",
      "project": "Spring Boot",
      "projectSlug": "spring-boot",
      "version": "4.0.1",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 118,
      "title": "Metadata Format",
      "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.0/documentation/spring-boot-docs/src/docs/antora/modules/specification/pages/configuration-metadata/format.adoc",
      "description": "Configuration metadata files are located ...
```

---

### getCodeExamples

#### Test 1: Find migration code examples

**Parameters:** `{"query": "migration", "limit": 10}`

**Result:**

```json
{
  "filters": {
    "query": "migration",
    "project": "all",
    "version": "all",
    "language": "all",
    "limit": 10
  },
  "totalFound": 0,
  "returnedResults": 0,
  "executionTimeMs": 70,
  "examples": []
}
```

#### Test 2: Find new configuration examples

**Parameters:** `{"query": "Spring Boot 4 configuration", "limit": 10}`

**Result:**

```json
{
  "filters": {
    "query": "Spring Boot 4 configuration",
    "project": "all",
    "version": "all",
    "language": "all",
    "limit": 10
  },
  "totalFound": 20,
  "returnedResults": 10,
  "executionTimeMs": 96,
  "examples": [
    {
      "id": 1975,
      "title": "Messaging with RabbitMQ - Example 4",
      "description": "What You Will Build",
      "codeSnippet": "Sending message...\n    Received <Hello from RabbitMQ!>",
      "language": "java",
      "category": "Getting Started",
      "tags": [
        "Getting Started",
        "spring-guide",
        "gs"
      ],
      "sourceUrl": "https://spring.io/guides/gs/messaging-rabbitmq",
      "project": "Spring AMQP",
      "projectSlug": "spring-amqp",
      "version": "4.0.1"
    },
    {
      "id": 284,
      "title": "Messaging with RabbitMQ - Example 4",
      "description": "What You Will Build",
      "codeSnippet": "Sending message...\n    Received <Hello from RabbitMQ!>",
      "language": "java",
      "category": "Getting Started",
      "tags": [
        "Getting Started",
        "spring-guide",
        "gs"
      ],
      "sourceUrl": "https://spring.io/guides/gs/messaging-rabbitmq",
      "project": "Spring AMQP",
      "projectSlug": "spring-amqp",
      "version": "4.0.x"
    },
    {
      "id": 381,
      "title": "Accessing Data with MySQL - Example 4",
      "description": "What You Will Build",
      "codeSnippet": "package com.example.accessingdatamysql;\n\nimport org.springframework.boot.SpringApplication;\nimport org.springframework.boot.autoconfigure.SpringBootApplication;\n\n@SpringBootApplication\npublic class AccessingDataMysqlApplication {\n\n  public static void main(String[] args) {\n    SpringApplication.run(AccessingDataMysqlApplication.class, args);\n  }\n\n}",
      "language": "java",
      "category": "Getting Started",
      "tags": [
        "Getting Started",
        "spring-guide",
        "gs"
      ],
      "sourceUrl": "https://spring.io/guides/gs/accessing-...
```

---

### findProjectsByUseCase

#### Test 1: Find projects related to migration

**Parameters:** `{"useCase": "migration"}`

**Result:**

```json
{
  "useCase": "migration",
  "totalFound": 0,
  "executionTimeMs": 43,
  "projects": []
}
```

---

### getDocumentationByVersion

#### Test 1: Get docs for target version 4.0.1

**Parameters:** `{"project": "spring-boot", "version": "4.0.1"}`

**Result:**

```json
{
  "project": "Spring Boot",
  "projectSlug": "spring-boot",
  "version": "4.0.1",
  "versionType": "GA",
  "isLatest": false,
  "totalDocuments": 158,
  "executionTimeMs": 285,
  "documentationByType": {
    "GitHub Reference": [
      {
        "id": 2816,
        "title": "Calling REST Services",
        "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.1/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/io/rest-client.adoc",
        "description": "Spring Boot provides various convenient ways to call remote REST services. If you are developing a non-blocking reactive application and you're using Spring WebFlux, then you can use javadoc:org.springframework.web.reactive.function.client.WebClient[]. If you prefer imperative APIs then you can use javadoc:org.springframework.web.client.RestClient[] or javadoc:org.springframework.web.client.RestTemplate[].",
        "project": "Spring Boot",
        "projectSlug": "spring-boot",
        "version": "4.0.1",
        "docType": "GitHub Reference",
        "contentType": "text/markdown"
      },
      {
        "id": 2817,
        "title": "Advanced Native Images Topics",
        "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.1/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/packaging/native-image/advanced-topics.adoc",
        "description": "Reflection hints are automatically created for configuration properties by the Spring ahead-of-time engine. Nested configuration properties which are not inner classes, however, *must* be annotated with javadoc:org.springframework.boot.context.properties.NestedConfigurationProperty[format=annotation], otherwise they won't be detected and will not be bindable.",
        "project": "Spring Boot",
        "projectSlug": "spring-boot",
        "version": "4.0.1",
        "docType": "GitHub Reference",
        "contentType": "text/markdown"
      },
      {
        "id": 2818,
        "title": "Recording HTTP E...
```

---

### getLatestSpringBootVersion

#### Test 1: Get latest GA versions (default, no params)

**Parameters:** `{}`

**Result:**

```json
{
  "totalVersionsForMajorMinor": 1,
  "executionTimeMs": 6,
  "latestVersion": {
    "id": 12,
    "version": "4.0.1",
    "majorVersion": 4,
    "minorVersion": 0,
    "patchVersion": 1,
    "state": "GA",
    "isCurrent": true,
    "releasedAt": "2025-11-01",
    "ossSupportEnd": "2026-12-01",
    "enterpriseSupportEnd": "2027-12-01",
    "referenceDocUrl": "https://docs.spring.io/spring-boot/index.html",
    "apiDocUrl": "https://docs.spring.io/spring-boot/api/java/index.html",
    "ossSupportActive": true,
    "enterpriseSupportActive": true,
    "isEndOfLife": false,
    "supportActive": true
  },
  "allVersions": [
    {
      "id": 12,
      "version": "4.0.1",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 1,
      "state": "GA",
      "isCurrent": true,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    }
  ]
}
```

#### Test 2: Get latest 4.0.x patch version

**Parameters:** `{"majorVersion": 4, "minorVersion": 0}`

**Result:**

```json
{
  "majorVersion": 4,
  "minorVersion": 0,
  "totalVersionsForMajorMinor": 4,
  "executionTimeMs": 6,
  "latestVersion": {
    "id": 13,
    "version": "4.0.2-SNAPSHOT",
    "majorVersion": 4,
    "minorVersion": 0,
    "patchVersion": 2,
    "state": "SNAPSHOT",
    "isCurrent": false,
    "releasedAt": "2025-11-01",
    "ossSupportEnd": "2026-12-01",
    "enterpriseSupportEnd": "2027-12-01",
    "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/index.html",
    "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/api/java/index.html",
    "ossSupportActive": true,
    "enterpriseSupportActive": true,
    "isEndOfLife": false,
    "supportActive": true
  },
  "allVersions": [
    {
      "id": 13,
      "version": "4.0.2-SNAPSHOT",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 2,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 6,
      "version": "4.0.1-SNAPSHOT",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 1,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.1-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.1-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 12,
      "version": "4.0.1",
     ...
```

#### Test 3: Get latest 3.5.x patch version for source

**Parameters:** `{"majorVersion": 3, "minorVersion": 5}`

**Result:**

```json
{
  "majorVersion": 3,
  "minorVersion": 5,
  "totalVersionsForMajorMinor": 4,
  "executionTimeMs": 5,
  "latestVersion": {
    "id": 11,
    "version": "3.5.10-SNAPSHOT",
    "majorVersion": 3,
    "minorVersion": 5,
    "patchVersion": 10,
    "state": "SNAPSHOT",
    "isCurrent": false,
    "releasedAt": "2025-05-01",
    "ossSupportEnd": "2026-06-01",
    "enterpriseSupportEnd": "2032-06-01",
    "referenceDocUrl": "https://docs.spring.io/spring-boot/3.5-SNAPSHOT/index.html",
    "apiDocUrl": "https://docs.spring.io/spring-boot/3.5-SNAPSHOT/api/java/index.html",
    "ossSupportActive": true,
    "enterpriseSupportActive": true,
    "isEndOfLife": false,
    "supportActive": true
  },
  "allVersions": [
    {
      "id": 11,
      "version": "3.5.10-SNAPSHOT",
      "majorVersion": 3,
      "minorVersion": 5,
      "patchVersion": 10,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-05-01",
      "ossSupportEnd": "2026-06-01",
      "enterpriseSupportEnd": "2032-06-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/3.5-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/3.5-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 4,
      "version": "3.5.9-SNAPSHOT",
      "majorVersion": 3,
      "minorVersion": 5,
      "patchVersion": 9,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-05-01",
      "ossSupportEnd": "2026-06-01",
      "enterpriseSupportEnd": "2032-06-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/3.5-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/3.5-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 10,
      "version": "3.5.9",
      "majorV...
```

---

### listProjectsBySpringBootVersion

#### Test 1: List compatible projects for 4.0

**Parameters:** `{"majorVersion": 4, "minorVersion": 0, "allVersions": false}`

**Result:**

```json
{
  "springBootVersion": {
    "version": "4.0.2-SNAPSHOT",
    "majorVersion": 4,
    "minorVersion": 0,
    "state": "SNAPSHOT"
  },
  "totalProjects": 43,
  "totalCompatibleVersions": 43,
  "executionTimeMs": 35,
  "projects": [
    {
      "slug": "spring-data-jpa",
      "name": "Spring Data JPA",
      "description": "Spring Data JPA",
      "homepage": "https://spring.io/projects/spring-data-jpa",
      "compatibleVersions": [
        {
          "version": "4.0.1",
          "state": "GA",
          "isLatest": true,
          "referenceDocUrl": "https://docs.spring.io/spring-data/jpa/reference/4.0/",
          "apiDocUrl": "https://docs.spring.io/spring-data/jpa/docs/current/api/"
        }
      ]
    },
    {
      "slug": "spring-amqp",
      "name": "Spring AMQP",
      "description": "Spring AMQP",
      "homepage": "https://spring.io/projects/spring-amqp",
      "compatibleVersions": [
        {
          "version": "4.0.1",
          "state": "GA",
          "isLatest": true,
          "referenceDocUrl": "https://docs.spring.io/spring-amqp/reference/4.0.1",
          "apiDocUrl": "https://docs.spring.io/spring-amqp/docs/4.0.1/api"
        }
      ]
    },
    {
      "slug": "spring-cloud-openfeign",
      "name": "Spring Cloud OpenFeign",
      "description": "Spring Cloud OpenFeign",
      "homepage": "https://spring.io/projects/spring-cloud-openfeign",
      "compatibleVersions": [
        {
          "version": "5.0.0",
          "state": "GA",
          "isLatest": true,
          "referenceDocUrl": "https://docs.spring.io/spring-cloud-openfeign/reference/5.0/",
          "apiDocUrl": ""
        }
      ]
    },
    {
      "slug": "spring-cloud",
      "name": "Spring Cloud",
      "description": "Spring Cloud provides tools for developers to quickly build common patterns in distributed systems",
      "homepage": "https://spring.io/projects/spring-cloud",
      "compatibleVersions": [
        {
          "version": "2025.1.0",
          "state...
```

---

### getWikiReleaseNotes

#### Test 1: Get Spring Boot 4.0 release notes

**Parameters:** `{"version": "4.0"}`

**Result:**

```json
{
  "version": "4.0",
  "title": "Spring Boot 4.0 Release Notes",
  "contentMarkdown": "#\n\n# Upgrading from Spring Boot 3.5 Since this is a major release of Spring Boot, upgrading existing applications can be a little more involved that usual. We\u00e2\u0080\u0099ve put together a [dedicated migration guide](Spring-Boot-4.0-Migration-Guide) to help you upgrade your existing Spring Boot 3.5 applications.\nIf you\u00e2\u0080\u0099re currently running with an earlier version of Spring Boot, we strongly recommend that you [upgrade to Spring Boot 3.5](Spring-Boot-3.5-Release-Notes) before migrating to Spring Boot 4.0.\n\n## New and Noteworthy |-----|---------------------------------------------------------------------------------------------------------------------------------------|\n| Tip | Check [the configuration changelog](Spring-Boot-4.0-Configuration-Changelog) for a complete overview of the changes in configuration. |\n\n### Milestones Available from Maven Central Starting with 4.0.0-M1, all Spring Boot milestones (and release candidates) are now published to Maven Central in addition to <https://repo.spring.io>. This should make it easier to try new milestones in the 4.x line as they become available.\n\n### Gradle 9 Gradle 9 is now supported for building Spring Boot applications. Support for Gradle 8.x (8.14 or later) remains.\n\n### HTTP Service Clients Spring Boot now includes auto-configuration support and configuration properties for HTTP Service Clients. HTTP Service Clients allow you to annotate plain Java interfaces and have Spring automatically create implementations of them.\nFor example, the following interface can be used to call an \"echo\" service:\n\n```\njava\n@HttpExchange(url = \"https://echo.zuplo.io\")\npublic interface EchoService {\n\n@PostExchange\nMap<?, ?> echo(@RequestBody Map<String, String> message);\n\n}\n```\n\nFor full details of the feature, please see the [updated documentation](https://docs.spring.io/spring-boot/4.0-SNAPSHOT/r...
```

#### Test 2: Get Spring Boot 3.5 release notes for current state

**Parameters:** `{"version": "3.5"}`

**Result:**

```json
{
  "version": "3.5",
  "title": "Spring Boot 3.5 Release Notes",
  "contentMarkdown": "#\n\n# Upgrading from Spring Boot 3.4\n\n### spring-boot-parent The `spring-boot-parent` module is no longer published. It provides dependency management for internal dependencies used, for example, in Spring Boot\u00e2\u0080\u0099s own tests. If you were using `spring-boot-parent`, replace it with dependency management of your own that meets your application\u00e2\u0080\u0099s needs.\n\n### Actuator 'heapdump' Endpoint The `heapdump` actuator endpoint now defaults to `access=NONE`. The aims to help reduce the likelihood of a misconfiguration application leaking sensitive information.\nIf you want to use it, you now need to both expose it, and configure access (*previously, you only needed to expose it*).\nTo do that, you can use something similar to following configuration:\n\n```\nyaml\nmanagement:\nendpoints:\nweb:\nexposure:\ninclude: heapdump\nendpoint:\nheapdump:\naccess: unrestricted\n```\n\n### Using '.enabled' and Other Boolean Configuration Properties Supported values for `.enabled` properties have been tightened with this release and are now more consistent. Values must now be either `true` or `false`.\nPrevious versions of Spring Boot would sometimes use conditions that considered any value other than `false` as enabled.\n\n### Validation of Profile Naming Rules for profile naming have been tightened with this release and are now more consistent. Profiles can now only contain `-` (dash), `_` (underscore), letters and digits. Additionally, Profiles are not allowed to start or end with `-` or `_`.\n\n| Note | As of Spring Boot 3.5.1 these restrictions have been relaxed a little to also allow `.`, `+` and `@` characters. You can also set `spring.profiles.validate` to `false` to disable validation entirely. |\n\n### Follow Redirects with TestRestTemplate The `TestRestTemplate` now uses the same follow redirects settings as the regular `RestTemplate`. The `HttpOption.ENABL...
```

---

### getWikiMigrationGuide

#### Test 1: Get migration guide from 3.5 to 4.0

**Parameters:** `{"fromVersion": "3.5", "toVersion": "4.0"}`

**Result:**

```json
{
  "sourceVersion": null,
  "targetVersion": null,
  "migrationPath": null,
  "title": null,
  "contentMarkdown": null,
  "sourceUrl": null,
  "found": false,
  "message": "Migration guide for 3.5 -> 4.0 not found. Available upgrade paths to 4.0 from: 3.0."
}
```

#### Test 2: Get migration guide from 2.7 to 3.0

**Parameters:** `{"fromVersion": "2.7", "toVersion": "3.0"}`

**Result:**

```json
{
  "sourceVersion": null,
  "targetVersion": null,
  "migrationPath": null,
  "title": null,
  "contentMarkdown": null,
  "sourceUrl": null,
  "found": false,
  "message": "Migration guide for 2.7 -> 3.0 not found. Available upgrade paths to 3.0 from: 2.0."
}
```

---

## Use Case 2: App Seeding (Creating New Spring Boot Applications)

This use case covers creating new applications with a specific Spring Boot version.

### listSpringProjects

#### Test 1: List all Spring projects to select dependencies

**Parameters:** `{}`

**Result:**

```json
{
  "count": 56,
  "executionTimeMs": 4,
  "projects": [
    {
      "name": "Spring Framework",
      "slug": "spring-framework",
      "description": "The Spring Framework provides a comprehensive programming and configuration model",
      "homepage": "https://spring.io/projects/spring-framework",
      "github": "https://github.com/spring-projects/spring-framework"
    },
    {
      "name": "Spring Data",
      "slug": "spring-data",
      "description": "Spring Data provides a familiar and consistent Spring-based programming model for data access",
      "homepage": "https://spring.io/projects/spring-data",
      "github": "https://github.com/spring-projects/spring-data"
    },
    {
      "name": "Spring Security",
      "slug": "spring-security",
      "description": "Spring Security is a framework that provides authentication, authorization and protection",
      "homepage": "https://spring.io/projects/spring-security",
      "github": "https://github.com/spring-projects/spring-security"
    },
    {
      "name": "Spring Batch",
      "slug": "spring-batch",
      "description": "Spring Batch provides reusable functions that are essential in processing large volumes of records, including logging/tracing, transaction management, job processing statistics, job restart, skip, and resource management.",
      "homepage": "https://spring.io/projects/spring-batch",
      "github": "https://github.com/spring-projects/spring-batch"
    },
    {
      "name": "Spring AMQP",
      "slug": "spring-amqp",
      "description": "Spring AMQP",
      "homepage": "https://spring.io/projects/spring-amqp",
      "github": "https://github.com/spring-projects/spring-amqp"
    },
    {
      "name": "Spring Cloud Bus",
      "slug": "spring-cloud-bus",
      "description": "Spring Cloud Bus",
      "homepage": "https://spring.io/projects/spring-cloud-bus",
      "github": "https://github.com/spring-projects/spring-cloud-bus"
    },
    {
      "name": "Spring Cloud Circuit Brea...
```

---

### getSpringVersions

#### Test 1: Get available Spring Boot versions

**Parameters:** `{"project": "spring-boot"}`

**Result:**

```json
{
  "project": "Spring Boot",
  "slug": "spring-boot",
  "description": "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications",
  "versions": [
    {
      "version": "4.0.2.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2026-01-02"
    },
    {
      "version": "4.0.2-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "4.0.1-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "4.0.1.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "4.0.1",
      "type": "GA",
      "isLatest": false,
      "isDefault": true,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "4.0.1.RELEASE",
      "type": "GA",
      "isLatest": true,
      "isDefault": false,
      "releaseDate": "2026-01-03"
    },
    {
      "version": "4.0.0.RELEASE",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "4.0.0",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "3.5.10-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "3.5.10.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2026-01-02"
    },
    {
      "version": "3.5.9-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "3.5.9.BUILD-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": fal...
```

#### Test 2: Get Spring Data versions for persistence

**Parameters:** `{"project": "spring-data"}`

**Result:**

```json
{
  "project": "Spring Data",
  "slug": "spring-data",
  "description": "Spring Data provides a familiar and consistent Spring-based programming model for data access",
  "versions": [
    {
      "version": "2026.0.0-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "unknown"
    },
    {
      "version": "2025.1.2-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "2025.1.1-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "2025.1.1",
      "type": "GA",
      "isLatest": true,
      "isDefault": true,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "2025.1.0",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "2025.1.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "2025.0.8-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "2025.0.7",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "2025.0.7-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "2025.0.6",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "2025.0.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "2024.1.13-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      ...
```

#### Test 3: Get Spring Security versions

**Parameters:** `{"project": "spring-security"}`

**Result:**

```json
{
  "project": "Spring Security",
  "slug": "spring-security",
  "description": "Spring Security is a framework that provides authentication, authorization and protection",
  "versions": [
    {
      "version": "7.0.3-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "7.0.2",
      "type": "GA",
      "isLatest": true,
      "isDefault": true,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "7.0.1-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "7.0.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "7.0.0",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-11-01"
    },
    {
      "version": "6.5.8-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "6.5.7",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-05-01"
    },
    {
      "version": "6.5.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "6.4.14-SNAPSHOT",
      "type": "SNAPSHOT",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2024-11-01"
    },
    {
      "version": "6.4.13",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2024-11-01"
    },
    {
      "version": "6.4.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": "6.3.x",
      "type": "GA",
      "isLatest": false,
      "isDefault": false,
      "releaseDate": "2025-12-01"
    },
    {
      "version": ...
```

---

### listSpringBootVersions

#### Test 1: List latest GA versions for new project

**Parameters:** `{"state": "GA", "limit": 5}`

**Result:**

```json
{
  "filters": {
    "state": "GA",
    "limit": 5
  },
  "enterpriseSubscriptionEnabled": true,
  "totalFound": 9,
  "returnedResults": 5,
  "executionTimeMs": 5,
  "versions": [
    {
      "id": 1,
      "version": "3.4.12",
      "majorVersion": 3,
      "minorVersion": 4,
      "patchVersion": 12,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2024-11-01",
      "ossSupportEnd": "2025-12-01",
      "enterpriseSupportEnd": "2026-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/3.4/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/3.4/api/java/index.html",
      "ossSupportActive": false,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 3,
      "version": "3.5.8",
      "majorVersion": 3,
      "minorVersion": 5,
      "patchVersion": 8,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2025-05-01",
      "ossSupportEnd": "2026-06-01",
      "enterpriseSupportEnd": "2032-06-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/3.5/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/3.5/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 7,
      "version": "2.7.x",
      "majorVersion": 2,
      "minorVersion": 7,
      "patchVersion": 0,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2022-05-01",
      "ossSupportEnd": "2023-06-01",
      "enterpriseSupportEnd": "2029-06-01",
      "referenceDocUrl": "",
      "apiDocUrl": "",
      "ossSupportActive": false,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 8,
      "version": "3.3.x",
      "majorVersion": 3,
      "minorVersion": 3,
      "patchVersion": 0,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "20...
```

---

### filterSpringBootVersionsBySupport

#### Test 1: Find actively supported versions

**Parameters:** `{"supportActive": true, "limit": 5}`

**Result:**

```json
{
  "filters": {
    "supportActive": "true",
    "limit": 5
  },
  "enterpriseSubscriptionEnabled": true,
  "supportDateUsed": "enterpriseSupportEnd",
  "totalFound": 5,
  "executionTimeMs": 5,
  "versions": [
    {
      "id": 13,
      "version": "4.0.2-SNAPSHOT",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 2,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 12,
      "version": "4.0.1",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 1,
      "state": "GA",
      "isCurrent": true,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 5,
      "version": "4.0.0",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 0,
      "state": "GA",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.0/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.0/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 11,
  ...
```

---

### searchSpringDocs

#### Test 1: Search for getting started guides

**Parameters:** `{"query": "getting started Spring Boot", "limit": 5}`

**Result:**

```json
{
  "query": "getting started Spring Boot",
  "filters": {
    "project": "all",
    "version": "all",
    "docType": "all"
  },
  "totalResults": 5,
  "returnedResults": 5,
  "executionTimeMs": 77,
  "results": [
    {
      "id": 2863,
      "title": "Tutorials",
      "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.1/documentation/spring-boot-docs/src/docs/antora/modules/tutorial/pages/index.adoc",
      "description": "This section provides tutorials to help you get started using Spring Boot.",
      "project": "Spring Boot",
      "projectSlug": "spring-boot",
      "version": "4.0.1",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 103,
      "title": "Tutorials",
      "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.0/documentation/spring-boot-docs/src/docs/antora/modules/tutorial/pages/index.adoc",
      "description": "This section provides tutorials to help you get started using Spring Boot.",
      "project": "Spring Boot",
      "projectSlug": "spring-boot",
      "version": "4.0.0",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 3051,
      "title": "Authentication",
      "url": "https://github.com/spring-projects/spring-security/blob/7.0.x/docs/modules/ROOT/pages/reactive/authentication/index.adoc",
      "description": "",
      "project": "Spring Security",
      "projectSlug": "spring-security",
      "version": "7.0.x",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 1358,
      "title": "Authentication",
      "url": "https://github.com/spring-projects/spring-security/blob/7.0.0/docs/modules/ROOT/pages/reactive/authentication/index.adoc",
      "description": "",
      "project": "Spring Security",
      "projectSlug": "spring-security",
      "version": "7.0.0",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 1...
```

#### Test 2: Search for REST API documentation

**Parameters:** `{"query": "REST API tutorial", "limit": 5}`

**Result:**

```json
{
  "query": "REST API tutorial",
  "filters": {
    "project": "all",
    "version": "all",
    "docType": "all"
  },
  "totalResults": 5,
  "returnedResults": 5,
  "executionTimeMs": 55,
  "results": [
    {
      "id": 2863,
      "title": "Tutorials",
      "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.1/documentation/spring-boot-docs/src/docs/antora/modules/tutorial/pages/index.adoc",
      "description": "This section provides tutorials to help you get started using Spring Boot.",
      "project": "Spring Boot",
      "projectSlug": "spring-boot",
      "version": "4.0.1",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 103,
      "title": "Tutorials",
      "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.0/documentation/spring-boot-docs/src/docs/antora/modules/tutorial/pages/index.adoc",
      "description": "This section provides tutorials to help you get started using Spring Boot.",
      "project": "Spring Boot",
      "projectSlug": "spring-boot",
      "version": "4.0.0",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 2603,
      "title": "Java DSL",
      "url": "https://github.com/spring-projects/spring-integration/blob/v7.0.1/src/reference/antora/modules/ROOT/pages/dsl.adoc",
      "description": "The Spring Integration Java configuration and DSL provides a set of convenient builders and a fluent API that lets you configure Spring Integration message flows from Spring `@Configuration` classes.",
      "project": "Spring Integration",
      "projectSlug": "spring-integration",
      "version": "7.0.1",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 2240,
      "title": "Java DSL",
      "url": "https://github.com/spring-projects/spring-integration/blob/v7.0.0/src/reference/antora/modules/ROOT/pages/dsl.adoc",
      "description": "The Spring Integration Java config...
```

#### Test 3: Search for JPA setup

**Parameters:** `{"query": "Spring Data JPA configuration", "limit": 5}`

**Result:**

```json
{
  "query": "Spring Data JPA configuration",
  "filters": {
    "project": "all",
    "version": "all",
    "docType": "all"
  },
  "totalResults": 5,
  "returnedResults": 5,
  "executionTimeMs": 82,
  "results": [
    {
      "id": 2603,
      "title": "Java DSL",
      "url": "https://github.com/spring-projects/spring-integration/blob/v7.0.1/src/reference/antora/modules/ROOT/pages/dsl.adoc",
      "description": "The Spring Integration Java configuration and DSL provides a set of convenient builders and a fluent API that lets you configure Spring Integration message flows from Spring `@Configuration` classes.",
      "project": "Spring Integration",
      "projectSlug": "spring-integration",
      "version": "7.0.1",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 2240,
      "title": "Java DSL",
      "url": "https://github.com/spring-projects/spring-integration/blob/v7.0.0/src/reference/antora/modules/ROOT/pages/dsl.adoc",
      "description": "The Spring Integration Java configuration and DSL provides a set of convenient builders and a fluent API that lets you configure Spring Integration message flows from Spring `@Configuration` classes.",
      "project": "Spring Integration",
      "projectSlug": "spring-integration",
      "version": "7.0.0",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id": 864,
      "title": "Static Resources",
      "url": "https://github.com/spring-projects/spring-framework/blob/v7.0.2/framework-docs/modules/ROOT/pages/web/webmvc/mvc-config/static-resources.adoc",
      "description": "This option provides a convenient way to serve static resources from a list of {spring-framework-api}/core/io/Resource.html[`Resource`]-based locations.",
      "project": "Spring Framework",
      "projectSlug": "spring-framework",
      "version": "7.0.2",
      "docType": "GitHub Reference",
      "contentType": "text/markdown"
    },
    {
      "id"...
```

---

### getCodeExamples

#### Test 1: Find REST controller examples

**Parameters:** `{"query": "REST controller", "limit": 10}`

**Result:**

```json
{
  "filters": {
    "query": "REST controller",
    "project": "all",
    "version": "all",
    "language": "all",
    "limit": 10
  },
  "totalFound": 20,
  "returnedResults": 10,
  "executionTimeMs": 80,
  "examples": [
    {
      "id": 412,
      "title": "Testing the Web Layer - Example 7",
      "description": "What You Will Build",
      "codeSnippet": "package com.example.testingweb;\n\nimport static org.assertj.core.api.Assertions.assertThat;\n\nimport org.junit.jupiter.api.Test;\n\nimport org.springframework.beans.factory.annotation.Autowired;\nimport org.springframework.boot.test.context.SpringBootTest;\n\n@SpringBootTest\nclass SmokeTest {\n\n\t@Autowired\n\tprivate HomeController controller;\n\n\t@Test\n\tvoid contextLoads() throws Exception {\n\t\tassertThat(controller).isNotNull();\n\t}\n}",
      "language": "java",
      "category": "Getting Started",
      "tags": [
        "Getting Started",
        "spring-guide",
        "gs"
      ],
      "sourceUrl": "https://spring.io/guides/gs/testing-web",
      "project": "Spring Boot",
      "projectSlug": "spring-boot",
      "version": "4.0.0.RELEASE"
    },
    {
      "id": 1918,
      "title": "Testing the Web Layer - Example 7",
      "description": "What You Will Build",
      "codeSnippet": "package com.example.testingweb;\n\nimport static org.assertj.core.api.Assertions.assertThat;\n\nimport org.junit.jupiter.api.Test;\n\nimport org.springframework.beans.factory.annotation.Autowired;\nimport org.springframework.boot.test.context.SpringBootTest;\n\n@SpringBootTest\nclass SmokeTest {\n\n\t@Autowired\n\tprivate HomeController controller;\n\n\t@Test\n\tvoid contextLoads() throws Exception {\n\t\tassertThat(controller).isNotNull();\n\t}\n}",
      "language": "java",
      "category": "Getting Started",
      "tags": [
        "Getting Started",
        "spring-guide",
        "gs"
      ],
      "sourceUrl": "https://spring.io/guides/gs/testing-web",
      "project": "Spring Boot",
      "projectSl...
```

#### Test 2: Find JPA repository examples

**Parameters:** `{"query": "JPA repository", "limit": 10}`

**Result:**

```json
{
  "filters": {
    "query": "JPA repository",
    "project": "all",
    "version": "all",
    "language": "all",
    "limit": 10
  },
  "totalFound": 20,
  "returnedResults": 10,
  "executionTimeMs": 85,
  "examples": [
    {
      "id": 2018,
      "title": "Svn Config Server",
      "description": "Sample Config Server and Config Client.  The Config Server is configured to use Subversion rather than git",
      "codeSnippet": "GitHub repository: https://github.com/spring-cloud-samples/svn-config-server",
      "language": "java",
      "category": "Sample Repository",
      "tags": [
        "github",
        "sample",
        "spring-cloud-samples",
        "spring-cloud"
      ],
      "sourceUrl": "https://github.com/spring-cloud-samples/svn-config-server",
      "project": "Spring Cloud",
      "projectSlug": "spring-cloud",
      "version": "2025.1.0"
    },
    {
      "id": 302,
      "title": "Svn Config Server",
      "description": "Sample Config Server and Config Client.  The Config Server is configured to use Subversion rather than git",
      "codeSnippet": "GitHub repository: https://github.com/spring-cloud-samples/svn-config-server",
      "language": "java",
      "category": "Sample Repository",
      "tags": [
        "github",
        "sample",
        "spring-cloud-samples",
        "spring-cloud"
      ],
      "sourceUrl": "https://github.com/spring-cloud-samples/svn-config-server",
      "project": "Spring Cloud",
      "projectSlug": "spring-cloud",
      "version": "2025.1.x"
    },
    {
      "id": 311,
      "title": "Spring Cloud Config Vault",
      "description": "Sample application demonstrating how to use Hashicorp Vault as a backend for a Spring Cloud Config server",
      "codeSnippet": "GitHub repository: https://github.com/spring-cloud-samples/spring-cloud-config-vault",
      "language": "java",
      "category": "Sample Repository",
      "tags": [
        "github",
        "sample",
        "spring-cloud-samples",
        ...
```

#### Test 3: Find security config examples

**Parameters:** `{"query": "security configuration", "limit": 10}`

**Result:**

```json
{
  "filters": {
    "query": "security configuration",
    "project": "all",
    "version": "all",
    "language": "all",
    "limit": 10
  },
  "totalFound": 20,
  "returnedResults": 10,
  "executionTimeMs": 77,
  "examples": [
    {
      "id": 229,
      "title": "Securing a Web Application - Example 5",
      "description": "What You Will Build",
      "codeSnippet": "implementation 'org.springframework.boot:spring-boot-starter-security'\nimplementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'\ntestImplementation 'org.springframework.security:spring-security-test'",
      "language": "java",
      "category": "Getting Started",
      "tags": [
        "Getting Started",
        "spring-guide",
        "gs"
      ],
      "sourceUrl": "https://spring.io/guides/gs/securing-web",
      "project": "Spring Security",
      "projectSlug": "spring-security",
      "version": "7.0.x"
    },
    {
      "id": 1883,
      "title": "Securing a Web Application - Example 5",
      "description": "What You Will Build",
      "codeSnippet": "implementation 'org.springframework.boot:spring-boot-starter-security'\nimplementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'\ntestImplementation 'org.springframework.security:spring-security-test'",
      "language": "java",
      "category": "Getting Started",
      "tags": [
        "Getting Started",
        "spring-guide",
        "gs"
      ],
      "sourceUrl": "https://spring.io/guides/gs/securing-web",
      "project": "Spring Security",
      "projectSlug": "spring-security",
      "version": "7.0.2"
    },
    {
      "id": 230,
      "title": "Securing a Web Application - Example 6",
      "description": "What You Will Build",
      "codeSnippet": "implementation(\"org.springframework.boot:spring-boot-starter-security\")\nimplementation(\"org.thymeleaf.extras:thymeleaf-extras-springsecurity6\")\ntestImplementation(\"org.springframework.security:spring-security-test\")",
      "language": "java",...
```

---

### findProjectsByUseCase

#### Test 1: Find projects for web development

**Parameters:** `{"useCase": "web"}`

**Result:**

```json
{
  "useCase": "web",
  "totalFound": 8,
  "executionTimeMs": 40,
  "projects": [
    {
      "name": "Spring Cloud Open Service Broker",
      "slug": "spring-cloud-open-service-broker",
      "description": "Spring Cloud Open Service Broker",
      "homepage": "https://spring.io/projects/spring-cloud-open-service-broker",
      "github": "https://github.com/spring-projects/spring-cloud-open-service-broker"
    },
    {
      "name": "Spring Cloud Circuit Breaker",
      "slug": "spring-cloud-circuitbreaker",
      "description": "Spring Cloud Circuit Breaker",
      "homepage": "https://spring.io/projects/spring-cloud-circuitbreaker",
      "github": "https://github.com/spring-projects/spring-cloud-circuitbreaker"
    },
    {
      "name": "Spring Cloud Data Flow",
      "slug": "spring-cloud-dataflow",
      "description": "Spring Cloud Data Flow",
      "homepage": "https://spring.io/projects/spring-cloud-dataflow",
      "github": "https://github.com/spring-projects/spring-cloud-dataflow"
    },
    {
      "name": "Spring Cloud Stream Applications",
      "slug": "spring-cloud-stream-applications",
      "description": "Spring Cloud Stream Applications",
      "homepage": "https://spring.io/projects/spring-cloud-stream-applications",
      "github": "https://github.com/spring-projects/spring-cloud-stream-applications"
    },
    {
      "name": "Spring Cloud App Broker",
      "slug": "spring-cloud-app-broker",
      "description": "Spring Cloud App Broker",
      "homepage": "https://spring.io/projects/spring-cloud-app-broker",
      "github": "https://github.com/spring-projects/spring-cloud-app-broker"
    },
    {
      "name": "Spring Security",
      "slug": "spring-security",
      "description": "Spring Security is a framework that provides authentication, authorization and protection",
      "homepage": "https://spring.io/projects/spring-security",
      "github": "https://github.com/spring-projects/spring-security"
    },
    {
      "name": "Spring ...
```

#### Test 2: Find projects for database access

**Parameters:** `{"useCase": "database"}`

**Result:**

```json
{
  "useCase": "database",
  "totalFound": 5,
  "executionTimeMs": 45,
  "projects": [
    {
      "name": "Spring Data",
      "slug": "spring-data",
      "description": "Spring Data provides a familiar and consistent Spring-based programming model for data access",
      "homepage": "https://spring.io/projects/spring-data",
      "github": "https://github.com/spring-projects/spring-data"
    },
    {
      "name": "Spring Batch",
      "slug": "spring-batch",
      "description": "Spring Batch provides reusable functions that are essential in processing large volumes of records, including logging/tracing, transaction management, job processing statistics, job restart, skip, and resource management.",
      "homepage": "https://spring.io/projects/spring-batch",
      "github": "https://github.com/spring-projects/spring-batch"
    },
    {
      "name": "Spring Framework",
      "slug": "spring-framework",
      "description": "The Spring Framework provides a comprehensive programming and configuration model",
      "homepage": "https://spring.io/projects/spring-framework",
      "github": "https://github.com/spring-projects/spring-framework"
    },
    {
      "name": "Spring Security",
      "slug": "spring-security",
      "description": "Spring Security is a framework that provides authentication, authorization and protection",
      "homepage": "https://spring.io/projects/spring-security",
      "github": "https://github.com/spring-projects/spring-security"
    },
    {
      "name": "Spring Cloud",
      "slug": "spring-cloud",
      "description": "Spring Cloud provides tools for developers to quickly build common patterns in distributed systems",
      "homepage": "https://spring.io/projects/spring-cloud",
      "github": "https://github.com/spring-cloud"
    }
  ]
}
```

#### Test 3: Find projects for security

**Parameters:** `{"useCase": "security"}`

**Result:**

```json
{
  "useCase": "security",
  "totalFound": 2,
  "executionTimeMs": 41,
  "projects": [
    {
      "name": "Spring Security",
      "slug": "spring-security",
      "description": "Spring Security is a framework that provides authentication, authorization and protection",
      "homepage": "https://spring.io/projects/spring-security",
      "github": "https://github.com/spring-projects/spring-security"
    },
    {
      "name": "Spring Security Kerberos",
      "slug": "spring-security-kerberos",
      "description": "Spring Security Kerberos",
      "homepage": "https://spring.io/projects/spring-security-kerberos",
      "github": "https://github.com/spring-projects/spring-security-kerberos"
    }
  ]
}
```

#### Test 4: Find projects for messaging

**Parameters:** `{"useCase": "messaging"}`

**Result:**

```json
{
  "useCase": "messaging",
  "totalFound": 45,
  "executionTimeMs": 82,
  "projects": [
    {
      "name": "Spring Cloud Open Service Broker",
      "slug": "spring-cloud-open-service-broker",
      "description": "Spring Cloud Open Service Broker",
      "homepage": "https://spring.io/projects/spring-cloud-open-service-broker",
      "github": "https://github.com/spring-projects/spring-cloud-open-service-broker"
    },
    {
      "name": "Spring Cloud Stream Applications",
      "slug": "spring-cloud-stream-applications",
      "description": "Spring Cloud Stream Applications",
      "homepage": "https://spring.io/projects/spring-cloud-stream-applications",
      "github": "https://github.com/spring-projects/spring-cloud-stream-applications"
    },
    {
      "name": "Spring Cloud App Broker",
      "slug": "spring-cloud-app-broker",
      "description": "Spring Cloud App Broker",
      "homepage": "https://spring.io/projects/spring-cloud-app-broker",
      "github": "https://github.com/spring-projects/spring-cloud-app-broker"
    },
    {
      "name": "Spring Cloud Circuit Breaker",
      "slug": "spring-cloud-circuitbreaker",
      "description": "Spring Cloud Circuit Breaker",
      "homepage": "https://spring.io/projects/spring-cloud-circuitbreaker",
      "github": "https://github.com/spring-projects/spring-cloud-circuitbreaker"
    },
    {
      "name": "Spring Cloud Data Flow",
      "slug": "spring-cloud-dataflow",
      "description": "Spring Cloud Data Flow",
      "homepage": "https://spring.io/projects/spring-cloud-dataflow",
      "github": "https://github.com/spring-projects/spring-cloud-dataflow"
    },
    {
      "name": "Spring Cloud Contract",
      "slug": "spring-cloud-contract",
      "description": "Spring Cloud Contract",
      "homepage": "https://spring.io/projects/spring-cloud-contract",
      "github": "https://github.com/spring-projects/spring-cloud-contract"
    },
    {
      "name": "Spring Cloud Gateway",
      "slug": "spring...
```

---

### getDocumentationByVersion

#### Test 1: Get docs for Spring Boot 4.0.1

**Parameters:** `{"project": "spring-boot", "version": "4.0.1"}`

**Result:**

```json
{
  "project": "Spring Boot",
  "projectSlug": "spring-boot",
  "version": "4.0.1",
  "versionType": "GA",
  "isLatest": false,
  "totalDocuments": 158,
  "executionTimeMs": 16,
  "documentationByType": {
    "GitHub Reference": [
      {
        "id": 2816,
        "title": "Calling REST Services",
        "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.1/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/io/rest-client.adoc",
        "description": "Spring Boot provides various convenient ways to call remote REST services. If you are developing a non-blocking reactive application and you're using Spring WebFlux, then you can use javadoc:org.springframework.web.reactive.function.client.WebClient[]. If you prefer imperative APIs then you can use javadoc:org.springframework.web.client.RestClient[] or javadoc:org.springframework.web.client.RestTemplate[].",
        "project": "Spring Boot",
        "projectSlug": "spring-boot",
        "version": "4.0.1",
        "docType": "GitHub Reference",
        "contentType": "text/markdown"
      },
      {
        "id": 2817,
        "title": "Advanced Native Images Topics",
        "url": "https://github.com/spring-projects/spring-boot/blob/v4.0.1/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/packaging/native-image/advanced-topics.adoc",
        "description": "Reflection hints are automatically created for configuration properties by the Spring ahead-of-time engine. Nested configuration properties which are not inner classes, however, *must* be annotated with javadoc:org.springframework.boot.context.properties.NestedConfigurationProperty[format=annotation], otherwise they won't be detected and will not be bindable.",
        "project": "Spring Boot",
        "projectSlug": "spring-boot",
        "version": "4.0.1",
        "docType": "GitHub Reference",
        "contentType": "text/markdown"
      },
      {
        "id": 2818,
        "title": "Recording HTTP Ex...
```

#### Test 2: Get docs for Spring Boot 3.5.9

**Parameters:** `{"project": "spring-boot", "version": "3.5.9"}`

**Result:**

```json
{
  "project": "Spring Boot",
  "projectSlug": "spring-boot",
  "version": "3.5.9",
  "versionType": "GA",
  "isLatest": false,
  "totalDocuments": 0,
  "executionTimeMs": 29,
  "documentationByType": {},
  "allDocuments": []
}
```

---

### getLatestSpringBootVersion

#### Test 1: Get latest GA versions (all current versions)

**Parameters:** `{}`

**Result:**

```json
{
  "totalVersionsForMajorMinor": 1,
  "executionTimeMs": 5,
  "latestVersion": {
    "id": 12,
    "version": "4.0.1",
    "majorVersion": 4,
    "minorVersion": 0,
    "patchVersion": 1,
    "state": "GA",
    "isCurrent": true,
    "releasedAt": "2025-11-01",
    "ossSupportEnd": "2026-12-01",
    "enterpriseSupportEnd": "2027-12-01",
    "referenceDocUrl": "https://docs.spring.io/spring-boot/index.html",
    "apiDocUrl": "https://docs.spring.io/spring-boot/api/java/index.html",
    "ossSupportActive": true,
    "enterpriseSupportActive": true,
    "isEndOfLife": false,
    "supportActive": true
  },
  "allVersions": [
    {
      "id": 12,
      "version": "4.0.1",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 1,
      "state": "GA",
      "isCurrent": true,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    }
  ]
}
```

#### Test 2: Get latest 4.0.x for newest features

**Parameters:** `{"majorVersion": 4, "minorVersion": 0}`

**Result:**

```json
{
  "majorVersion": 4,
  "minorVersion": 0,
  "totalVersionsForMajorMinor": 4,
  "executionTimeMs": 4,
  "latestVersion": {
    "id": 13,
    "version": "4.0.2-SNAPSHOT",
    "majorVersion": 4,
    "minorVersion": 0,
    "patchVersion": 2,
    "state": "SNAPSHOT",
    "isCurrent": false,
    "releasedAt": "2025-11-01",
    "ossSupportEnd": "2026-12-01",
    "enterpriseSupportEnd": "2027-12-01",
    "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/index.html",
    "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/api/java/index.html",
    "ossSupportActive": true,
    "enterpriseSupportActive": true,
    "isEndOfLife": false,
    "supportActive": true
  },
  "allVersions": [
    {
      "id": 13,
      "version": "4.0.2-SNAPSHOT",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 2,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.2-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 6,
      "version": "4.0.1-SNAPSHOT",
      "majorVersion": 4,
      "minorVersion": 0,
      "patchVersion": 1,
      "state": "SNAPSHOT",
      "isCurrent": false,
      "releasedAt": "2025-11-01",
      "ossSupportEnd": "2026-12-01",
      "enterpriseSupportEnd": "2027-12-01",
      "referenceDocUrl": "https://docs.spring.io/spring-boot/4.0.1-SNAPSHOT/index.html",
      "apiDocUrl": "https://docs.spring.io/spring-boot/4.0.1-SNAPSHOT/api/java/index.html",
      "ossSupportActive": true,
      "enterpriseSupportActive": true,
      "isEndOfLife": false,
      "supportActive": true
    },
    {
      "id": 12,
      "version": "4.0.1",
     ...
```

---

### listProjectsBySpringBootVersion

#### Test 1: List compatible projects for new 4.0 app

**Parameters:** `{"majorVersion": 4, "minorVersion": 0, "allVersions": false}`

**Result:**

```json
{
  "springBootVersion": {
    "version": "4.0.2-SNAPSHOT",
    "majorVersion": 4,
    "minorVersion": 0,
    "state": "SNAPSHOT"
  },
  "totalProjects": 43,
  "totalCompatibleVersions": 43,
  "executionTimeMs": 17,
  "projects": [
    {
      "slug": "spring-data-jpa",
      "name": "Spring Data JPA",
      "description": "Spring Data JPA",
      "homepage": "https://spring.io/projects/spring-data-jpa",
      "compatibleVersions": [
        {
          "version": "4.0.1",
          "state": "GA",
          "isLatest": true,
          "referenceDocUrl": "https://docs.spring.io/spring-data/jpa/reference/4.0/",
          "apiDocUrl": "https://docs.spring.io/spring-data/jpa/docs/current/api/"
        }
      ]
    },
    {
      "slug": "spring-amqp",
      "name": "Spring AMQP",
      "description": "Spring AMQP",
      "homepage": "https://spring.io/projects/spring-amqp",
      "compatibleVersions": [
        {
          "version": "4.0.1",
          "state": "GA",
          "isLatest": true,
          "referenceDocUrl": "https://docs.spring.io/spring-amqp/reference/4.0.1",
          "apiDocUrl": "https://docs.spring.io/spring-amqp/docs/4.0.1/api"
        }
      ]
    },
    {
      "slug": "spring-cloud-openfeign",
      "name": "Spring Cloud OpenFeign",
      "description": "Spring Cloud OpenFeign",
      "homepage": "https://spring.io/projects/spring-cloud-openfeign",
      "compatibleVersions": [
        {
          "version": "5.0.0",
          "state": "GA",
          "isLatest": true,
          "referenceDocUrl": "https://docs.spring.io/spring-cloud-openfeign/reference/5.0/",
          "apiDocUrl": ""
        }
      ]
    },
    {
      "slug": "spring-cloud",
      "name": "Spring Cloud",
      "description": "Spring Cloud provides tools for developers to quickly build common patterns in distributed systems",
      "homepage": "https://spring.io/projects/spring-cloud",
      "compatibleVersions": [
        {
          "version": "2025.1.0",
          "state...
```

#### Test 2: List compatible projects for 3.5 app

**Parameters:** `{"majorVersion": 3, "minorVersion": 5, "allVersions": false}`

**Result:**

```json
{
  "springBootVersion": {
    "version": "3.5.10-SNAPSHOT",
    "majorVersion": 3,
    "minorVersion": 5,
    "state": "SNAPSHOT"
  },
  "totalProjects": 48,
  "totalCompatibleVersions": 48,
  "executionTimeMs": 18,
  "projects": [
    {
      "slug": "spring-data-jpa",
      "name": "Spring Data JPA",
      "description": "Spring Data JPA",
      "homepage": "https://spring.io/projects/spring-data-jpa",
      "compatibleVersions": [
        {
          "version": "3.5.7",
          "state": "GA",
          "isLatest": false,
          "referenceDocUrl": "https://docs.spring.io/spring-data/jpa/reference/3.5/",
          "apiDocUrl": "https://docs.spring.io/spring-data/jpa/docs/3.5.7/api/"
        }
      ]
    },
    {
      "slug": "spring-amqp",
      "name": "Spring AMQP",
      "description": "Spring AMQP",
      "homepage": "https://spring.io/projects/spring-amqp",
      "compatibleVersions": [
        {
          "version": "3.2.8",
          "state": "GA",
          "isLatest": false,
          "referenceDocUrl": "https://docs.spring.io/spring-amqp/reference/3.2.8",
          "apiDocUrl": "https://docs.spring.io/spring-amqp/docs/3.2.8/api"
        }
      ]
    },
    {
      "slug": "spring-cloud-openfeign",
      "name": "Spring Cloud OpenFeign",
      "description": "Spring Cloud OpenFeign",
      "homepage": "https://spring.io/projects/spring-cloud-openfeign",
      "compatibleVersions": [
        {
          "version": "4.3.1",
          "state": "GA",
          "isLatest": false,
          "referenceDocUrl": "https://docs.spring.io/spring-cloud-openfeign/reference/4.3/",
          "apiDocUrl": ""
        }
      ]
    },
    {
      "slug": "spring-cloud",
      "name": "Spring Cloud",
      "description": "Spring Cloud provides tools for developers to quickly build common patterns in distributed systems",
      "homepage": "https://spring.io/projects/spring-cloud",
      "compatibleVersions": [
        {
          "version": "2025.0.1",
          "sta...
```

---

### getWikiReleaseNotes

#### Test 1: Get 4.0 features for new project

**Parameters:** `{"version": "4.0"}`

**Result:**

```json
{
  "version": "4.0",
  "title": "Spring Boot 4.0 Release Notes",
  "contentMarkdown": "#\n\n# Upgrading from Spring Boot 3.5 Since this is a major release of Spring Boot, upgrading existing applications can be a little more involved that usual. We\u00e2\u0080\u0099ve put together a [dedicated migration guide](Spring-Boot-4.0-Migration-Guide) to help you upgrade your existing Spring Boot 3.5 applications.\nIf you\u00e2\u0080\u0099re currently running with an earlier version of Spring Boot, we strongly recommend that you [upgrade to Spring Boot 3.5](Spring-Boot-3.5-Release-Notes) before migrating to Spring Boot 4.0.\n\n## New and Noteworthy |-----|---------------------------------------------------------------------------------------------------------------------------------------|\n| Tip | Check [the configuration changelog](Spring-Boot-4.0-Configuration-Changelog) for a complete overview of the changes in configuration. |\n\n### Milestones Available from Maven Central Starting with 4.0.0-M1, all Spring Boot milestones (and release candidates) are now published to Maven Central in addition to <https://repo.spring.io>. This should make it easier to try new milestones in the 4.x line as they become available.\n\n### Gradle 9 Gradle 9 is now supported for building Spring Boot applications. Support for Gradle 8.x (8.14 or later) remains.\n\n### HTTP Service Clients Spring Boot now includes auto-configuration support and configuration properties for HTTP Service Clients. HTTP Service Clients allow you to annotate plain Java interfaces and have Spring automatically create implementations of them.\nFor example, the following interface can be used to call an \"echo\" service:\n\n```\njava\n@HttpExchange(url = \"https://echo.zuplo.io\")\npublic interface EchoService {\n\n@PostExchange\nMap<?, ?> echo(@RequestBody Map<String, String> message);\n\n}\n```\n\nFor full details of the feature, please see the [updated documentation](https://docs.spring.io/spring-boot/4.0-SNAPSHOT/r...
```

---

### getWikiMigrationGuide

#### Test 1: Understand changes from 3.5 to 4.0

**Parameters:** `{"fromVersion": "3.5", "toVersion": "4.0"}`

**Result:**

```json
{
  "sourceVersion": null,
  "targetVersion": null,
  "migrationPath": null,
  "title": null,
  "contentMarkdown": null,
  "sourceUrl": null,
  "found": false,
  "message": "Migration guide for 3.5 -> 4.0 not found. Available upgrade paths to 4.0 from: 3.0."
}
```

---

## Summary

| Tool | Modernization Tests | Seeding Tests | Total |
|------|---------------------|---------------|-------|
| listSpringProjects | 1 | 1 | 2 |
| getSpringVersions | 2 | 3 | 5 |
| listSpringBootVersions | 1 | 1 | 2 |
| filterSpringBootVersionsBySupport | 1 | 1 | 2 |
| searchSpringDocs | 3 | 3 | 6 |
| getCodeExamples | 2 | 3 | 5 |
| findProjectsByUseCase | 1 | 4 | 5 |
| getDocumentationByVersion | 1 | 2 | 3 |
| getLatestSpringBootVersion | 3 | 2 | 5 |
| listProjectsBySpringBootVersion | 1 | 2 | 3 |
| getWikiReleaseNotes | 2 | 1 | 3 |
| getWikiMigrationGuide | 2 | 1 | 3 |
| **Total** | **20** | **24** | **44** |
