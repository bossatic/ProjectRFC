# RFC Server Refactoring - Remaining Implementation Tasks

## Overview
This document outlines the remaining tasks needed to complete the refactoring of the Talend SAP RFC Server.

## Task 1: Update ApplicationProperties.java

### File Location
`org/talend/sap/impl/server/configuration/ApplicationProperties.java`

### Current Implementation
```java
@Configuration
public class ApplicationProperties implements InitializingBean {

    @Autowired
    protected ConfigurableEnvironment environment;

    protected Properties kafkaConsumerProperties;  // REMOVE THIS
    protected Properties kafkaProducerProperties;  // KEEP THIS

    public void afterPropertiesSet() throws Exception {
        // Load both consumer and producer properties
        // REMOVE consumer loading logic
    }

    @Bean
    @Qualifier("kafka-consumer")
    public Properties kafkaConsumerProperties() {  // REMOVE THIS BEAN
        // ...
    }

    @Bean
    @Qualifier("kafka-producer")
    public Properties kafkaProducerProperties() {  // KEEP THIS BEAN
        // ...
    }
}
```

### Required Changes
1. **Remove the following:**
   - `protected Properties kafkaConsumerProperties;` field
   - Consumer properties loading logic (lines for kafka-consumer prefix)
   - `@Bean kafkaConsumerProperties()` method
   - All consumer-specific property filtering

2. **Keep the following:**
   - `protected Properties kafkaProducerProperties;` field
   - Producer properties loading logic
   - `@Bean kafkaProducerProperties()` method
   - Helper methods: `getProperties()`, `startsWith()`, `stripPrefix()`

3. **Update to new package:**
   - Change package from `org.talend.sap.impl.server.configuration` to `org.dataingest.rfc.server.config`

### New Package Location
`org/dataingest/rfc/server/config/ApplicationProperties.java`

---

## Task 2: Update Named Connection Service

### File Locations
1. `org/talend/sap/impl/server/named/SAPNamedConnectionServiceImpl.java`
2. `org/talend/sap/server/named/SAPNamedConnectionFeature.java`

### Changes to SAPNamedConnectionServiceImpl

#### Remove the following:
1. **Mock Implementation Fields:**
   ```java
   private final Map<String, SAPBWSourceSystemMock> bwSourceSystemMocks;
   private final Map<String, SAPIDocReceiverMock> idocReceiverMocks;
   ```

2. **Mock Initialization in Constructor:**
   ```java
   this.bwSourceSystemMocks = new HashMap<>();
   this.idocReceiverMocks = new HashMap<>();
   ```

3. **Streaming Feature Injection:**
   ```java
   @Autowired
   protected SAPStreamReceiverFactory streamReceiverFactory;
   ```

4. **Mock Startup Logic in `afterPropertiesSet()`:**
   - Lines that start mock implementations for BW and IDOC

5. **Mock Creation Logic in `createServer()`:**
   - Lines that check for `feature.bw_source_system.mock.enabled`
   - Lines that check for `feature.idoc.mock.enabled`
   - Lines that return mock instances

6. **Streaming Feature Logic in `createServer()`:**
   - Lines that check for `feature.streaming.enabled`
   - Lines that create stream receivers
   - Lines that add stream receivers to server

#### Update the following:

1. **Feature Check Logic:**
   ```java
   // OLD
   if (!connection.isAtLeastOneFeatureEnabled(
       new SAPNamedConnectionFeature[] {
           BW_SOURCE_SYSTEM,
           BW_SOURCE_SYSTEM_MOCK,
           IDOC,
           IDOC_MOCK,
           STREAMING
       })) {
       return null;
   }

   // NEW
   if (!connection.isAtLeastOneFeatureEnabled(
       new SAPNamedConnectionFeature[] {
           BW_SOURCE_SYSTEM,
           IDOC
       })) {
       return null;
   }
   ```

2. **Move package:**
   - From: `org/talend/sap/impl/server/named/`
   - To: `org/dataingest/rfc/server/named/`

