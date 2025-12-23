# Testing Guide for RFC Server - Eclipse IDE

## Overview

This guide provides comprehensive testing procedures for the RFC Server project using Eclipse IDE. Includes unit tests, integration tests, debugging, and validation procedures.

---

## Part 1: Unit Testing Setup

### 1.1 Test Framework Configuration

#### JUnit 5 (Recommended)

Ensure `pom.xml` includes:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### Mockito for Mocking

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### 1.2 Test Source Directory Structure

Verify Eclipse recognizes test directory:

```
src/test/
├── java/
│   └── org/dataingest/rfc/server/
│       ├── publisher/
│       │   ├── IDocKafkaPublisherTest.java
│       │   └── BWDataKafkaPublisherTest.java
│       ├── util/
│       │   ├── IDocTopicNameUtilTest.java
│       │   └── BWDataTopicNameUtilTest.java
│       ├── config/
│       │   └── ApplicationConfigurationTest.java
│       └── integration/
│           ├── KafkaIntegrationTest.java
│           └── EndToEndTest.java
└── resources/
    ├── application-test.properties
    └── logback-test.xml
```

**Configure in Eclipse:**
1. Right-click `src/test/java` folder
2. **Build Path → Use as Source Folder** (if not already configured)
3. Same for `src/test/resources`

---

## Part 2: Unit Tests

### 2.1 IDocKafkaPublisher Unit Tests

Create file: `src/test/java/org/dataingest/rfc/server/publisher/IDocKafkaPublisherTest.java`

```java
package org.dataingest.rfc.server.publisher;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataingest.rfc.server.exception.KafkaPublishException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IDocKafkaPublisherTest {

    @Mock
    private KafkaProducer<String, String> kafkaProducer;

    private ObjectMapper objectMapper;
    private IDocKafkaPublisher publisher;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new IDocKafkaPublisher(kafkaProducer, objectMapper);
    }

    @Test
    public void testPublishSuccess() {
        // Arrange
        String documentNumber = "DOC123456";
        String topic = "SAP.IDOCS.ORDERS_05";
        String idocJson = "{\"docNumber\":\"DOC123456\",\"type\":\"ORDERS\"}";

        RecordMetadata metadata = mock(RecordMetadata.class);
        when(kafkaProducer.send(any())).thenReturn(
            new org.apache.kafka.clients.producer.FutureRecordMetadata(
                null, new Exception())
        );

        // Act
        assertDoesNotThrow(() -> publisher.publish(topic, documentNumber, idocJson));

        // Assert
        ArgumentCaptor<org.apache.kafka.clients.producer.ProducerRecord> captor =
            ArgumentCaptor.forClass(org.apache.kafka.clients.producer.ProducerRecord.class);
        verify(kafkaProducer).send(captor.capture());
        assertEquals(documentNumber, captor.getValue().key());
        assertEquals(topic, captor.getValue().topic());
    }

    @Test
    public void testPublishTimeout() {
        // Arrange
        String topic = "SAP.IDOCS.ORDERS_05";
        String documentNumber = "DOC123456";
        String idocJson = "{}";

        when(kafkaProducer.send(any())).thenThrow(
            new org.apache.kafka.common.errors.TimeoutException("Producer timeout")
        );

        // Act & Assert
        assertThrows(KafkaPublishException.class, () ->
            publisher.publish(topic, documentNumber, idocJson)
        );
    }

    @Test
    public void testPublishWithNullTopic() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            publisher.publish(null, "DOC123", "{}")
        );
    }

    @Test
    public void testPublishWithEmptyJson() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            publisher.publish("SAP.IDOCS.ORDERS_05", "DOC123", "")
        );
    }
}
```

### 2.2 BWDataKafkaPublisher Unit Tests

Create file: `src/test/java/org/dataingest/rfc/server/publisher/BWDataKafkaPublisherTest.java`

```java
package org.dataingest.rfc.server.publisher;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataingest.rfc.server.exception.KafkaPublishException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BWDataKafkaPublisherTest {

    @Mock
    private KafkaProducer<String, String> kafkaProducer;

    private ObjectMapper objectMapper;
    private BWDataKafkaPublisher publisher;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new BWDataKafkaPublisher(kafkaProducer, objectMapper);
    }

    @Test
    public void testPublishDataSourceSuccess() {
        // Arrange
        String requestId = "REQ-001";
        String topic = "SAP.DATASOURCES.0MATERIAL_ATTR";
        String dataJson = "{\"requestId\":\"REQ-001\",\"datasource\":\"0MATERIAL_ATTR\"}";

        // Act
        assertDoesNotThrow(() -> publisher.publish(topic, requestId, dataJson));

        // Assert
        verify(kafkaProducer).send(any());
    }

    @Test
    public void testPublishKafkaFailure() {
        // Arrange
        when(kafkaProducer.send(any())).thenThrow(
            new RuntimeException("Kafka broker unavailable")
        );

        // Act & Assert
        assertThrows(KafkaPublishException.class, () ->
            publisher.publish("SAP.DATASOURCES.0MATERIAL_ATTR", "REQ-001", "{}")
        );
    }
}
```

