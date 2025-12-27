# SAP IDoc Capture Server with Kafka Integration

A robust Java application that captures SAP IDocs via RFC, converts them to JSON format, and publishes them to Apache Kafka for downstream processing.

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Installation](#installation)
5. [Configuration](#configuration)
6. [Running the Application](#running-the-application)
7. [Project Structure](#project-structure)
8. [Documentation Setup](#documentation-setup)
9. [Usage Examples](#usage-examples)
10. [Troubleshooting](#troubleshooting)
11. [Performance Tuning](#performance-tuning)

---

## Features

### Core Capabilities

- **RFC/IDoc Server**: Acts as an RFC destination to receive IDocs from SAP
- **XML Capture**: Preserves complete IDoc structure as XML files
- **JSON Conversion**: Converts XML to clean, structured JSON format
- **Kafka Integration**: Publishes IDoc JSON to Kafka topics automatically
- **Documentation-Driven**: Only converts to JSON when IDoc documentation exists
- **Background Monitoring**: Automatically detects new documentation files without restart
- **Configurable Everything**: All settings via external properties file
- **Error Handling**: Comprehensive error handling with detailed logging

### Key Advantages

✅ No code changes needed - just update configuration
✅ Automatic document type detection from received IDocs
✅ JSON conversion only when documentation exists
✅ Background watcher for new documentation
✅ Asynchronous Kafka publishing (non-blocking)
✅ Cross-platform (Windows, Linux, macOS)
✅ Detailed logging for debugging and monitoring

---

## Architecture

```
┌─────────────┐
│ SAP System  │
│  (ERP)      │
└──────┬──────┘
       │ RFC/IDoc
       ↓
┌─────────────────────────────────────┐
│  IDoc Capture Server                │
│  (SimpleIDocCaptureWithConfig)      │
│                                     │
│  ┌─────────────┐  ┌──────────────┐  │
│  │ JCo Server  │  │ TID Handler  │  │
│  └──────┬──────┘  └──────────────┘  │
│         │                           │
│  ┌──────▼───────────────────────┐   │
│  │ IDoc Processing Pipeline     │   │
│  │                              │   │
│  │ 1. Extract IDoc Type         │   │
│  │ 2. Write XML File            │   │
│  │ 3. Check Documentation       │   │
│  │ 4. Convert to JSON           │   │
│  │ 5. Publish to Kafka          │   │
│  └──────┬───────────────────────┘   │
└─────────┼───────────────────────────┘
          │
    ┌─────┴─────┬──────────┬──────────┐
    ↓           ↓          ↓          ↓
 XML Files  JSON Files  Kafka Topics  Logs
```

---

## Prerequisites

### System Requirements

- **Java**: JDK 11 or higher
- **Maven**: 3.6.0 or higher (for building)
- **SAP JCo**: 3.1.0 (Must be downloaded from SAP https://support.sap.com/en/product/connectors/jco.html)
- **Kafka**: 4.1.1 or compatible broker (for publishing)
- **Operating System**: Windows, Linux, or macOS

### SAP System Setup

1. SAP system must be configured to send IDocs to external RFC destination
2. Program ID must be registered in SAP (Transaction SMGW or SM59)
3. IDoc types to be sent must be configured in SAP

### Kafka Setup (Optional)

1. Kafka broker must be running and accessible
2. Topics will be created automatically (if broker allows auto-creation)
3. Or pre-create topics with naming pattern: `{prefix}{idoc_type}`

---

## Installation

### Step 1: Clone/Download the Project

```bash
cd ProjectRFC
```

### Step 2: Download Dependencies

```bash
mvn clean package
```

This will:
- Download all Maven dependencies (Kafka, Gson, etc.)
- Compile the Java source code
- Copy dependencies to `lib/` folder
- Create JAR files in `target/`

### Step 3: Verify Installation

Check that these files exist:
- `lib/sapjco3.jar`
- `lib/sapidoc3.jar`
- `lib/kafka-clients-4.1.1.jar`
- `lib/gson-2.10.1.jar`
- `lib/slf4j-*.jar` (multiple files)

---

## Configuration

### Main Configuration File: `idoc_capture.properties`

#### IDoc Capture Settings

```properties
# Output directory where XML files will be saved
output.directory=/data/IDocs/captured

# IDoc Documentation folder
# Download documentation from SAP (WE60) and save as IDOCTYPE.doc
idoc.documentation.directory=/data/IDocs/documentation

# JSON output directory (leave empty to use same as output.directory)
json.output.directory=/data/IDocs/captured

# Documentation auto-reload interval in seconds (0 = disable)
documentation.reload.interval=600

# JCo Server configuration
jco.server.name=IDOC_SERVER
jco.repository.destination=SAP_SYSTEM

# Logging options
logging.enabled=true
file.use.timestamp=true

# JSON conversion settings
json.conversion.enabled=true
summary.file.enabled=true
summary.file.name=idoc_capture_summary.txt
```

#### Kafka Configuration

```properties
# Enable/disable Kafka publishing
kafka.enabled=true

# Kafka broker address(es) - comma separated for multiple brokers
kafka.bootstrap.servers=localhost:9092

# Topic name prefix (final topic: prefix + IDOC_TYPE)
# Example: idoc_ORDERS05, idoc_INVOIC01
kafka.topic.prefix=idoc_

# Push JSON to Kafka
kafka.push.json=true

# Producer settings
kafka.acks=all              # Acknowledgement level: all, 1, or 0
kafka.retries=3             # Number of retries on failure
kafka.batch.size=16384      # Batch size in bytes
kafka.linger.ms=10          # Wait time in ms before sending
```

### SAP JCo Configuration Files

#### IDOC_SERVER.jcoDestination

**Location**: Same directory as application

**Purpose**: Configures RFC server to receive IDocs

```properties
# SAP Gateway connection
jco.server.gwhost=sapapp1.example.com    # SAP Gateway host
jco.server.gwserv=sapgw10                  # SAP Gateway service
jco.server.progid=RFC_PROGID                   # Program ID (register in SAP)
jco.server.repository_destination=SAP_SYSTEM

# Server settings
jco.server.connection_count=2               # Parallel connections
jco.server.max_startup_delay=0              # Startup timeout
```

#### SAP_SYSTEM.jcoDestination

**Purpose**: Client connection for repository queries

```properties
# Application Server or Message Server
jco.client.ashost=sapapp1.example.com    # AS host
jco.client.sysnr=10                        # System number
jco.client.client=100                      # Client (Mandant)

# Authentication
jco.client.user=RFCUSER                    # RFC user
jco.client.passwd=password                 # Password
jco.client.lang=EN                         # Language
```

---

## Running the Application

### Prerequisites Check

1. **Verify SAP JCo files exist**:
   ```bash
   dir lib\sapjco3.jar
   dir lib\sapidoc3.jar
   ```

2. **Verify configuration files exist**:
   ```bash
   dir IDOC_SERVER.jcoDestination
   dir SAP_SYSTEM.jcoDestination
   dir idoc_capture.properties
   ```

3. **Create output directories**:
   ```bash
   mkdir /data/IDocs/captured
   mkdir /data/IDocs/documentation
   ```

### Method 1: Command Line (Windows)

```bash
cd ProjectRFC

java -cp "target/classes;lib/*" org.dataingest.rfc.server.idoc.SimpleIDocCaptureWithConfig idoc_capture.properties
```

### Method 2: Batch File (Windows)

Create `run.bat`:

```batch
@echo off
REM IDoc Capture Server Launcher
cd /d ProjectRFC
java -cp "target/classes;lib/*" org.dataingest.rfc.server.idoc.SimpleIDocCaptureWithConfig idoc_capture.properties
pause
```

Then run:
```bash
run.bat
```

### Method 3: Shell Script (Linux/macOS)

Create `run.sh`:

```bash
#!/bin/bash
cd "$(dirname "$0")"
java -cp "target/classes:lib/*" org.dataingest.rfc.server.idoc.SimpleIDocCaptureWithConfig idoc_capture.properties
```

Make executable:
```bash
chmod +x run.sh
./run.sh
```

### Verification

Successful startup output:

```
[IDocCapture] Configuration loaded from: idoc_capture.properties
[IDocCapture] Scanning documentation directory: \IDocs\documentation
[IDocCapture] Found 2 files in documentation directory
[IDocCapture]   Loaded documentation for type: ORDERS05 (ORDERS05.txt)
[IDocCapture]   Loaded documentation for type: INVOIC01 (INVOIC01.txt)
[IDocCapture] Documentation scan complete. Found 2 documentation files available
[IDocCapture] Available IDoc types: ORDERS05, INVOIC01
[IDocCapture] Documentation watcher started (reload interval: 600s)
[JCo] Environment initialized successfully
[IDocCapture] Starting IDoc capture server: IDOC_SERVER
[IDocCapture] Output directory: \IDocs\captured
[IDocCapture] Listening for IDocs...
```

---

## Project Structure

```
ProjectRFC\
├── src/main/java/org/dataingest/rfc/server/
│   ├── config/
│   │   ├── IDocCaptureConfig.java              # Configuration manager
│   │   ├── JCoDestinationDataProvider.java     # JCo SPI provider
│   │   └── JCoEnvironmentInitializer.java      # JCo initialization
│   │
│   ├── idoc/
│   │   ├── SimpleIDocCaptureWithConfig.java    # Main application class
│   │   ├── IDocXmlToJsonConverter.java         # XML to JSON converter
│   │   ├── IDocDocumentationManager.java       # Documentation manager
│   │   └── IDocToJsonConverter.java            # (Deprecated)
│   │
│   └── kafka/
│       └── KafkaProducerService.java           # Kafka publisher
│
├── lib/                                         # Third-party JARs
│   ├── sapjco3.jar                            # SAP JCo
│   ├── sapidoc3.jar                           # SAP IDoc
│   ├── kafka-clients-4.1.1.jar                # Kafka client
│   ├── gson-2.10.1.jar                        # JSON library
│   └── slf4j-*.jar                            # Logging library
│
├── target/                                      # Maven build output
│   ├── classes/                               # Compiled classes
│   └── rfc-server-*.jar                       # JAR files
│
├── IDOC_SERVER.jcoDestination                 # RFC server config
├── SAP_SYSTEM.jcoDestination                  # Client connection config
├── idoc_capture.properties                    # Main configuration
├── pom.xml                                     # Maven build file
├── README.md                                   # This file
└── TESTING_GUIDE.md                           # Testing instructions
```

---

## Documentation Setup

### Downloading IDoc Documentation from SAP

1. **In SAP, go to Transaction WE60**
2. **Search for your IDoc type**
   - Example: ORDERS05, INVOIC01
3. **Click "Display IDoc Documentation"**
4. **Select Format**: Choose appropriate format
5. **Download**: Save to file with naming convention:
   ```
   /data/IDocs/documentation/{IDOCTYPE}.txt
   /data/IDocs/documentation/{IDOCTYPE}.doc
   ```

### Examples

```
ORDERS05.txt         → Documentation for ORDERS05 IDoc type
INVOIC01.doc         → Documentation for INVOIC01 IDoc type
```

### Documentation Auto-Update

Once documentation is placed in the folder:

1. Application scans on startup
2. Background watcher rescans every 600 seconds (configurable)
3. New documentation is automatically available
4. **No application restart needed!**

---

## Usage Examples

### Example 1: Send ORDERS IDoc from SAP

1. **Ensure configuration is set up**
2. **Start the application**: `run.bat`
3. **In SAP, send IDoc**: ORDERS05 via WE19 or standard process
4. **Check output**:

```
[IDocCapture] Extracted IDoc type from XML: ORDERS05
[IDocCapture] IDoc captured: IDOC_20251227_175206_ABC123.xml + IDOC_20251227_175206_ABC123.json [Type: ORDERS05] (Total: 1)
[IDocCapture] Published to Kafka - Topic: idoc_orders05, Partition: 0, Offset: 42
```

### Example 2: View Generated JSON

**File**: `/data/IDocs/captured/IDOC_20251227_175206_ABC123.json`

```json
{
  "control": {
    "DOCNUM": "0000000001955146",
    "MESTYP": "ORDERS05",
    "SNDPRN": "HD1CLNT100",
    "RCVPRN": "RFCPROG",
    "CREDAT": "20251227",
    "CRETIM": "175206",
    "STATUS": "03"
  },
  "segments": [
    {
      "segment_name": "E1EDK01",
      "segment_number": "000001",
      "parent_segment": "000000",
      "hierarchy_level": "02",
      "fields": {
        "ACTION": "004",
        "CURCY": "USD",
        "WKURS": "123456.123",
        "ZTERM": "X400"
      }
    }
  ]
}
```

### Example 3: Consume from Kafka

**Kafka Consumer Command**:

```bash
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic idoc_orders05 \
  --from-beginning \
  --property print.key=true
```

**Output**:
```
0000000001955146 {"control": {...}, "segments": [...]}
```

---

## Troubleshooting

### Issue: "JCo_ERROR_RESOURCE: Server IDOC_SERVER does not exist"

**Cause**: `.jcoDestination` files not found in working directory

**Solution**:
1. Ensure `IDOC_SERVER.jcoDestination` exists in project root
2. Run application from `ProjectRFC`
3. Verify file permissions are readable

### Issue: "NoClassDefFoundError: org/apache/kafka/clients/producer/KafkaProducer"

**Cause**: Kafka libraries not on classpath

**Solution**:
```bash
mvn clean package
# This downloads and copies all dependencies to lib/
```

### Issue: "ClassNotFoundException: com.sap.conn.idoc.IDocException"

**Cause**: SAP JCo JAR files missing

**Solution**:
1. Verify `lib/sapjco3.jar` exists
2. Verify `lib/sapidoc3.jar` exists
3. Check classpath includes `lib/*`

### Issue: IDoc received but JSON not created

**Cause**: Documentation not available for IDoc type

**Solution**:
1. Download documentation from SAP WE60
2. Save to: `/data/IDocs/documentation/{IDOCTYPE}.txt`
3. Wait for auto-scan (max 600 seconds) or restart
4. Verify in logs: `Available IDoc types: ...`

### Issue: Cannot connect to SAP Gateway

**Cause**: SAP JCo configuration incorrect

**Solution**:
1. Verify `IDOC_SERVER.jcoDestination` settings:
   - `jco.server.gwhost`: Correct SAP Gateway host
   - `jco.server.gwserv`: Correct Gateway service (sapgw##)
   - `jco.server.progid`: Matches SAP registration
2. Verify SAP network connectivity
3. Check SAP logs for registration

### Issue: Kafka publishing fails

**Cause**: Kafka broker unreachable

**Solution**:
1. Verify Kafka broker is running
2. Check `kafka.bootstrap.servers` in config
3. Test connectivity:
   ```bash
   telnet localhost 9092
   ```
4. Check Kafka broker logs
5. If necessary, disable Kafka temporarily:
   ```properties
   kafka.enabled=false
   ```

---

## Performance Tuning

### For High Volume IDocs

```properties
# Increase documentation reload interval to reduce scan overhead
documentation.reload.interval=3600

# Kafka producer optimization
kafka.batch.size=32768          # Larger batch
kafka.linger.ms=100             # More time to batch
kafka.acks=1                    # Faster but less safe

# JCo server connections
# Edit IDOC_SERVER.jcoDestination:
jco.server.connection_count=5   # More parallel connections
```

### For Low Latency

```properties
# Reduce delays
documentation.reload.interval=60
kafka.linger.ms=0              # Send immediately

# Kafka settings
kafka.acks=all                 # Wait for all replicas
kafka.batch.size=8192          # Smaller batches
```

### Monitoring

Check file system for output:
```bash
dir /data/IDocs/captured /O:D   # List by date
```

Monitor Kafka:
```bash
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic idoc_orders05 \
  --from-beginning
```

---

## Support and Troubleshooting

### Logs Location

All logs are printed to console. To save logs to file:

**Windows**:
```bash
run.bat > idoc_server.log 2>&1
```

**Linux/macOS**:
```bash
./run.sh > idoc_server.log 2>&1
```

### Common Errors and Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| Port already in use | Another RFC server on same port | Change program ID and register in SAP |
| OutOfMemory | Large IDocs | Increase JVM heap: `java -Xmx2048m ...` |
| Slow performance | Documentation scanning | Increase `documentation.reload.interval` |
| Kafka connection timeout | Broker unreachable | Check network and broker status |

---

## Version Information

- **Java Version**: 11 or higher
- **Maven Version**: 3.6.0 or higher
- **SAP JCo**: 3.1.0
- **Kafka Client**: 4.1.1
- **Gson**: 2.10.1
- **SLF4J**: 1.7.36

---

## License

This project is configured for SAP connectivity and requires valid SAP licenses.

---

## Quick Start Checklist

- [ ] Java 11+ installed
- [ ] Maven installed
- [ ] Project cloned/downloaded
- [ ] Run `mvn clean package`
- [ ] SAP JCo files present in `lib/`
- [ ] Create directories for captured IDocs
- [ ] Download IDoc documentation from SAP
- [ ] Configure `idoc_capture.properties`
- [ ] Configure `.jcoDestination` files
- [ ] Register program ID in SAP
- [ ] Run application
- [ ] Send test IDoc from SAP
- [ ] Verify XML and JSON files created
- [ ] Check Kafka topics (if enabled)

---

## Next Steps

1. **Test with sample IDoc**: Use SAP WE19 to send test IDoc
2. **Monitor output**: Check XML and JSON files created
3. **Configure Kafka**: If needed for downstream processing
4. **Set up documentation**: Download documentation for your IDoc types
5. **Implement consumers**: Create applications to process JSON from Kafka

---

**Last Updated**: December 27, 2025
**Project**: RFC Server - IDoc Capture with Kafka Integration