### Changes to SAPNamedConnectionFeature Enum

#### Remove enum constants:
```java
STREAMING("feature.streaming.enabled"),
BW_SOURCE_SYSTEM_MOCK("feature.bw_source_system.mock.enabled"),
IDOC_MOCK("feature.idoc.mock.enabled"),
```

#### Keep enum constants:
```java
BW_SOURCE_SYSTEM("feature.bw_source_system.enabled"),
IDOC("feature.idoc.enabled"),
```

#### Move package:
- From: `org/talend/sap/server/named/`
- To: `org/dataingest/rfc/server/named/`

---

## Task 3: Update ApplicationPropertiesEncryption.java

### File Location
`org/talend/sap/impl/server/configuration/ApplicationPropertiesEncryption.java`

### Required Changes

1. **Remove JMS encrypted property keys:**
   ```java
   // REMOVE THESE LINES
   ENCRYPTED_KEYS.add("jms.ssl.keystore.password");
   ENCRYPTED_KEYS.add("jms.login.password");
   ```

2. **Keep SAP JCo encrypted keys:**
   ```java
   // KEEP THESE
   ENCRYPTED_KEYS.add("jco.client.passwd");
   ENCRYPTED_KEYS.add("jco.client.saprouter");
   ENCRYPTED_KEYS.add("jco.server.saprouter");
   ```

3. **Move package:**
   - From: `org/talend/sap/impl/server/configuration/`
   - To: `org/dataingest/rfc/server/config/`

---

## Task 4: Remove Obsolete Files (30 Files)

### JMS Configuration (9 files)
Location: `org/talend/sap/impl/server/configuration/`

```
1. JmsBrokerConfiguration.java
2. JmsBrokerCondition.java
3. JmsBrokerSslContext.java
4. JmsRemoteBrokerConfiguration.java
5. JmsRemoteBrokerCondition.java
6. JmsReconnect.java
7. JmsQueueReconnect.java
8. JmsTopicReconnect.java
9. JmsReconnectTask.java
```

### JMS Publishers (2 files)
Location: `org/talend/sap/impl/server/`

```
1. SAPIDocPublisher.java
2. SAPBWDataRequestPublisher.java
```

### Helper (1 file)
Location: `org/talend/sap/impl/server/configuration/`

```
1. Holder.java
```

### Streaming Components (13 files)

**Controller (1 file):**
```
1. SAPStreamController.java
   Location: org/talend/sap/impl/server/controller/
```

**Exceptions (3 files):**
```
1. BadRequestException.java
   Location: org/talend/sap/impl/server/controller/exception/
2. NotFoundException.java
   Location: org/talend/sap/impl/server/controller/exception/
3. ServiceUnavailableException.java
   Location: org/talend/sap/impl/server/controller/exception/
```

**Stream Receiver Factory (2 files):**
```
1. SAPStreamReceiverFactory.java (interface)
   Location: org/talend/sap/server/
2. SAPStreamReceiverFactoryImpl.java (implementation)
   Location: org/talend/sap/impl/server/
```

**Kafka Admin Components (6 files):**
```
1. KafkaAdmin.java (interface)
   Location: org/talend/sap/server/
2. KafkaAdminImpl.java (implementation)
   Location: org/talend/sap/impl/server/
3. KafkaAdminFactory.java (interface)
   Location: org/talend/sap/server/
4. KafkaAdminFactoryImpl.java (implementation)
   Location: org/talend/sap/impl/server/
5. KafkaConsumerFactory.java (interface)
   Location: org/talend/sap/server/
6. KafkaConsumerFactoryImpl.java (implementation)
   Location: org/talend/sap/impl/server/
```

**Configuration (1 file):**
```
1. ThreadPoolConfiguration.java
   Location: org/talend/sap/impl/server/configuration/
```

### Mock Implementations (2 files)
Location: `org/talend/sap/impl/server/`

