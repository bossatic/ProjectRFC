# RFC Server - Testing Starting Point Guide

## 🎯 Where to Start Testing This Project

This guide provides clear entry points for testing the RFC Server project using Eclipse IDE, after all Talend packages have been removed and replaced with official SAP libraries (JCo 3.0 and IDOC 3.0).

---

## Quick Summary of Changes

### ✅ What Was Updated

1. **pom.xml** - Added SAP JCo and IDOC library dependencies
2. **All Talend imports removed** - Replaced with official SAP packages
3. **New Model Classes Created:**
   - `SAPIDOCDocument.java` - Represents SAP IDOC documents
   - `SAPBWDataRequest.java` - Represents SAP BW data requests
4. **New RFC Server Implementation:**
   - `SAPRFCServerImpl.java` - RFC server using SAP JCo
5. **Publishers Updated:**
   - `IDocKafkaPublisher.java` - Uses `SAPIDOCDocument`
   - `BWDataKafkaPublisher.java` - Uses `SAPBWDataRequest`

---

## Testing Entry Points

### **Entry Point 1: Project Setup & Import** (30 minutes)
**When:** First time setup
**Where:** `ECLIPSE_IMPORT_GUIDE.md`

```bash
1. Import project into Eclipse
2. Verify Maven dependencies download
3. Check build succeeds: Ctrl+B
4. Verify no compilation errors
```

**Status Check:**
```
✓ Project imports without errors
✓ All SAP libraries resolved (sapjco3.jar, sapidoc3.jar)
✓ Build succeeds
✓ No missing dependencies
```

---

### **Entry Point 2: Application Startup Test** (5 minutes)
**When:** After successful build
**Where:** Eclipse or Terminal

**Test #1: Spring Boot Startup**
```bash
# Option A: Eclipse
Right-click project → Run As → Spring Boot App

# Option B: Terminal
cd D:\RFC_SERVER\ProjectRFC
mvn spring-boot:run
```

**Expected Output:**
```
...
Started RFCServerApplication in X.XXX seconds
...
```

**Verify:**
- [ ] No startup errors
- [ ] No missing bean errors
- [ ] Application listening on port 8080

**Test #2: Health Check Endpoint**
```bash
# In browser or terminal
curl http://localhost:8080/rfc/actuator/health

# Expected response:
{"status":"UP"}
```

---

### **Entry Point 3: Unit Tests** (30 minutes - 2 hours)
**When:** After application starts successfully
**Where:** `src/test/java/`

**Step 1: Create Test for IDocKafkaPublisher**

File: `src/test/java/org/dataingest/rfc/server/publisher/IDocKafkaPublisherTest.java`

```java
package org.dataingest.rfc.server.publisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.dataingest.rfc.server.model.SAPIDOCDocument;

@ExtendWith(MockitoExtension.class)
public class IDocKafkaPublisherTest {

    @Mock
    private Producer<String, String> kafkaProducer;

    private ObjectMapper objectMapper;
    private IDocKafkaPublisher publisher;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new IDocKafkaPublisher();
        publisher.kafkaProducer = kafkaProducer;
        publisher.objectMapper = objectMapper;
    }

    @Test
    public void testPublishSAPDocument() throws Exception {
        // Create test IDOC document
        SAPIDOCDocument doc = new SAPIDOCDocument();
        doc.setDocumentNumber("0000000001");
        doc.setMessageType("ORDERS");
        doc.setMessageTypeVersion("05");
        doc.setSenderSystem("SAP_DEV");
        doc.setReceiverSystem("KAFKA");
        doc.addSegment("EDI_DC40,001,001,E2EDK01505");

        // Verify topic name
        assertEquals("SAP.IDOCS.ORDERS_05", doc.getTopicName());

        // Test serialization
        String json = objectMapper.writeValueAsString(doc);
        assertTrue(json.contains("ORDERS"));
        assertTrue(json.contains("0000000001"));
    }
}
```

**Step 2: Run Tests**
```bash
# Eclipse
Right-click project → Run As → Maven test

# Or Terminal
mvn test
```

**Expected Result:**
```
Tests run: 1, Failures: 0, Errors: 0
BUILD SUCCESS
```

---

### **Entry Point 4: Model Class Tests** (15 minutes)
**When:** To verify SAP model classes work correctly
**Where:** `src/test/java/org/dataingest/rfc/server/model/`

**Test #1: SAPIDOCDocument Model**
```java
@Test
public void testSAPIDOCDocumentModel() {
    SAPIDOCDocument doc = new SAPIDOCDocument();
    doc.setDocumentNumber("DOC123");
    doc.setMessageType("INVOIC");
    doc.setMessageTypeVersion("01");

    // Verify topic generation
    String topic = doc.getTopicName();
    assertEquals("SAP.IDOCS.INVOIC_01", topic);

    // Verify segment handling
    doc.addSegment("SEGMENT_DATA_1");
    doc.addSegment("SEGMENT_DATA_2");
    assertEquals(2, doc.getSegmentData().size());
}
```