### 2.3 Topic Name Utility Tests

Create file: `src/test/java/org/dataingest/rfc/server/util/IDocTopicNameUtilTest.java`

```java
package org.dataingest.rfc.server.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IDOC Topic Name Utility Tests")
public class IDocTopicNameUtilTest {

    private final IDocTopicNameUtil util = new IDocTopicNameUtil();

    @Test
    @DisplayName("Should generate correct topic name for ORDERS IDOC")
    public void testGenerateOrdersTopicName() {
        // Arrange & Act
        String topic = util.generateTopicName("ORDERS", "05");

        // Assert
        assertEquals("SAP.IDOCS.ORDERS_05", topic);
    }

    @Test
    @DisplayName("Should generate correct topic name for INVOIC IDOC")
    public void testGenerateInvoicTopicName() {
        // Arrange & Act
        String topic = util.generateTopicName("INVOIC", "01");

        // Assert
        assertEquals("SAP.IDOCS.INVOIC_01", topic);
    }

    @Test
    @DisplayName("Should sanitize special characters in IDOC type")
    public void testSanitizeSpecialCharacters() {
        // Arrange & Act
        String topic = util.generateTopicName("ORDERS-05.TEST", "01");

        // Assert - special chars should be removed or replaced
        assertTrue(topic.matches("^SAP\\.IDOCS\\.[A-Z0-9_]+$"));
    }

    @Test
    @DisplayName("Should throw exception for null IDOC type")
    public void testNullIdocType() {
        assertThrows(IllegalArgumentException.class, () ->
            util.generateTopicName(null, "05")
        );
    }

    @Test
    @DisplayName("Should handle empty extension")
    public void testEmptyExtension() {
        assertThrows(IllegalArgumentException.class, () ->
            util.generateTopicName("ORDERS", "")
        );
    }
}
```

### 2.4 Configuration Tests

Create file: `src/test/java/org/dataingest/rfc/server/config/ApplicationConfigurationTest.java`

```java
package org.dataingest.rfc.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApplicationConfigurationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testObjectMapperBeanCreated() {
        assertNotNull(objectMapper);
    }

    @Test
    public void testObjectMapperSerialization() throws Exception {
        // Test JSON serialization
        String json = objectMapper.writeValueAsString(
            new TestData("value", 123)
        );
        assertTrue(json.contains("\"testField\":\"value\""));
    }

    // Inner test class
    static class TestData {
        public String testField;
        public int testValue;

        TestData(String field, int value) {
            this.testField = field;
            this.testValue = value;
        }
    }
}
```

---

## Part 3: Integration Tests

### 3.1 Embedded Kafka Setup

#### Add Dependencies to pom.xml

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.17.6</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.17.6</version>
    <scope>test</scope>
