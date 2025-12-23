# RFC Server - Project Structure Template for Eclipse

## Complete Project Directory Layout

```
D:\RFC_SERVER\workspace\
└── rfc-server/                              (Eclipse project root)
    ├── .classpath                           (Eclipse classpath configuration)
    ├── .project                             (Eclipse project definition)
    ├── .settings/                           (Eclipse IDE settings)
    │   ├── org.eclipse.core.resources.prefs
    │   ├── org.eclipse.jdt.core.prefs
    │   └── org.eclipse.m2e.core.prefs
    │
    ├── pom.xml                              (Maven configuration - DO NOT DELETE)
    │
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── org/dataingest/rfc/server/
    │   │   │       ├── RFCServerApplication.java         (Main Spring Boot class)
    │   │   │       │
    │   │   │       ├── config/                          (Configuration classes)
    │   │   │       │   ├── ApplicationConfiguration.java
    │   │   │       │   └── WebConfiguration.java
    │   │   │       │
    │   │   │       ├── exception/                       (Custom exceptions)
    │   │   │       │   ├── KafkaPublishException.java
    │   │   │       │   └── InvalidIDocException.java
    │   │   │       │
    │   │   │       ├── publisher/                       (Kafka publishers)
    │   │   │       │   ├── IDocKafkaPublisher.java
    │   │   │       │   └── BWDataKafkaPublisher.java
    │   │   │       │
    │   │   │       ├── util/                            (Utility classes)
    │   │   │       │   ├── IDocTopicNameUtil.java
    │   │   │       │   ├── BWDataTopicNameUtil.java
    │   │   │       │   └── JsonHelper.java
    │   │   │       │
    │   │   │       ├── factory/                         (Factory implementations)
    │   │   │       │   ├── IDocReceiverFactoryImpl.java
    │   │   │       │   └── BWDataSourceFactoryImpl.java
    │   │   │       │
    │   │   │       ├── adapter/                         (Adapter classes)
    │   │   │       │   ├── IDocReceiverAdapter.java
    │   │   │       │   └── BWDataSourceAdapter.java
    │   │   │       │
    │   │   │       ├── model/                           (Data models - optional)
    │   │   │       │   ├── IDocMessage.java
    │   │   │       │   └── BWDataRequest.java
    │   │   │       │
    │   │   │       ├── service/                         (Business logic - optional)
    │   │   │       │   ├── IDocService.java
    │   │   │       │   └── BWDataService.java
    │   │   │       │
    │   │   │       ├── controller/                      (REST endpoints - optional)
    │   │   │       │   ├── HealthController.java
    │   │   │       │   └── InfoController.java
    │   │   │       │
    │   │   │       └── named/                           (Named Connection Service)
    │   │   │           └── NamedConnectionService.java
    │   │   │
    │   │   └── resources/
    │   │       ├── application.properties               (Main configuration)
    │   │       ├── application-dev.properties           (Development profile)
    │   │       ├── application-prod.properties          (Production profile)
    │   │       ├── logback-spring.xml                   (Logging configuration)
    │   │       └── data/                                (Sample data - optional)
    │   │           └── sample-idoc.json
    │   │
    │   └── test/
    │       ├── java/
    │       │   └── org/dataingest/rfc/server/
    │       │       ├── publisher/
    │       │       │   ├── IDocKafkaPublisherTest.java
    │       │       │   └── BWDataKafkaPublisherTest.java
    │       │       │
    │       │       ├── util/
    │       │       │   ├── IDocTopicNameUtilTest.java
    │       │       │   └── BWDataTopicNameUtilTest.java
    │       │       │
    │       │       ├── config/
    │       │       │   └── ApplicationConfigurationTest.java
    │       │       │
    │       │       └── integration/
    │       │           ├── KafkaIntegrationTest.java
    │       │           └── EndToEndTest.java
    │       │
    │       └── resources/
    │           ├── application-test.properties          (Test configuration)
    │           ├── logback-test.xml                     (Test logging)
    │           └── fixtures/                            (Test data)
    │               ├── idoc-sample.json
    │               └── bw-data-sample.json
    │
    ├── target/                                 (Maven build output - DO NOT EDIT)
    │   ├── classes/                           (Compiled classes)
    │   ├── test-classes/                      (Compiled test classes)
    │   ├── rfc-server-1.0.0.jar              (Generated JAR)
    │   └── site/                              (Generated reports)
    │
    ├── .git/                                   (Git configuration - if using Git)
    │   └── hooks/
    │       └── pre-commit                      (Git pre-commit hook)
    │
    └── Documentation/
        ├── IMPLEMENTATION_GUIDE.md             (Refactoring guide)
        ├── REMAINING_TASKS.md                  (TODO list)
        ├── ECLIPSE_SETUP_GUIDE.md              (This guide)
        ├── TESTING_GUIDE_ECLIPSE.md            (Testing procedures)
        ├── QUICK_START_CHECKLIST.md            (Quick start)
        ├── PROJECT_STRUCTURE_TEMPLATE.md       (This file)
        ├── README.md                           (Project overview)
        └── DEPLOYMENT_GUIDE.md                 (Deployment procedures)
```

