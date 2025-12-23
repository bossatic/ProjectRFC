# SAP RFC Server - Refactoring Plan

## Overview

Simplify the SAP RFC Server by removing JMS/ActiveMQ and the streaming API, replacing them with direct Kafka publishing using Jackson serialization. Also rebrand by removing all "Talend" references and renaming packages.

## Goals

- Remove ALL JMS/ActiveMQ functionality
- Remove complex streaming REST API
- Publish IDOCs and BW data directly to Kafka
- Use Jackson for JSON serialization
- Remove mock implementations
- Keep health monitoring and BAPI handlers
- **Rename package from `org.talend.sap` to `org.dataingest.rfc.server`**
- **Remove all "talend" references from code, configs, and topic names**

## Architecture Changes

### Current Flow:
```
SAP System → Receiver → BlockingQueue → Publisher Thread → JMS Topics
```

### Target Flow:
```
SAP System → Receiver → Kafka Publisher (synchronous) → Kafka Topics
```

## Implementation Steps

### 1. Create New Kafka Publishers (NEW FILES)

**IDocKafkaPublisher.java** - `org/dataingest/rfc/server/publisher/`
- Inject `Producer<String, String>` and `ObjectMapper`
- Use `IDocTopicNameUtil.getTopicName()` for topic naming
- Serialize IDOCs to JSON with Jackson: `objectMapper.writeValueAsString(idoc)`
- Send synchronously to Kafka: `kafkaProducer.send().get()` with timeout
- Throw exception on failure to trigger SAP transaction rollback
- Topic pattern: `SAP.IDOCS.{TYPE}_{EXTENSION}` (changed from TALEND prefix)

**BWDataKafkaPublisher.java** - `org/dataingest/rfc/server/publisher/`
- Same structure as IDOC publisher
- Use `BWDataTopicNameUtil.getTopicName()`
- Serialize BW data request to JSON
- Topic pattern: `SAP.DATASOURCES.{DATASOURCE_NAME}` (changed from TALEND prefix)

**KafkaPublishException.java** - `org/dataingest/rfc/server/exception/`
- Custom exception for Kafka publish failures
- Used to trigger SAP transaction rollback

### 2. Update Configuration Classes

**ApplicationConfiguration.java** - MAJOR REWRITE
- Remove: `idocQueue`, `requestQueue`, `streamReceiverService` beans
- Add: `ObjectMapper` bean with `JavaTimeModule` configured
- Keep: Clean, minimal configuration

**ApplicationProperties.java** - SIMPLIFY
- Remove: `kafkaConsumerProperties` and consumer filtering logic
- Keep: `kafkaProducerProperties` bean and producer filtering
- Keep: Helper methods for property loading

**ApplicationPropertiesEncryption.java**
- Remove JMS encrypted keys: `jms.ssl.keystore.password`, `jms.login.password`
- Keep: SAP JCo encrypted keys

### 3. Update Factory Implementations

**IDocReceiverFactoryImpl.java** (renamed from SAPIDocReceiverFactoryImpl)
- Remove: `BlockingQueue<ISAPIDocPackage>` injection
- Remove: `createMock()` method
- Add: Inject `IDocKafkaPublisher`
- Update: Pass publisher to receivers instead of queue
- Note: May need adapter pattern if `sap-impl` classes are external
- Package: `org.dataingest.rfc.server.factory`

**BWDataSourceFactoryImpl.java** (renamed from SAPBWSourceSystemFactoryImpl)
- Remove: `BlockingQueue<ISAPBWDataRequest>` injection
- Remove: `createMock()` method
- Add: Inject `BWDataKafkaPublisher`
- Update: Pass publisher to source system
- Package: `org.dataingest.rfc.server.factory`

### 4. Update Named Connection Service

**SAPNamedConnectionServiceImpl.java**
- Remove: `streamReceiverFactory` injection
- Remove: Mock maps and mock startup logic
- Remove: Streaming feature checks
- Update: Feature check to only include `BW_SOURCE_SYSTEM` and `IDOC`

**SAPNamedConnectionFeature.java** (enum)
- Remove: `STREAMING`, `BW_SOURCE_SYSTEM_MOCK`, `IDOC_MOCK` constants
- Keep: `BW_SOURCE_SYSTEM`, `IDOC`

### 5. Remove Files (30 files total)