</dependency>
```

### 3.2 Kafka Integration Test

Create file: `src/test/java/org/dataingest/rfc/server/integration/KafkaIntegrationTest.java`

```java
package org.dataingest.rfc.server.integration;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    private static String bootstrapServers;

    @BeforeAll
    static void setup() {
        bootstrapServers = kafka.getBootstrapServers();
    }

    @Test
    public void testPublishAndConsumeMessage() throws Exception {
        // Create producer
        KafkaProducer<String, String> producer =
            createProducer(bootstrapServers);

        // Publish message
        ProducerRecord<String, String> record =
            new ProducerRecord<>(
                "SAP.IDOCS.ORDERS_05",
                "DOC123",
                "{\"documentNumber\":\"DOC123\",\"type\":\"ORDERS\"}"
            );
        producer.send(record).get();
        producer.flush();

        // Create consumer
        KafkaConsumer<String, String> consumer =
            createConsumer(bootstrapServers);
        consumer.subscribe(java.util.Arrays.asList("SAP.IDOCS.ORDERS_05"));

        // Consume message
        ConsumerRecords<String, String> records =
            consumer.poll(java.time.Duration.ofSeconds(10));

        // Assertions
        assertFalse(records.isEmpty());
        assertEquals("DOC123", records.iterator().next().key());

        producer.close();
        consumer.close();
    }

    private KafkaProducer<String, String> createProducer(String bootstrapServers) {
        var props = new java.util.Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<>(props);
    }

    private KafkaConsumer<String, String> createConsumer(String bootstrapServers) {
        var props = new java.util.Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "test-group");
        props.put("key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");
        return new KafkaConsumer<>(props);
    }
}
```

### 3.3 End-to-End Test

Create file: `src/test/java/org/dataingest/rfc/server/integration/EndToEndTest.java`

```java
package org.dataingest.rfc.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class EndToEndTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testApplicationHealthCheck() {
        // Check health endpoint
        var response = restTemplate.getForObject(
            "/actuator/health",
            String.class
        );

        assertNotNull(response);
        assertTrue(response.contains("UP"));
    }

    @Test
    public void testApplicationInfoEndpoint() {
        // Check info endpoint
        var response = restTemplate.getForObject(
            "/actuator/info",
            String.class
        );

        assertNotNull(response);
    }
}
```

---

## Part 4: Running Tests in Eclipse

### 4.1 Run All Tests

**Method 1: Right-click project**
```
Right-click project → Run As → Maven test
```

**Method 2: Maven CLI**
```bash
cd D:\RFC_SERVER\ProjectRFC
mvn test
```

**Method 3: Run Configuration**
1. **Run → Run Configurations...**
2. Create new **Maven Build**
3. **Base directory:** Project root
4. **Goals:** `test`
5. Click **Run**

### 4.2 Run Single Test Class

1. Right-click test class
2. Select **Run As → JUnit Test**

Or use Maven CLI:
```bash
mvn test -Dtest=IDocKafkaPublisherTest
```

### 4.3 Run Tests with Specific Tags

```bash
mvn test -Dgroups=@DisplayName
```

### 4.4 Monitor Test Execution

In Eclipse **JUnit** view:
- ✅ Green bar = all tests passed
- ❌ Red bar = one or more tests failed
- ⏸️ Blue bar = paused (breakpoint in test)

### 4.5 View Test Results

1. **Window → Show View → Other → JUnit**
2. Tests appear in tree:
   - Package names
   - Test class names
   - Individual test methods
3. Click test to see:
   - Pass/fail status
   - Execution time
   - Stack trace if failed

---

## Part 5: Debugging Tests

### 5.1 Debug Single Test

1. Right-click test class
2. Select **Debug As → JUnit Test**
3. Application pauses at breakpoints

### 5.2 Set Breakpoints in Tests

1. Click in left margin of test code
2. Blue circle appears
3. Run test in debug mode
4. Execution pauses at breakpoint

### 5.3 Debug with Variables

While debugging:
1. Open **Variables** view
2. Expand test objects
3. Inspect field values
4. Right-click variable → **Watch** to monitor

### 5.4 Step Through Test Execution

| Key | Action |
|-----|--------|
| F5 | Step into method |
| F6 | Step over line |
| F7 | Step out of method |
| F8 | Resume execution |
| Ctrl+Shift+I | Inspect expression |

---

## Part 6: Test Coverage

### 6.1 Add Code Coverage Tool (JaCoCo)

Add to `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 6.2 Generate Coverage Report

```bash
mvn clean test jacoco:report
```

Report location: `target/site/jacoco/index.html`

### 6.3 View Coverage in Eclipse

1. Right-click project
2. Select **Coverage As → Maven test (with coverage)**
3. Coverage statistics appear in editor gutter
4. Green = covered, Red = not covered, Yellow = partial

---

## Part 7: Test Configuration Files

### 7.1 application-test.properties

Create file: `src/test/resources/application-test.properties`

```properties
# Spring Boot Test Configuration
spring.application.name=rfc-server-test
server.port=8081

# Kafka Test Configuration (use test container or embedded)
kafka.bootstrap.servers=localhost:29092
kafka.acks=all
kafka.retries=1
kafka.max.in.flight.requests.per.connection=1
kafka.compression.type=none
kafka.enable.idempotence=true
kafka.request.timeout.ms=10000
kafka.delivery.timeout.ms=30000

# Feature Flags
feature.idoc.enabled=true
feature.idoc.transactional=true
feature.bw_source_system.enabled=true

# Logging (verbose for tests)
logging.level.root=INFO
logging.level.org.dataingest.rfc.server=DEBUG
logging.level.org.apache.kafka=WARN
logging.level.org.springframework.boot=INFO
logging.level.org.springframework.test=DEBUG
```

### 7.2 logback-test.xml