**Test #2: SAPBWDataRequest Model**
```java
@Test
public void testSAPBWDataRequestModel() {
    SAPBWDataRequest req = new SAPBWDataRequest();
    req.setRequestId("REQ001");
    req.setDataSourceName("0MATERIAL_ATTR");

    // Verify topic generation
    String topic = req.getTopicName();
    assertEquals("SAP.DATASOURCES.0MATERIAL_ATTR", topic);
}
```

---

### **Entry Point 5: Utility Tests** (15 minutes)
**When:** To verify topic name generation
**Where:** `src/test/java/org/dataingest/rfc/server/util/`

**Test: IDocTopicNameUtil**
```java
@Test
public void testTopicNameGeneration() {
    // Test 1: Basic topic name
    String topic1 = IDocTopicNameUtil.getTopicName("ORDERS", "05");
    assertEquals("SAP.IDOCS.ORDERS_05", topic1);

    // Test 2: With SAP document
    SAPIDOCDocument doc = new SAPIDOCDocument();
    doc.setMessageType("INVOIC");
    doc.setMessageTypeVersion("01");
    String topic2 = IDocTopicNameUtil.getTopicName(doc);
    assertEquals("SAP.IDOCS.INVOIC_01", topic2);

    // Test 3: Special characters sanitization
    String topic3 = IDocTopicNameUtil.getTopicName("ORDER@#$", "05");
    assertTrue(topic3.matches("^SAP\\.IDOCS\\.[A-Z0-9_]+$"));
}
```

---

### **Entry Point 6: SAP RFC Server Configuration** (30 minutes)
**When:** Ready to configure SAP connectivity
**Where:** `src/main/resources/application.properties`

**Configuration Steps:**

1. **Add SAP RFC Server Configuration:**
```properties
# SAP RFC Server Configuration (JCo)
sap.jco.server.enabled=true
sap.jco.server.name=KAFKA_RFC_SERVER
sap.jco.server.gwhost=localhost          # SAP Gateway Host
sap.jco.server.gwserv=3300               # SAP Gateway Service (usually 3300)
sap.jco.server.progid=KAFKA_RFC          # Program ID
sap.jco.server.connection.count=2        # Connection pool size

# Kafka configuration (should already exist)
kafka.bootstrap.servers=localhost:9092
kafka.acks=all
kafka.retries=3
```

2. **Verify SAP Connectivity:**
- SAP Gateway must be accessible from application server
- Gateway host and service port must be correct
- Program ID must be registered in SAP (transaction SM59)

**Test Connection:**
```bash
# Check if SAP RFC server starts
mvn spring-boot:run

# Look for in logs:
# INFO SAPRFCServerImpl: Starting SAP RFC Server
# INFO SAPRFCServerImpl: SAP RFC Server started successfully
```

---

### **Entry Point 7: Integration Tests** (1-2 hours)
**When:** To test end-to-end IDOC publishing
**Where:** `src/test/java/org/dataingest/rfc/server/integration/`

**Test: Kafka Integration with Embedded Kafka**
```java
@SpringBootTest
@Testcontainers
public class KafkaIDOCIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @Autowired
    private IDocKafkaPublisher publisher;

    @Test
    public void testPublishIDOCToKafka() throws Exception {
        // Create IDOC document
        SAPIDOCDocument doc = new SAPIDOCDocument();
        doc.setDocumentNumber("DOC001");
        doc.setMessageType("ORDERS");
        doc.setMessageTypeVersion("05");
        doc.setSenderSystem("SAP");
        doc.addSegment("SEGMENT_DATA");

        // Publish to Kafka
        assertDoesNotThrow(() -> publisher.publishSAPDocument(doc));
    }
}
```

**Run Integration Tests:**
```bash
mvn verify
```

---

## Complete Testing Workflow

### **Day 1: Setup & Basic Testing** (1-2 hours)

1. ✅ Import project into Eclipse (`ECLIPSE_IMPORT_GUIDE.md`)
2. ✅ Build project successfully (Ctrl+B)
3. ✅ Start application (`mvn spring-boot:run`)
4. ✅ Verify health endpoint
5. ✅ Stop application

**Result:** Application runs without errors

---

### **Day 2: Unit Testing** (2-4 hours)

1. ✅ Create tests for SAPIDOCDocument model
2. ✅ Create tests for SAPBWDataRequest model
3. ✅ Create tests for IDocTopicNameUtil
4. ✅ Create tests for BWDataTopicNameUtil
5. ✅ Run all unit tests (`mvn test`)