**JMS Configuration (9 files)** - `configuration/`
- JmsBrokerConfiguration.java
- JmsBrokerCondition.java
- JmsBrokerSslContext.java
- JmsRemoteBrokerConfiguration.java
- JmsRemoteBrokerCondition.java
- JmsReconnect.java
- JmsQueueReconnect.java
- JmsTopicReconnect.java
- JmsReconnectTask.java

**JMS Publishers (2 files)**
- SAPIDocPublisher.java
- SAPBWDataRequestPublisher.java

**Helper (1 file)**
- Holder.java

**Streaming (13 files)**
- controller/SAPStreamController.java
- controller/exception/BadRequestException.java
- controller/exception/NotFoundException.java
- controller/exception/ServiceUnavailableException.java
- SAPStreamReceiverFactory.java (interface)
- SAPStreamReceiverFactoryImpl.java
- KafkaAdmin.java (interface)
- KafkaAdminImpl.java
- KafkaAdminFactory.java (interface)
- KafkaAdminFactoryImpl.java
- KafkaConsumerFactory.java (interface)
- KafkaConsumerFactoryImpl.java
- configuration/ThreadPoolConfiguration.java

**Mocks (2 files)**
- SAPIDocReceiverMock.java
- SAPBWSourceSystemMock.java

**Sample Data (3 files)** - `sample-idocs/`
- 0000000000813429.txt
- 0000000000814496.txt
- 0000000000814490.txt

### 6. Update Maven Dependencies (pom.xml)

**Remove:**
- All ActiveMQ dependencies: `activemq-broker`, `activemq-client`, `activemq-kahadb-store`, `activemq-jaas`, `activemq-openwire-legacy`
- `jakarta.jms:jakarta.jms-api`
- `com.google.code.gson:gson`
- `org.apache.kafka:kafka_2.12` (keep only `kafka-clients`)
- Property: `<activemq.release>5.18.3</activemq.release>`

**Keep:**
- `org.apache.kafka:kafka-clients`
- Jackson libraries (already present)
- Spring Boot (web, actuator)
- SAP libraries (sap-api, sap-impl, sapjco)

### 7. Rename Utility Classes and Update Topic Names

**IDocTopicNameUtil.java** (renamed from SAPIDocNameUtil.java)
- Update topic prefix from `TALEND.IDOCS.` to `SAP.IDOCS.`
- Keep sanitization logic for topic names
- Package: `org.dataingest.rfc.server.util`

**BWDataTopicNameUtil.java** (renamed from SAPBWDataRequestNameUtil.java)
- Update topic prefix from `TALEND.DATASOURCES.` to `SAP.DATASOURCES.`
- Keep sanitization logic for topic names
- Package: `org.dataingest.rfc.server.util`

**BAPI Handlers (keep functionality, rename):**
- BapiBwFunctionExists.java → Package: `org.dataingest.rfc.server.bapi`
- BapiDSourceIsSupported.java → Package: `org.dataingest.rfc.server.bapi`

**Other Configuration (keep, update package):**
- KafkaProducerConfiguration.java → Package: `org.dataingest.rfc.server.config`
- KafkaCondition.java → Package: `org.dataingest.rfc.server.config`

### 8. External Library Considerations

If `SAPIDocReceiver` and `SAPBWSourceSystem` are external (likely from `sap-impl` JAR), create adapters:

**IDocReceiverAdapter.java** (NEW)
- Wraps external receiver class
- Injects `IDocKafkaPublisher`
- On receive: publish to Kafka, then commit/rollback SAP transaction
- Package: `org.dataingest.rfc.server.adapter`

**BWDataSourceAdapter.java** (NEW)
- Wraps external source system class
- Injects `BWDataKafkaPublisher`
- On receive: publish to Kafka
- Package: `org.dataingest.rfc.server.adapter`

Update factories to return adapters instead of direct instances.

### 9. Configuration Updates

**application.properties - Remove:**
- All `jms.*` properties
- `feature.*.mock.enabled` properties
- `feature.streaming.*` properties

**application.properties - Keep:**
- `kafka.*` properties (producer config)
- `jco.*` properties (SAP connection)
- `feature.bw_source_system.enabled`
- `feature.idoc.enabled`
- `feature.idoc.transactional`
- `feature.idoc.transactionAbortTimeout`
- `spring.*` properties

**Recommended Kafka Properties:**
```properties
kafka.bootstrap.servers=localhost:9092
kafka.acks=all
kafka.retries=3
kafka.max.in.flight.requests.per.connection=1
kafka.compression.type=gzip
kafka.enable.idempotence=true
kafka.request.timeout.ms=30000
```

