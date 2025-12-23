# Eclipse IDE Setup Guide - RFC Server Project

## Quick Start Overview

This guide provides step-by-step instructions for setting up the RFC Server project in Eclipse IDE for testing, debugging, and updating the refactored Kafka-based implementation.

**Project Details:**
- **Project Name:** RFC Server (rfc-server)
- **Package:** org.dataingest.rfc.server
- **Build Tool:** Maven 3.6+
- **Java Version:** 11+ (recommended Java 17+)
- **IDE:** Eclipse IDE 2024-03 or later
- **Spring Boot Version:** 2.7+ or 3.x

---

## Part 1: Prerequisites & Environment Setup

### 1.1 Required Software

Before starting, ensure you have installed:

- **Java Development Kit (JDK)**
  - Version: 11 or higher (17+ recommended)
  - Download: https://www.oracle.com/java/technologies/downloads/
  - Verify: `java -version` and `javac -version` in terminal

- **Maven**
  - Version: 3.6.0 or higher
  - Download: https://maven.apache.org/download.cgi
  - Verify: `mvn -version` in terminal

- **Eclipse IDE for Enterprise Java Developers**
  - Version: 2024-03 or later
  - Download: https://www.eclipse.org/downloads/packages/
  - Package: "Eclipse IDE for Enterprise Java and Web Developers"

- **Apache Kafka (for testing)**
  - Version: 3.0+ (optional, for integration testing)
  - Download: https://kafka.apache.org/downloads

### 1.2 Environment Variables

Configure these environment variables on Windows:

```
JAVA_HOME = C:\Program Files\Java\jdk-17
MAVEN_HOME = C:\Program Files\Apache\maven-3.8.1
PATH = %JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%
```

Verify setup:
```bash
java -version
javac -version
mvn --version
```

### 1.3 Git Configuration (if using version control)

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@company.com"
```

---

## Part 2: Eclipse IDE Installation & Configuration

### 2.1 Install Eclipse IDE

1. **Download Eclipse IDE for Enterprise Java Developers**
   - Visit: https://www.eclipse.org/downloads/packages/
   - Select your operating system (Windows 64-bit)
   - Extract to: `C:\eclipse` or preferred location

2. **Launch Eclipse**
   - Double-click `eclipse.exe`
   - Select workspace location: `D:\RFC_SERVER\workspace` (or your preference)

3. **Initial Setup**
   - Accept license agreement
   - Wait for initial plugin installation
   - Close "Welcome" tab when complete

### 2.2 Configure Java & Maven in Eclipse

#### Configure JDK

1. **Window → Preferences** (or Eclipse → Settings on macOS)
2. Navigate to: **Java → Installed JREs**
3. Click **Add...**
4. Select **Standard VM**
5. Browse to JDK installation (e.g., `C:\Program Files\Java\jdk-17`)
6. Set as default and apply

#### Configure Maven

1. **Window → Preferences**
2. Navigate to: **Maven → Installations**
3. Click **Add...**
4. Browse to Maven installation (e.g., `C:\Program Files\Apache\maven-3.8.1`)
5. Set as default
6. Navigate to: **Maven → User Settings**
7. Verify `settings.xml` location (typically `~/.m2/settings.xml`)
8. Apply and Close

#### Configure Maven Repository

1. **Window → Preferences**
2. Navigate to: **Maven → Repositories**
3. Verify or add repository index for Maven Central
4. If needed, add your corporate repository:
   ```xml
   <!-- In ~/.m2/settings.xml -->
   <repository>
       <id>central</id>
       <url>https://repo.maven.apache.org/maven2</url>
   </repository>
   ```

### 2.3 Install Optional but Recommended Plugins

From **Help → Eclipse Marketplace**:

- **Spring Tools 4** - Spring Boot development support
  - Search: "Spring Tools 4"
  - Install and restart Eclipse

- **Kafka Eclipse Plug-in** - Kafka development support (optional)
  - Search: "Kafka" or "JetBrains Kafka"
  - Helpful for testing Kafka topics

---

## Part 3: Import & Configure RFC Server Project

### 3.1 Import Project into Eclipse

#### Option A: From Existing Maven Project

1. **File → Import...**
2. Select **Maven → Existing Maven Projects**
3. Click **Next**
4. **Root Directory:** Browse to `D:\RFC_SERVER\ProjectRFC`
5. Click **Finish**
6. Wait for Maven to download dependencies (5-10 minutes first time)
7. Right-click project → **Maven → Update Project** (Force update)

#### Option B: From Git Repository

1. **File → Import...**
2. Select **Git → Projects from Git**
3. Click **Next**
4. Choose **Clone URI**
5. Enter repository URL and credentials
6. Complete the wizard
7. Right-click project → **Maven → Update Project**

### 3.2 Project Structure Verification

After import, verify the project structure:

```
rfc-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/dataingest/rfc/server/
│   │   │       ├── config/
│   │   │       │   ├── ApplicationConfiguration.java
│   │   │       │   └── KafkaPublishException.java
│   │   │       ├── publisher/
│   │   │       │   ├── IDocKafkaPublisher.java
│   │   │       │   └── BWDataKafkaPublisher.java
│   │   │       ├── util/
│   │   │       │   ├── IDocTopicNameUtil.java
│   │   │       │   └── BWDataTopicNameUtil.java
│   │   │       ├── factory/
│   │   │       │   ├── IDocReceiverFactoryImpl.java
│   │   │       │   └── BWDataSourceFactoryImpl.java
│   │   │       └── adapter/
│   │   │           ├── IDocReceiverAdapter.java
│   │   │           └── BWDataSourceAdapter.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/
│       └── resources/
├── pom.xml
├── IMPLEMENTATION_GUIDE.md
├── REMAINING_TASKS.md
└── README.md
```

### 3.3 Build Configuration

After import, Eclipse automatically runs Maven. Monitor the **Problems** view for any issues.

#### Fix Common Import Issues

1. **"Cannot find symbol"** errors:
   - Right-click project → **Maven → Update Project** (Force with `F5`)

2. **JDK not found**:
   - Right-click project → **Properties**
   - Navigate to: **Java Build Path → Libraries**
   - Remove invalid JRE → Add correct JRE

3. **Source not found**:
   - Right-click project → **Properties**
   - Navigate to: **Java Build Path → Source**
   - Verify paths are correct

---

## Part 4: Maven Configuration & Dependency Management

### 4.1 Update pom.xml (if needed)

Key dependencies that should be in `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Kafka Client -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
    </dependency>

    <!-- Jackson for JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <scope>test</scope>
        <classifier>test</classifier>
    </dependency>
