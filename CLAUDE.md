# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

### Build
```bash
mvn clean package
```
This compiles Java source, copies dependencies to `lib/`, and creates JAR files in `target/`.

### Run Application (Windows)
```bash
java -cp "target/classes;lib/*" org.dataingest.rfc.server.idoc.SimpleIDocCaptureWithConfig idoc_capture.properties
```

### Run Application (Linux/macOS)
```bash
java -cp "target/classes:lib/*" org.dataingest.rfc.server.idoc.SimpleIDocCaptureWithConfig idoc_capture.properties
```

## High-Level Architecture

### Purpose
SAP IDoc Capture Server that receives IDocs via RFC protocol, converts them to JSON, and publishes to Apache Kafka.

### Core Data Flow
```
SAP System → RFC/IDoc Server → XML Capture → JSON Conversion → Kafka Publishing
```

### Main Components

#### 1. Entry Point & IDoc Reception
- **SimpleIDocCaptureWithConfig.java**: Main application class
  - Initializes JCo RFC server to receive IDocs from SAP
  - Uses nested handler classes: `MyIDocHandlerFactory`, `MyIDocReceiveHandler`, `MyTidHandler`
  - Extracts IDoc type and document number from received XML
  - Orchestrates the conversion and publishing pipeline

#### 2. Configuration Layer (`config/`)
- **IDocCaptureConfig.java**: Centralized configuration manager
  - Loads all settings from `idoc_capture.properties`
  - Manages output directories, JCo settings, Kafka settings
  - Provides logging and error handling utilities

- **JCoEnvironmentInitializer.java**: Initializes SAP JCo environment
  - Registers custom destination data provider
  - Must be called before any JCo operations

- **JCoDestinationDataProvider.java**: Custom SPI for JCo configuration
  - Enables loading SAP connection settings from `.jcoDestination` files
  - These files MUST be in the application's working directory

#### 3. Conversion Layer (`idoc/`)
- **IDocXmlToJsonConverter.java**: Primary XML-to-JSON converter
  - Parses IDoc XML structure and creates clean JSON format
  - Groups segments by type with hierarchical structure
  - Handles control segment (EDI_DC40) and data segments separately
  - Produces JSON with "control" and "segments" sections

- **IDocDocumentationManager.java**: (Currently commented out in main flow)
  - Scans for IDoc documentation files in configured directory
  - Caches available documentation types
  - Background watcher rescans at configured intervals
  - Documentation was originally used to validate conversion but now conversion is automatic

#### 4. Kafka Integration (`kafka/`)
- **KafkaProducerService.java**: Asynchronous Kafka publisher
  - Initializes Kafka producer with configuration from properties
  - Publishes JSON to topics with pattern: `{prefix}{idoc_type}` (e.g., `idoc_orders05`)
  - Uses document number (DOCNUM) as Kafka message key
  - Non-blocking async publishing in separate threads

### Key SAP JCo Integration Points

The application uses SAP JCo libraries for RFC/IDoc connectivity:
- `sapjco3.jar` and `sapidoc3.jar` are system-scoped dependencies
- These JAR filenames MUST remain exactly as-is (SAP validates filenames)
- Located in `lib/` directory with absolute path references in `pom.xml`
- Configuration files (`*.jcoDestination`) must be in working directory

### Critical File Dependencies

1. **Runtime Configuration**: `idoc_capture.properties`
   - Main application configuration (paths, JCo names, Kafka settings)

2. **SAP JCo Connection Files** (in working directory):
   - `IDOC_SERVER.jcoDestination`: RFC server configuration (gateway host, program ID)
   - `SAP_SYSTEM.jcoDestination`: Client connection for metadata repository

3. **Output Directories** (configured in properties):
   - XML output directory (captured IDocs)
   - JSON output directory (converted JSON)
   - Documentation directory (IDoc structure definitions)

### JSON Output Structure

The converter produces JSON with this structure:
```json
{
  "control": {
    "DOCNUM": "...",
    "MESTYP": "...",
    ...
  },
  "segments": {
    "SEGMENT_NAME": {
      "fields": [...],
      "count": N
    }
  }
}
```

### Asynchronous Processing Pattern

The application uses async processing in two key areas:
1. **JSON Conversion**: Spawned in separate thread after XML is written (SimpleIDocCaptureWithConfig:118)
2. **Kafka Publishing**: Runs in dedicated thread pool for non-blocking sends (KafkaProducerService:64)

This ensures IDoc reception doesn't block on downstream processing.

### Important Implementation Notes

- The documentation manager system exists but is currently disabled in the main flow (comments at lines 38, 46 in SimpleIDocCaptureWithConfig)
- JSON conversion now happens automatically for all IDocs regardless of documentation availability
- IDoc type extraction happens by parsing the XML file directly (not from IDoc metadata)
- Error handling allows partial success (e.g., XML saved even if JSON conversion fails)
- All file I/O uses UTF-8 encoding explicitly