---

## Directory Structure Creation in Eclipse

### Step 1: Create Main Source Packages

```
In Eclipse, right-click src/main/java → New → Package

1. org.dataingest.rfc.server
2. org.dataingest.rfc.server.config
3. org.dataingest.rfc.server.exception
4. org.dataingest.rfc.server.publisher
5. org.dataingest.rfc.server.util
6. org.dataingest.rfc.server.factory
7. org.dataingest.rfc.server.adapter
8. org.dataingest.rfc.server.model
9. org.dataingest.rfc.server.service
10. org.dataingest.rfc.server.controller
11. org.dataingest.rfc.server.named
```

### Step 2: Create Test Packages

```
In Eclipse, right-click src/test/java → New → Package

1. org.dataingest.rfc.server.publisher
2. org.dataingest.rfc.server.util
3. org.dataingest.rfc.server.config
4. org.dataingest.rfc.server.integration
```

### Step 3: Create Resources Directories

```
In Eclipse, right-click src/main/resources → New → Folder

1. data (for sample data files)
```

```
In Eclipse, right-click src/test/resources → New → Folder

1. fixtures (for test data)
```

---

## Key Files Explanation

### Essential Files (Must Have)

| File | Purpose | Location |
|------|---------|----------|
| `pom.xml` | Maven configuration, dependencies | Project root |
| `RFCServerApplication.java` | Spring Boot main class | `src/main/java/org/dataingest/rfc/server/` |
| `application.properties` | Application configuration | `src/main/resources/` |
| `ApplicationConfiguration.java` | Spring bean configuration | `src/main/java/org/dataingest/rfc/server/config/` |

### Core Feature Files (Required for Functionality)

| File | Purpose | Location |
|------|---------|----------|
| `IDocKafkaPublisher.java` | IDOC publishing to Kafka | `src/main/java/org/dataingest/rfc/server/publisher/` |
| `BWDataKafkaPublisher.java` | BW data publishing to Kafka | `src/main/java/org/dataingest/rfc/server/publisher/` |
| `IDocTopicNameUtil.java` | IDOC topic naming | `src/main/java/org/dataingest/rfc/server/util/` |
| `BWDataTopicNameUtil.java` | BW topic naming | `src/main/java/org/dataingest/rfc/server/util/` |
| `KafkaPublishException.java` | Custom exception | `src/main/java/org/dataingest/rfc/server/exception/` |

### Adapter/Factory Files (For SAP Integration)

| File | Purpose | Location |
|------|---------|----------|
| `IDocReceiverAdapter.java` | Wraps IDOC receiver | `src/main/java/org/dataingest/rfc/server/adapter/` |
| `BWDataSourceAdapter.java` | Wraps BW source system | `src/main/java/org/dataingest/rfc/server/adapter/` |
| `IDocReceiverFactoryImpl.java` | Creates IDOC receivers | `src/main/java/org/dataingest/rfc/server/factory/` |
| `BWDataSourceFactoryImpl.java` | Creates BW sources | `src/main/java/org/dataingest/rfc/server/factory/` |

### Test Files (For Quality Assurance)

| File | Purpose | Location |
|------|---------|----------|
| `IDocKafkaPublisherTest.java` | IDOC publisher tests | `src/test/java/org/dataingest/rfc/server/publisher/` |
| `KafkaIntegrationTest.java` | Kafka integration tests | `src/test/java/org/dataingest/rfc/server/integration/` |
| `application-test.properties` | Test configuration | `src/test/resources/` |

### Configuration Files