**Result:** 80%+ code coverage

---

### **Day 3: Integration Testing** (2-4 hours)

1. ✅ Create integration test for Kafka publishing
2. ✅ Setup embedded Kafka (TestContainers)
3. ✅ Test IDOC publishing to Kafka
4. ✅ Test BW data publishing to Kafka
5. ✅ Run integration tests (`mvn verify`)

**Result:** End-to-end workflows validated

---

### **Day 4: SAP Connectivity** (4-8 hours)

1. ✅ Configure SAP RFC Server in application.properties
2. ✅ Test SAP Gateway connectivity
3. ✅ Register RFC Program ID in SAP (SM59)
4. ✅ Configure Kafka broker connectivity from SAP system
5. ✅ Send test IDOC from SAP
6. ✅ Verify message appears in Kafka topic

**Result:** SAP integration working

---

## Quick Test Commands

### Build & Clean
```bash
mvn clean package              # Clean build
mvn clean install              # Install locally
mvn package -DskipTests        # Skip tests
```

### Run Tests
```bash
mvn test                       # Run unit tests
mvn verify                     # Run all tests including integration
mvn test -Dtest=TestClass     # Run specific test
mvn test -Dgroups=@Tag        # Run tests with tag
```

### Code Quality
```bash
mvn jacoco:report              # Generate coverage report
# Report: target/site/jacoco/index.html
```

### Run Application
```bash
mvn spring-boot:run            # Run Spring Boot app
mvn clean package && java -jar target/rfc-server-1.0.0.jar
```

---

## File Structure for Tests

```
src/test/
├── java/org/dataingest/rfc/server/
│   ├── model/
│   │   ├── SAPIDOCDocumentTest.java
│   │   └── SAPBWDataRequestTest.java
│   ├── publisher/
│   │   ├── IDocKafkaPublisherTest.java
│   │   └── BWDataKafkaPublisherTest.java
│   ├── util/
│   │   ├── IDocTopicNameUtilTest.java
│   │   └── BWDataTopicNameUtilTest.java
│   └── integration/
│       ├── KafkaIDOCIntegrationTest.java
│       └── KafkaBWIntegrationTest.java
└── resources/
    ├── application-test.properties
    └── fixtures/
        ├── idoc-sample.json
        └── bw-data-sample.json
```

---

## Troubleshooting

### Error: "Cannot find sapjco3.jar"
```
Solution:
1. Verify D:\RFC_SERVER\SapLibs\SapJCO\sapjco3.jar exists
2. Run: mvn clean install -U (force update)
3. Check .classpath configuration
```

### Error: "SAP RFC Server initialization failed"
```
Solution:
1. Check sap.jco.server.enabled=true in application.properties
2. Verify SAP Gateway is accessible
3. Check SAP Gateway host/port are correct
```

### Error: "No such file or directory: application.properties"
```
Solution:
1. Verify file exists: src/main/resources/application.properties
2. Run Maven build to copy resources: mvn package
3. Restart application
```

### Tests Fail: "Port 8080 already in use"
```
Solution:
1. Kill existing process: lsof -i :8080 (macOS/Linux)
2. Or change port: server.port=8081 in application-test.properties
3. Or increase timeout: @TestPropertySource with custom port
```

---

## Next Steps After Testing

1. **Deploy to Test Environment**
   - Build JAR: `mvn clean package`
   - Configure properties for test SAP system
   - Start application: `java -jar rfc-server-1.0.0.jar`

2. **Configure SAP System**
   - Register RFC Program ID (SM59)
   - Configure Kafka broker connectivity
   - Create test IDOCs

3. **Monitor in Production**
   - Check health endpoint: `/rfc/actuator/health`
   - Monitor logs for errors
   - Track Kafka topics for published messages

---

## Documentation References

- **ECLIPSE_IMPORT_GUIDE.md** - Import project steps
- **ECLIPSE_SETUP_GUIDE.md** - Eclipse configuration details
- **TESTING_GUIDE_ECLIPSE.md** - Comprehensive testing guide
- **IMPLEMENTATION_GUIDE.md** - Architecture overview
- **QUICK_START_CHECKLIST.md** - Quick reference

---

## Summary

**Testing starts here:**

1. Import project → Build succeeds ✓
2. Run application → Starts without errors ✓
3. Write unit tests → All pass ✓
4. Integration tests → End-to-end works ✓
5. SAP connectivity → IDOCs published to Kafka ✓

**You're ready to test! Start with:** `ECLIPSE_IMPORT_GUIDE.md`

---

**Last Updated:** 2024-12-18
**Version:** 1.0.0
**Status:** Ready for Testing with Official SAP Libraries