Create file: `src/test/resources/logback-test.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="org.dataingest.rfc.server" level="DEBUG"/>
    <logger name="org.springframework" level="INFO"/>
    <logger name="org.apache.kafka" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## Part 8: Common Test Issues & Solutions

| Issue | Solution |
|-------|----------|
| **Tests not found** | Ensure class name ends with `Test`, rebuild project |
| **Timeout on test** | Increase timeout: `@Test(timeout = 5000)` or remove for integration tests |
| **Mock not working** | Add `@ExtendWith(MockitoExtension.class)` to test class |
| **Kafka connection failed** | Use TestContainers or verify Kafka running on correct port |
| **Transaction not committed** | Add `@Transactional` to test or use `@DataJpaTest` |
| **Resource not found** | Verify file in `src/test/resources`, check `build-helper-maven-plugin` |

---

## Part 9: Test Best Practices

### 9.1 Test Structure (AAA Pattern)

```java
@Test
public void testSomething() {
    // Arrange - Set up test data
    String input = "test";
    String expected = "TEST";

    // Act - Execute code under test
    String actual = input.toUpperCase();

    // Assert - Verify results
    assertEquals(expected, actual);
}
```

### 9.2 Naming Conventions

```java
// Good test names (describe what is tested)
@Test
public void shouldPublishToKafkaWhenValidIDocReceived() { }

@Test
public void shouldThrowExceptionWhenKafkaConnectionFails() { }

@Test
public void shouldReturnCorrectTopicNameForOrdersIdoc() { }

// Bad test names (unclear)
@Test
public void test1() { }

@Test
public void testPublish() { }
```

### 9.3 Use DisplayName for Clarity

```java
@Test
@DisplayName("Should successfully publish IDOC to Kafka when data is valid")
public void testPublishSuccess() {
    // Test implementation
}
```

### 9.4 Test Independence

Each test should:
- Be independent (no order dependencies)
- Clean up after itself
- Not share state with other tests
- Use `@BeforeEach` for setup
- Use `@AfterEach` for cleanup

---

## Part 10: Continuous Testing

### 10.1 Enable Auto-Test Execution

**Prerequisite:** Install Infinitest Eclipse plugin
- **Help → Eclipse Marketplace**
- Search: "Infinitest"
- Install and restart Eclipse

**Usage:**
- Tests run automatically when code changes
- Green bar in left margin = tests passed
- Red bar = tests failed
- Helps catch regressions immediately

### 10.2 Git Hooks for Tests

Create file: `.git/hooks/pre-commit`

```bash
#!/bin/sh
# Run tests before committing
mvn clean test

if [ $? -ne 0 ]; then
    echo "Tests failed. Commit aborted."
    exit 1
fi
```

Make executable:
```bash
chmod +x .git/hooks/pre-commit
```

---

## Part 11: Performance Testing

### 11.1 Basic Performance Test

```java
@Test
@DisplayName("Should publish 1000 messages in under 5 seconds")
public void testPublishPerformance() throws Exception {
    // Arrange
    IDocKafkaPublisher publisher = new IDocKafkaPublisher(kafkaProducer, mapper);
    int messageCount = 1000;

    // Act & Measure
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < messageCount; i++) {
        publisher.publish(
            "SAP.IDOCS.ORDERS_05",
            "DOC" + i,
            "{\"id\":" + i + "}"
        );
    }

    long duration = System.currentTimeMillis() - startTime;

    // Assert
    assertTrue(duration < 5000, "Publishing took " + duration + "ms");
}
```

### 11.2 Load Testing with JMH

Add dependency:
```xml
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.35</version>
    <scope>test</scope>
</dependency>
```

---

## Part 12: Test Report Generation

### 12.1 Generate Test Report

```bash
# Run tests and generate report
mvn clean test surefire-report:report

# Report location
target/site/surefire-report.html
```

### 12.2 View Report in Browser

1. Open: `target/site/surefire-report.html`
2. Shows:
   - Total tests run
   - Successes/failures
   - Execution time
   - Stack traces for failures

---

## Part 13: Testing Checklist

Before deploying, verify:

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code coverage > 80%
- [ ] No compiler warnings
- [ ] No Maven build warnings
- [ ] Test execution time < 5 minutes
- [ ] Kafka integration tested
- [ ] Error scenarios tested
- [ ] Edge cases covered
- [ ] Documentation updated

---

## Quick Reference

### Common Test Commands

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=IDocKafkaPublisherTest

# Run tests matching pattern
mvn test -Dtest=*Integration*

# Run with coverage
mvn clean test jacoco:report

# Skip tests during build
mvn package -DskipTests

# Run with debug output
mvn test -X

# Run failing tests only
mvn test --fail-at-end
```

### Test Annotations Quick Reference

```java
@Test                           // Mark as test method
@BeforeEach                     // Setup before each test
@AfterEach                      // Cleanup after each test
@BeforeAll                      // Setup before all tests (static)
@AfterAll                       // Cleanup after all tests (static)
@DisplayName("description")     // Custom test name
@Disabled                       // Skip test
@ParameterizedTest             // Run with multiple parameters
@ExtendWith(MockitoExtension)  // Enable Mockito
@SpringBootTest                // Load Spring context
@MockBean                       // Mock Spring bean
@Autowired                      // Inject Spring bean
```

---

**Document Version:** 1.0.0
**Last Updated:** 2024-12-18
**Status:** Complete - Ready for Testing