| File | Purpose | Location |
|------|---------|----------|
| `application.properties` | Production config | `src/main/resources/` |
| `application-dev.properties` | Development config | `src/main/resources/` |
| `application-prod.properties` | Production-specific config | `src/main/resources/` |
| `logback-spring.xml` | Logging configuration | `src/main/resources/` |

---

## Package Organization Rationale

### `config/`
Contains Spring configuration classes:
- `ApplicationConfiguration.java` - Bean definitions
- `WebConfiguration.java` - Web-related config
- Environment-specific properties files

### `exception/`
Custom exception classes:
- `KafkaPublishException.java` - Kafka publishing errors
- `InvalidIDocException.java` - IDOC validation errors

### `publisher/`
Kafka publisher implementations:
- `IDocKafkaPublisher.java` - IDOC publisher
- `BWDataKafkaPublisher.java` - BW data publisher
- Independent, testable classes

### `util/`
Utility classes used across application:
- `IDocTopicNameUtil.java` - IDOC topic naming logic
- `BWDataTopicNameUtil.java` - BW topic naming logic
- `JsonHelper.java` - JSON serialization helpers

### `factory/`
Factory pattern implementations:
- `IDocReceiverFactoryImpl.java` - Creates IDOC receivers
- `BWDataSourceFactoryImpl.java` - Creates BW sources
- Decouples creation from usage

### `adapter/`
Adapter pattern implementations:
- `IDocReceiverAdapter.java` - Adapts IDOC receiver
- `BWDataSourceAdapter.java` - Adapts BW source
- Bridges between external SAP interfaces and Kafka

### `model/`
Data model classes:
- `IDocMessage.java` - IDOC data structure
- `BWDataRequest.java` - BW data structure
- Can be auto-generated from SAP structures

### `service/`
Business logic layer:
- `IDocService.java` - IDOC processing logic
- `BWDataService.java` - BW data processing logic
- Handles orchestration and business rules

### `controller/`
REST endpoint definitions:
- `HealthController.java` - Health check endpoints
- `InfoController.java` - Application info endpoints
- Can be extended for admin functions

### `named/`
SAP Named Connection Service:
- `NamedConnectionService.java` - Manages SAP connections
- Connection pooling and lifecycle
- Receives RFC calls from SAP

---

## File Naming Conventions

### Java Classes

```
✅ Good Names:
- IDocKafkaPublisher.java         (interface naming with I prefix)
- IDocKafkaPublisherImpl.java      (implementation with Impl suffix)
- IDocReceiverAdapter.java        (pattern in name)
- IDocTopicNameUtil.java          (utility in name)
- KafkaPublishException.java      (exception in name)
- ApplicationConfiguration.java    (clear purpose)

❌ Avoid:
- idocpublisher.java              (lowercase, unclear)
- IDocPublisher2.java             (version number in name)
- IDocPublisherNew.java           (temporal state in name)
- Utils.java                      (too generic)
- Helper.java                     (vague purpose)
```

### Test Classes

```
✅ Good Names:
- IDocKafkaPublisherTest.java
- IDocTopicNameUtilTest.java
- KafkaIntegrationTest.java
- IDocPublisherIT.java            (IT = Integration Test)

❌ Avoid:
- IDocTest.java                   (which class tested?)
- Test1.java                      (meaningless)
- IDocPublisherTestCase.java      (too long)
```

### Configuration Files

```
✅ Good Names:
- application.properties           (default)
- application-dev.properties       (development profile)
- application-test.properties      (test profile)
- application-prod.properties      (production profile)
- logback-spring.xml              (Spring Boot logging)

❌ Avoid:
- config.properties               (too vague)
- application-local.properties    (machine-specific)
- props.xml                       (unclear)
```

---

## How to Create Structure in Eclipse

### Method 1: Using Eclipse Menus (Easiest)

1. **Expand `src/main/java`** in Project Explorer
2. Right-click and select **New → Package**
3. Enter package name: `org.dataingest.rfc.server`
4. Click **Finish**
5. Right-click on created package
6. Repeat for sub-packages: `config`, `publisher`, `util`, etc.

### Method 2: Dragging Files

1. If you have generated Java files in project root
2. Create package structure first
3. Drag/copy files to appropriate packages
4. Eclipse asks about updates - select **Yes to All**

### Method 3: Maven Plugin (Automated)

Use Maven archetype to generate structure:

```bash
mvn archetype:generate \
  -DgroupId=org.dataingest \
  -DartifactId=rfc-server \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

Then copy your code into generated structure.

---

## Build Output Structure

After running `mvn clean package`:

```
target/
├── classes/                          (Compiled main classes)
│   └── org/dataingest/rfc/server/
│       ├── config/
│       ├── publisher/
│       └── ... (all packages)
│
├── test-classes/                    (Compiled test classes)
│   └── org/dataingest/rfc/server/
│       └── ... (all test packages)
│
├── rfc-server-1.0.0.jar            (Executable JAR)
├── rfc-server-1.0.0-sources.jar    (Source code JAR)
├── rfc-server-1.0.0-javadoc.jar    (JavaDoc JAR)
│
├── surefire-reports/               (Test reports)
│   └── ... (test results)
│
└── site/                            (Generated documentation)
    ├── jacoco/                      (Code coverage reports)
    └── surefire-report.html         (Test report)
```

### Important: Do NOT edit target/ directory

- Contents regenerated on each build
- Safe to delete at any time
- Keep `.gitignore` excluding target/

---

## Version Control Setup

### .gitignore File

Create `.gitignore` in project root:

```gitignore
# Build artifacts
target/
dist/
*.jar
*.war
*.ear
*.class

# IDE
.classpath
.project
.settings/
.idea/
*.iml
*.iws
*.ipr

# Dependencies
.m2/
.gradle/

# OS
.DS_Store
Thumbs.db
*.swp

# Logs
*.log
logs/

# Test
.coverage
.testrun
```

### .gitattributes File (Optional)

Create `.gitattributes` in project root:

```
# Auto line endings
* text=auto
*.java text eol=lf
*.xml text eol=lf
*.properties text eol=lf
*.md text eol=lf

# Binary files
*.jar binary
*.class binary
*.png binary
*.jpg binary
```

---

## Eclipse IDE Project Files

These are created automatically by Eclipse:

### `.project`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
    <name>rfc-server</name>
    <comment>RFC Server - Kafka-based SAP integration</comment>
    <projects/>
    <buildSpec>
        <buildCommand>
            <name>org.eclipse.jdt.core.javabuilder</name>
        </buildCommand>
        <buildCommand>
            <name>org.eclipse.m2e.core.maven2Builder</name>
        </buildCommand>
    </buildSpec>
    <natures>
        <nature>org.eclipse.jdt.core.javanature</nature>
        <nature>org.eclipse.m2e.core.maven2Nature</nature>
    </natures>
</projectDescription>
```

### `.classpath`
Auto-generated - Do NOT edit manually. Eclipse manages this based on:
- Source folders configuration
- Maven dependency resolution
- JDK configuration

---

## Quick Eclipse Structure Commands

### Create Package
```
Right-click src/main/java → New → Package → Enter name → Finish
```

### Create Java Class
```
Right-click package → New → Class → Enter name → Finish
```

### Create Test Class
```
Right-click package in src/test/java → New → JUnit Test Case
```

### Create Properties File
```
Right-click src/main/resources → New → File → name.properties → Finish
```

### Create Folder
```
Right-click src/main/resources → New → Folder → Enter name → Finish
```

---

## Organizing Existing Files

If you have Java files in project root that need to be moved:

1. **View in Explorer**
   - Right-click file
   - **Open With → System Explorer**

2. **Copy/Move Manually**
   - Copy file to appropriate package folder
   - In Eclipse: F5 to refresh
   - Delete original file

3. **Refactor in Eclipse**
   - Right-click class
   - **Refactor → Move**
   - Select destination package
   - Eclipse updates all references automatically

---

## Common Structure Mistakes to Avoid

| Mistake | Problem | Solution |
|---------|---------|----------|
| Files in default package | Not searchable, poor organization | Always use `org.dataingest.rfc.server.*` |
| Mixed src and test code | Confusing, harder to deploy | Keep test-only code in `src/test/java` |
| Flat package structure | Hard to navigate large codebases | Organize by feature/layer |
| No resources directory | Configuration hard to manage | Create `src/main/resources`, `src/test/resources` |
| Files outside packages | Compilation issues | All Java files must be in packages |

---

## Moving Forward

Once structure is created:

1. ✅ **Copy generated Java files** to appropriate packages
2. ✅ **Create unit tests** mirror source structure
3. ✅ **Add configuration files** to resources folders
4. ✅ **Run Maven build** to verify structure
5. ✅ **Commit to Git** with meaningful message

---

**Document Version:** 1.0.0
**Last Updated:** 2024-12-18
**Status:** Complete - Ready for Use
