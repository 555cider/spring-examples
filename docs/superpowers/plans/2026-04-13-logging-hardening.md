# Logging Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standardize log naming and rolling across `auth`, `client`, and `gateway` with predictable profile-specific behavior.

**Architecture:** Keep one `logback-spring.xml` per module but make all three follow the same convention. Add small configuration-structure tests first, then update logback and application properties.

**Tech Stack:** Spring Boot, Logback, JUnit 5, AssertJ

---

### Task 1: Lock the target logging structure with tests

**Files:**
- Create: `auth/src/test/java/com/example/auth/config/LogbackConfigurationTest.java`
- Create: `client/src/test/java/com/example/client/config/LogbackConfigurationTest.java`
- Create: `gateway/src/test/java/com/example/gateway/config/LogbackConfigurationTest.java`

- [ ] **Step 1: Write the failing tests**

```java
assertThat(xml).contains(".log.gz");
assertThat(xml).contains("<maxHistory>30</maxHistory>");
assertThat(xml).contains("default | local | test");
assertThat(xml).doesNotContain("../logs");
```

- [ ] **Step 2: Run each module test to verify failure**

Run:
- `./gradlew :auth:test --tests com.example.auth.config.LogbackConfigurationTest`
- `./gradlew :client:test --tests com.example.client.config.LogbackConfigurationTest`
- `./gradlew :gateway:test --tests com.example.gateway.config.LogbackConfigurationTest`

Expected: FAIL because the current XML still uses `../logs`, lacks `maxHistory`, and has no gz archive naming.

### Task 2: Apply standardized logback and property configuration

**Files:**
- Modify: `auth/src/main/resources/logback-spring.xml`
- Modify: `client/src/main/resources/logback-spring.xml`
- Modify: `gateway/src/main/resources/logback-spring.xml`
- Modify: `auth/src/main/resources/application.yml`
- Modify: `client/src/main/resources/application.yml`
- Modify: `gateway/src/main/resources/application.yml`

- [ ] **Step 1: Add shared logging path property**

```yaml
app:
  logging:
    path: ${LOG_PATH:./logs}
```

- [ ] **Step 2: Replace duplicated root declarations with profile-scoped roots**

```xml
<springProfile name="default | local | test">
    <logger name="com.example" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</springProfile>
```

- [ ] **Step 3: Add compressed rolling archive naming and limits**

```xml
<file>${LOG_PATH}/${springAppName}.log</file>
<fileNamePattern>${LOG_PATH}/archive/${springAppName}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
<maxFileSize>20MB</maxFileSize>
<maxHistory>30</maxHistory>
<totalSizeCap>1GB</totalSizeCap>
<cleanHistoryOnStart>true</cleanHistoryOnStart>
```

### Task 3: Verify the new configuration

**Files:**
- Test: `auth/src/test/java/com/example/auth/config/LogbackConfigurationTest.java`
- Test: `client/src/test/java/com/example/client/config/LogbackConfigurationTest.java`
- Test: `gateway/src/test/java/com/example/gateway/config/LogbackConfigurationTest.java`

- [ ] **Step 1: Run the targeted tests**

Run:
- `./gradlew :auth:test --tests com.example.auth.config.LogbackConfigurationTest`
- `./gradlew :client:test --tests com.example.client.config.LogbackConfigurationTest`
- `./gradlew :gateway:test --tests com.example.gateway.config.LogbackConfigurationTest`

Expected: PASS

- [ ] **Step 2: Run the full suite**

Run: `./gradlew test`
Expected: PASS