```
1. SAPIDocReceiverMock.java
2. SAPBWSourceSystemMock.java
```

### Sample Data (3 files)
Location: `sample-idocs/` (in JAR resources)

```
1. 0000000000813429.txt
2. 0000000000814496.txt
3. 0000000000814490.txt
```

---

## Task 5: Update Application Properties File

### Location
`src/main/resources/application.properties` or `src/main/resources/application.yml`

### Properties to Remove

**All JMS Properties:**
```properties
jms.bindAddress
jms.persistent
jms.dataDirectory
jms.ssl.keystore.path
jms.ssl.keystore.password
jms.login.config
jms.login.configDomain
jms.login.username
jms.login.password
jms.useJmx
jms.broker.url
jms.reconnect.interval
jms.durable.queue.replicate
jms.durable.queue.retentionPeriod
```

**Mock Feature Flags:**
```properties
feature.idoc.mock.enabled
feature.bw_source_system.mock.enabled
```

**Streaming Feature Flags:**
```properties
feature.streaming.enabled
feature.streaming.limit.parallel
feature.streaming.threadCount
feature.streaming.timeout
feature.streaming.topic.partitionCount
feature.streaming.topic.replicationFactor
```

**Kafka Consumer Properties:**
```properties
kafka.group.id
kafka.auto.offset.reset
kafka.enable.auto.commit
# ... all other kafka.* consumer properties
```

### Properties to Keep

**Kafka Producer Properties (Examples):**
```properties
kafka.bootstrap.servers=localhost:9092
kafka.acks=all
kafka.retries=3
kafka.max.in.flight.requests.per.connection=1
kafka.compression.type=gzip
kafka.enable.idempotence=true
kafka.request.timeout.ms=30000
kafka.delivery.timeout.ms=120000
kafka.linger.ms=10
kafka.batch.size=16384
```

**Feature Flags:**
```properties
feature.idoc.enabled=true
feature.idoc.transactional=true
feature.idoc.transactionAbortTimeout=60000
feature.bw_source_system.enabled=true
```