</dependencies>
```

### 4.2 Clean Maven Cache (if dependency issues occur)

```bash
# In Eclipse Terminal or Windows Command Prompt
mvn clean
mvn clean package
```

Or manually delete cache:
```
Delete: %USERPROFILE%\.m2\repository\org\dataingest\
Delete: %USERPROFILE%\.m2\repository\org\talend\
```

---

## Part 5: Project Building & Compilation

### 5.1 Build Project in Eclipse

**Method 1: Auto Build (Default)**
- Automatically builds when you save files
- View build status in bottom-right corner
- Check **Problems** view for errors

**Method 2: Manual Build**
1. Right-click project name
2. Select **Build Project**
3. Monitor **Console** view for output

**Method 3: Maven CLI**
```bash
# Navigate to project directory
cd D:\RFC_SERVER\ProjectRFC

# Clean and build
mvn clean install

# Or just build
mvn package

# Skip tests
mvn package -DskipTests
```

### 5.2 Monitor Build Progress

- **Console** view: See Maven output and compile errors
- **Problems** view: See all errors and warnings
- **Markers** on file tabs: Red X = error, Yellow ! = warning

### 5.3 Typical Build Errors & Solutions

| Error | Solution |
|-------|----------|
| `[ERROR] Invalid byte 1 of 1-byte UTF-8 sequence` | Encoding issue. File → Properties → Text File Encoding: UTF-8 |
| `[ERROR] 'encoding' cannot be resolved` | Add `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` to pom.xml |
| `[ERROR] Source option 11 is no longer supported` | Update pom.xml: `<maven.compiler.source>17</maven.compiler.source>` |
| `ClassNotFoundException` on build | Maven → Update Project (Force), then clean and rebuild |

---

## Part 6: Running & Testing

### 6.1 Run Application from Eclipse

#### Option A: Run as Spring Boot Application

1. Right-click project root
2. Select **Run As → Spring Boot App**
3. Monitor **Console** view for startup logs
4. Verify: `Started RFCServerApplication` message

#### Option B: Run as Java Application

1. **Run → Run Configurations...**
2. Create new **Java Application**
3. **Project:** Select "rfc-server"
4. **Main class:** `org.dataingest.rfc.server.RFCServerApplication`
5. **Arguments** tab:
   - **VM arguments:**
     ```
     -Dspring.config.location=classpath:application.properties
     -Dlogging.level.org.dataingest=DEBUG
     ```
6. Click **Run**

#### Option C: Maven CLI

```bash
# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn package
java -jar target/rfc-server-1.0.0.jar
```

### 6.2 Application Properties Configuration

Create/Edit `src/main/resources/application.properties`:

```properties
# Spring Boot
spring.application.name=rfc-server
server.port=8080
server.servlet.context-path=/rfc