## Critical Files

1. **IDocKafkaPublisher.java** (NEW) - Core IDOC publishing
2. **BWDataKafkaPublisher.java** (NEW) - Core BW publishing
3. **ApplicationConfiguration.java** (MODIFY) - Central config rewrite
4. **IDocReceiverFactoryImpl.java** (MODIFY) - Inject Kafka publisher
5. **pom.xml** (MODIFY) - Remove ActiveMQ/JMS dependencies, update group/artifact IDs
6. **IDocTopicNameUtil.java** (MODIFY) - Change topic prefix from TALEND to SAP
7. **BWDataTopicNameUtil.java** (MODIFY) - Change topic prefix from TALEND to SAP

## Error Handling

- **Kafka Failure:** Throw exception → SAP transaction rollback → SAP retries
- **Serialization Error:** Log error → rollback → alert monitoring
- **Timeout:** Configurable via `kafka.request.timeout.ms`
- **Logging:** INFO for receives, ERROR for failures, DEBUG for publishes

## Testing Strategy

1. Unit tests for new publishers with mocked Kafka producer
2. Integration tests with embedded Kafka
3. Verify SAP transaction rollback on Kafka failure
4. Compare JSON output (Gson vs Jackson) for compatibility
5. Load testing with realistic SAP volume

## Implementation Order

1. **Update pom.xml** - Change groupId, artifactId, and remove ActiveMQ/JMS dependencies
2. **Rename package structure** - Move all files from `org.talend.sap` to `org.dataingest.rfc.server`
3. **Update topic name utilities** - Change TALEND prefix to SAP in topic names
4. **Create new Kafka publisher classes** - IDocKafkaPublisher, BWDataKafkaPublisher
5. **Update configuration classes** - ApplicationConfiguration, ApplicationProperties
6. **Modify factory implementations** - Update to inject Kafka publishers
7. **Create adapters if needed** - For external sap-impl library integration
8. **Update named connection service** - Remove streaming and mock references
9. **Remove obsolete files** - JMS, streaming, mock components (30 files)
10. **Update application properties** - Remove JMS/streaming properties
11. **Update Spring Boot application** - Change app name from "tsap-rfc-server" to "rfc-server"
12. **Test thoroughly** - Unit, integration, and load testing

## Risks & Mitigations

**High Risk:**
- External library dependency → Use adapter pattern
- JSON format changes → Compare outputs, provide migration guide
- Transaction semantics → Synchronous Kafka sends ensure delivery

**Medium Risk:**
- Topic naming compatibility → Document new naming convention
- Performance impact → Configure timeouts, use compression

## Package Structure Changes

### Old Structure:
```
org.talend.sap
├── impl.server
│   ├── configuration
│   ├── controller
│   ├── named
│   └── [various impl classes]
└── server
    └── named
```

### New Structure:
```
org.dataingest.rfc.server
├── config           (configuration classes)
├── publisher        (Kafka publishers)
├── factory          (receiver and data source factories)
├── adapter          (adapters for external libraries)
├── util             (topic naming utilities)
├── bapi             (BAPI function handlers)
├── exception        (custom exceptions)
└── named            (named connection management)
```

## Maven Coordinates Changes

### pom.xml Updates:
- **GroupId**: `org.talend` → `org.dataingest`
- **ArtifactId**: `tsap-rfc-server` → `rfc-server`
- **Name**: `Talend SAP RFC Server` → `RFC Server`
- **Description**: Update to remove Talend references
- **Spring App Name**: `tsap-rfc-server` → `rfc-server`
- **JAR/PID Filename**: `tsap-rfc-server.jar/.pid` → `rfc-server.jar/.pid`

## Topic Naming Changes

### Old Topic Names:
- IDOCs: `TALEND.IDOCS.{TYPE}_{EXTENSION}`
- BW Data: `TALEND.DATASOURCES.{DATASOURCE_NAME}`

### New Topic Names:
- IDOCs: `SAP.IDOCS.{TYPE}_{EXTENSION}`
- BW Data: `SAP.DATASOURCES.{DATASOURCE_NAME}`

## Expected Outcome

- **Remove 60% of codebase** (30 files)
- **Simpler architecture** (no background threads, no queues)
- **Better reliability** (synchronous publishing with SAP transaction semantics)
- **Industry standard** (Jackson, direct Kafka)
- **Easier maintenance** (fewer components, clearer data flow)
- **Rebranded** (no Talend references, neutral naming)
- **Clean package structure** (organized by functionality)