**Spring Configuration:**
```properties
spring.application.name=rfc-server
server.port=8080
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

**SAP JCo Properties:**
```properties
# Keep all jco.* properties unchanged
```

---

## Task 6: Update Spring Boot Application Class

### File Location
`org/talend/sap/impl/server/SAPServerApplication.java`

### Required Changes

1. **Update package name:**
   - From: `org.talend.sap.impl.server`
   - To: `org.dataingest.rfc.server`

2. **Update application name:**
   ```java
   // OLD
   @SpringBootApplication
   public class SAPServerApplication {
       public static void main(String[] args) {
           System.setProperty("spring.application.name", "tsap-rfc-server");
           // ...
       }
   }

   // NEW
   @SpringBootApplication
   public class RFCServerApplication {
       public static void main(String[] args) {
           System.setProperty("spring.application.name", "rfc-server");
           // ...
       }
   }
   ```

3. **Remove Gson auto-configuration if present:**
   ```java
   // If it exists, remove this
   @SpringBootApplication(exclude = { GsonAutoConfiguration.class })
   ```

---

## Task 7: Update Other Classes (Move to New Package)

### Classes to Move and Update Package Name

All files in `org.talend.sap` package structure should be moved to `org.dataingest.rfc.server`:

1. **Utility Classes:**
   - Move `SAPIDocNameUtil.java` to `org/dataingest/rfc/server/util/`
   - Move `SAPBWDataRequestNameUtil.java` to `org/dataingest/rfc/server/util/`
   - Update to change topic prefix from `TALEND` to `SAP`

2. **BAPI Handlers:**
   - Move `BapiBwFunctionExists.java` to `org/dataingest/rfc/server/bapi/`
   - Move `BapiDSourceIsSupported.java` to `org/dataingest/rfc/server/bapi/`

3. **Named Connection Classes:**
   - Move `SAPNamedConnectionImpl.java` to `org/dataingest/rfc/server/named/`
   - Move `SAPNamedConnectionServiceImpl.java` to `org/dataingest/rfc/server/named/`
   - Move `SAPNamedConnectionFeature.java` to `org/dataingest/rfc/server/named/`

4. **Configuration:**
   - Move `KafkaProducerConfiguration.java` to `org/dataingest/rfc/server/config/`
   - Move `KafkaCondition.java` to `org/dataingest/rfc/server/config/`

---

## Task 8: Testing

### Unit Tests

Create unit tests for:

1. **IDocKafkaPublisher Tests:**
   ```java
   - testPublishIdoc()
   - testPublishIdocWithInvalidData()
   - testPublishIdocKafkaTimeout()
   - testPublishIdocSerializationError()
   ```

2. **BWDataKafkaPublisher Tests:**
   ```java
   - testPublishDataRequest()
   - testPublishDataRequestWithInvalidData()
   - testPublishDataRequestKafkaTimeout()
   ```

3. **Topic Name Utility Tests:**
   ```java
   - testIDocTopicNaming()
   - testBWDataTopicNaming()
   - testSpecialCharacterHandling()
   ```

### Integration Tests

1. **Embedded Kafka Tests:**
   - Test publishing to embedded Kafka broker
   - Verify message format and content
   - Test consumer reading published messages

2. **Transaction Tests:**
   - Test SAP transaction commit on successful publish
   - Test SAP transaction rollback on publish failure
   - Test rollback on serialization error

3. **Configuration Tests:**
   - Verify Jackson ObjectMapper configuration
   - Verify Kafka producer bean creation
   - Verify property loading

### End-to-End Tests

1. Send test IDOC from SAP system
2. Verify message appears in Kafka topic
3. Verify message format (JSON)
4. Verify transaction status

---

## Implementation Checklist

- [ ] Task 1: Update ApplicationProperties.java
- [ ] Task 2: Update Named Connection Service classes
- [ ] Task 3: Update ApplicationPropertiesEncryption.java
- [ ] Task 4: Remove 30 obsolete files
- [ ] Task 5: Update application.properties configuration file
- [ ] Task 6: Update Spring Boot application class
- [ ] Task 7: Move all classes to new package structure
- [ ] Task 8: Create and run unit tests
- [ ] Task 9: Create and run integration tests
- [ ] Task 10: Build Maven project successfully
- [ ] Task 11: Verify no compilation errors
- [ ] Task 12: Verify no runtime errors

---

## File Locations Reference

**New Classes (Already Created):**
- `D:/RFC_SERVER/ProjectRFC/ApplicationConfiguration.java`
- `D:/RFC_SERVER/ProjectRFC/KafkaPublishException.java`
- `D:/RFC_SERVER/ProjectRFC/IDocKafkaPublisher.java`
- `D:/RFC_SERVER/ProjectRFC/BWDataKafkaPublisher.java`
- `D:/RFC_SERVER/ProjectRFC/IDocTopicNameUtil.java`
- `D:/RFC_SERVER/ProjectRFC/BWDataTopicNameUtil.java`
- `D:/RFC_SERVER/ProjectRFC/IDocReceiverFactoryImpl.java`
- `D:/RFC_SERVER/ProjectRFC/BWDataSourceFactoryImpl.java`
- `D:/RFC_SERVER/ProjectRFC/IDocReceiverAdapter.java`
- `D:/RFC_SERVER/ProjectRFC/BWDataSourceAdapter.java`

**Documentation:**
- `D:/RFC_SERVER/ProjectRFC/REFACTORING_PLAN.md`
- `D:/RFC_SERVER/ProjectRFC/IMPLEMENTATION_SUMMARY.md`
- `D:/RFC_SERVER/ProjectRFC/REMAINING_TASKS.md` (this file)

---

**Total Estimated Effort:**
- Reading and understanding: 4-6 hours
- Code modifications: 12-16 hours
- Testing: 8-12 hours
- Total: 24-34 hours (3-4 days)