# Kafka Producer Configuration
kafka.bootstrap.servers=localhost:9092
kafka.acks=all
kafka.retries=3
kafka.max.in.flight.requests.per.connection=1
kafka.compression.type=gzip
kafka.enable.idempotence=true
kafka.request.timeout.ms=30000
kafka.delivery.timeout.ms=120000

# Feature Flags
feature.idoc.enabled=true
feature.idoc.transactional=true
feature.idoc.transactionAbortTimeout=60000
feature.bw_source_system.enabled=true

# Logging
logging.level.root=INFO
logging.level.org.dataingest.rfc.server=DEBUG
logging.level.org.springframework.boot=INFO
logging.level.org.apache.kafka=WARN

# Management (Actuator)
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### 6.3 Unit Testing

#### Run All Tests

```bash
# Eclipse: Right-click project → Run As → Maven test
# Or use Maven CLI:
mvn test
```

#### Run Specific Test Class

```bash
# Right-click test file → Run As → JUnit Test
# Or Maven CLI:
mvn test -Dtest=IDocKafkaPublisherTest
```

#### Create New Test

1. Right-click test class in `src/test/java`
2. Select **JUnit → New JUnit Test Case**
3. Configure test class details
4. Click **Finish**
5. Write test methods

Example test structure:

```java
package org.dataingest.rfc.server.publisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class IDocKafkaPublisherTest {

    private IDocKafkaPublisher publisher;

    @BeforeEach
    public void setup() {
        // Initialize publisher
    }

    @Test
    public void testPublishSuccess() {
        // Test successful publish
    }

    @Test
    public void testPublishTimeout() {
        // Test timeout handling
    }
}
```

### 6.4 Integration Testing

For testing with actual Kafka:

```java
@SpringBootTest
@ActiveProfiles("test")
public class IDocKafkaIntegrationTest {

    @Autowired
    private IDocKafkaPublisher publisher;

    @Test
    public void testPublishToKafka() throws Exception {
        // Test actual Kafka integration
    }
}
```

Set up `src/test/resources/application-test.properties`:

```properties
kafka.bootstrap.servers=localhost:29092
logging.level.org.dataingest.rfc.server=DEBUG
```

---

## Part 7: Debugging

### 7.1 Set Breakpoints

1. Click in left margin of code editor next to line number
2. Blue circle appears = breakpoint set
3. Run application in Debug mode

### 7.2 Debug Application

**Method 1: Eclipse Debug Launch**

1. Right-click project
2. Select **Debug As → Spring Boot App**
3. Application starts in debug mode
4. Execution pauses at breakpoints
5. Use **Debug** view to inspect variables

**Method 2: Debug Configuration**

1. **Run → Debug Configurations...**
2. Create new **Java Application** debug configuration
3. Configure same as run configuration
4. Click **Debug**

### 7.3 Debug Views & Controls

| View | Purpose |
|------|---------|
| **Debug** | Shows execution stack, threads, variables |
| **Variables** | Displays variable values in current scope |
| **Expressions** | Evaluate custom expressions |
| **Breakpoints** | Manage all breakpoints |
| **Console** | Application output and input |

### 7.4 Stepping Through Code

| Action | Keyboard | Purpose |
|--------|----------|---------|
| Step Over | F6 | Execute current line, stay in method |
| Step Into | F5 | Enter called method |
| Step Out | F7 | Execute until current method returns |
| Resume | F8 | Continue execution to next breakpoint |
| Terminate | Ctrl+Alt+W | Stop debugging |

### 7.5 Watch Expressions

1. While debugging, right-click variable in **Variables** view
2. Select **Watch**
3. Expression appears in **Expressions** view
4. Evaluate automatically as you step through code

---

## Part 8: Code Editing & Navigation

### 8.1 Navigate Code Efficiently

| Task | Shortcut | Menu |
|------|----------|------|
| Open type/class | Ctrl+Shift+T | Navigate → Open Type |
| Open resource | Ctrl+Shift+R | Navigate → Open Resource |
| Go to line | Ctrl+G | Navigate → Go to Line |
| Find references | Ctrl+Shift+G | Search → References |
| Find declarations | Ctrl+G | Search → Declarations |
| Quick outline | Ctrl+O | Navigate → Quick Outline |

### 8.2 Code Completion & Generation

| Task | Shortcut | Purpose |
|------|----------|---------|
| Code Assist | Ctrl+Space | Show code suggestions |
| Quick Fix | Ctrl+1 | Apply quick fixes to errors |
| Generate Methods | Alt+Shift+S | Generate getters/setters, constructors |
| Format Code | Ctrl+Shift+F | Format selected code |
| Organize Imports | Ctrl+Shift+O | Clean up and organize imports |

### 8.3 Refactoring

Right-click identifier:
- **Rename** - Rename in all references
- **Move** - Move to different package
- **Extract Method** - Extract code to new method
- **Extract Local Variable** - Create variable from expression
- **Change Method Signature** - Update method parameters

---

## Part 9: Maven & Dependency Management in Eclipse

### 9.1 Update Dependencies

1. Right-click project
2. Select **Maven → Update Project** (Alt+F5)
3. Select **Force Update of Snapshots/Releases**
4. Click **OK**

### 9.2 View Dependency Tree

1. Right-click `pom.xml`
2. Select **Maven → Show Dependency Tree**
3. Panel shows all resolved dependencies and conflicts

### 9.3 Add New Dependency

1. Open `pom.xml`
2. Click **Dependency Hierarchy** tab
3. Click **Add** button
4. Enter Group ID, Artifact ID, Version
5. Save file

Or manually edit `<dependencies>` section in **Source** tab.

---

## Part 10: Version Control Integration (Git)

### 10.1 Clone Repository into Eclipse

1. **File → Import...**
2. Select **Git → Projects from Git (with smart import)**
3. Click **Next**
4. **Clone URI**
5. Enter:
   - **URI:** `https://github.com/org/rfc-server.git`
   - **Authentication:** Enter credentials if needed
6. Complete wizard

### 10.2 Manage Changes

**Stage Changes:**
- Right-click file → **Team → Add to Index**

**Commit:**
- Right-click project → **Team → Commit...**
- Enter commit message
- Click **Commit**

**Push:**
- Right-click project → **Team → Push to Upstream**

**View History:**
- Right-click file → **Team → Show in History**

---

## Part 11: Common Eclipse Tasks & Workflows

### 11.1 Add New Java Class

1. Right-click package in `src/main/java`
2. Select **New → Class**
3. Enter:
   - **Name:** ClassName
   - **Package:** org.dataingest.rfc.server.*
   - Options: public, generate main()
4. Click **Finish**

### 11.2 Rename Package

1. Right-click package
2. Select **Refactor → Rename**
3. Enter new package name
4. Review changes and click **OK**

### 11.3 Find & Replace

- **Ctrl+H** - Open Find and Replace dialog
- Search across files using regex
- Preview changes before applying

### 11.4 View Maven Errors

1. Right-click project
2. Select **Validate**
3. Check **Problems** view for Maven validation issues

---

## Part 12: Performance Tips & Troubleshooting

### 12.1 Improve Eclipse Performance

1. **Increase memory allocation**
   - Edit `eclipse.ini`:
   ```
   -Xms1024m
   -Xmx2048m
   ```

2. **Disable unnecessary plugins**
   - **Help → About Eclipse → Installation Details**
   - Uninstall rarely-used plugins

3. **Use Project Build Sets**
   - **Project → Build Set → Edit...**
   - Only build relevant projects

4. **Enable indexing optimization**
   - **Window → Preferences → Java → Indexing**
   - Uncheck "Index all files" if not needed

### 12.2 Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| **Slow code completion** | Disable unnecessary plugins, increase memory |
| **Build takes forever** | Disable auto-build (Project → Build Automatically), build manually |
| **Eclipse freezes** | Increase Xmx in eclipse.ini, check background threads |
| **Maven dependencies not found** | Maven → Update Project (Force), clear .m2 cache |
| **Source folder not recognized** | Right-click folder → Build Path → Use as Source Folder |
| **Cannot find type** | Project → Clean, then Build Project |

### 12.3 Clear Cache & Reset

```bash
# Stop Eclipse first

# Delete metadata
del /s /q "%ECLIPSE_WORKSPACE%\.metadata"

# Or on macOS/Linux:
rm -rf ~/.eclipse/*/workspace/.metadata

# Restart Eclipse
```

---

## Part 13: Testing Checklist

### 13.1 Unit Test Execution

- [ ] Run all unit tests: `mvn test`
- [ ] All tests pass
- [ ] Code coverage > 80% (optional)
- [ ] No test failures in Eclipse Problems view

### 13.2 Integration Testing

- [ ] Start Kafka broker locally
- [ ] Configure `application-test.properties`
- [ ] Run integration tests: `mvn verify`
- [ ] Verify Kafka topic creation
- [ ] Verify message publishing

### 13.3 Application Startup

- [ ] Start application: `mvn spring-boot:run`
- [ ] Verify no startup errors
- [ ] Check health endpoint: `http://localhost:8080/rfc/actuator/health`
- [ ] Response shows: `{"status":"UP"}`

### 13.4 Feature Testing

- [ ] IDOC publishing to Kafka
- [ ] BW data publishing to Kafka
- [ ] JSON serialization validation
- [ ] Error handling and rollback
- [ ] Transaction commit/rollback

---

## Part 14: Build & Package

### 14.1 Create Executable JAR

```bash
# Terminal in project directory
mvn clean package

# JAR created at:
# target/rfc-server-1.0.0.jar
```

### 14.2 Build Verification

After build completes, verify:

```bash
# Verify JAR contents
jar tf target/rfc-server-1.0.0.jar | grep "org/dataingest"

# Run JAR
java -jar target/rfc-server-1.0.0.jar

# Check health endpoint
curl http://localhost:8080/rfc/actuator/health
```

### 14.3 Maven Build Profiles

Add test profiles to `pom.xml`:

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <maven.build.timestamp.format>yyyyMMdd-HHmmss</maven.build.timestamp.format>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-shade-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Build with profile: `mvn clean package -Pprod`

---

## Part 15: References & Resources

### 15.1 Official Documentation

- **Eclipse IDE:** https://www.eclipse.org/ide/
- **Maven Documentation:** https://maven.apache.org/guides/
- **Spring Boot:** https://spring.io/projects/spring-boot
- **Apache Kafka:** https://kafka.apache.org/
- **Jackson JSON:** https://github.com/FasterXML/jackson

### 15.2 Useful Links

- **Eclipse Keyboard Shortcuts:** https://www.eclipse.org/eclipse/news/4.9/cheatsheet.pdf
- **Maven Help:** `mvn help:active-profiles`
- **Spring Boot Guides:** https://spring.io/guides

### 15.3 Getting Help

- **Eclipse Forum:** https://www.eclipse.org/forums/
- **Stack Overflow:** Tag: [eclipse], [maven], [spring-boot]
- **Project Issues:** GitHub repository issues

---

## Quick Reference Commands

```bash
# Maven Commands
mvn clean                    # Clean build artifacts
mvn compile                  # Compile source
mvn test                     # Run tests
mvn package                  # Build JAR
mvn install                  # Install to local repository
mvn clean package            # Clean + Build
mvn spring-boot:run          # Run Spring Boot app
mvn -X clean package         # Debug mode build

# Java Commands
java -version                # Check Java version
java -jar target/rfc-server-1.0.0.jar        # Run JAR
java -Xmx1024m -jar rfc-server-1.0.0.jar    # Run with memory limit

# Eclipse Shortcuts
Ctrl+Space                   # Code completion
Ctrl+Shift+O                 # Organize imports
Ctrl+Shift+F                 # Format code
Ctrl+H                       # Find and replace
Ctrl+1                       # Quick fix
Alt+Shift+S                  # Generate (getters, setters, etc.)
Ctrl+Shift+R                 # Open resource
Ctrl+Shift+T                 # Open type
F5                           # Step into (debug)
F6                           # Step over (debug)
F8                           # Resume (debug)
```

---

## Summary

You now have a complete Eclipse IDE setup for the RFC Server project. Follow the phases:

1. ✅ Install prerequisites (Java, Maven, Eclipse)
2. ✅ Configure Eclipse settings
3. ✅ Import project into Eclipse
4. ✅ Build and compile
5. ✅ Run and debug application
6. ✅ Create and run tests
7. ✅ Package and deploy

**Next Steps:**
- Complete tasks in REMAINING_TASKS.md
- Implement integration tests
- Deploy to test environment
- Configure Kafka topics
- Test end-to-end workflows

---

**Document Version:** 1.0.0
**Last Updated:** 2024-12-18
**Status:** Complete - Ready for Use
