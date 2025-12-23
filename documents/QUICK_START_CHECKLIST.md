# RFC Server - Quick Start Checklist for Eclipse IDE

## Pre-Setup (Complete Before Opening Eclipse)

### Environment Verification
- [ ] Java installed: `java -version` shows 11+
- [ ] Maven installed: `mvn -version` shows 3.6.0+
- [ ] Eclipse IDE for Enterprise Java downloaded
- [ ] Workspace location planned (e.g., `D:\RFC_SERVER\workspace`)

### System Requirements Check
- [ ] Windows/macOS/Linux system requirements met
- [ ] At least 4GB RAM available
- [ ] 2GB disk space for IDE and project
- [ ] Network access to Maven Central Repository

---

## Phase 1: Eclipse Installation & Configuration (30 minutes)

### Install Eclipse
- [ ] Download Eclipse IDE 2024-03 or later
- [ ] Extract to installation directory (e.g., `C:\eclipse`)
- [ ] Launch Eclipse executable
- [ ] Select workspace location
- [ ] Wait for initial setup to complete

### Configure Java in Eclipse
- [ ] **Window → Preferences → Java → Installed JREs**
- [ ] Click **Add...**
- [ ] Select JDK installation path
- [ ] Set as default
- [ ] Click **Apply and Close**

### Configure Maven in Eclipse
- [ ] **Window → Preferences → Maven → Installations**
- [ ] Click **Add...**
- [ ] Select Maven installation path
- [ ] Set as default
- [ ] **Apply and Close**

### Optional: Install Spring Tools Plugin
- [ ] **Help → Eclipse Marketplace**
- [ ] Search: "Spring Tools 4"
- [ ] Install and restart Eclipse

---

## Phase 2: Import Project (15 minutes)

### Import RFC Server Project
- [ ] Create workspace folder: `D:\RFC_SERVER\workspace`
- [ ] **File → Import...**
- [ ] Select **Maven → Existing Maven Projects**
- [ ] Browse to: `D:\RFC_SERVER\ProjectRFC`
- [ ] Click **Finish**

### Wait for Maven Processing
- [ ] Monitor bottom-right corner for progress
- [ ] Allow 5-10 minutes for first-time dependency download
- [ ] Check **Problems** view for any errors

### Update Project Configuration
- [ ] Right-click project
- [ ] **Maven → Update Project** (Alt+F5)
- [ ] Select **Force Update of Snapshots/Releases**
- [ ] Click **OK**

### Verify Project Structure
- [ ] Expand project tree
- [ ] Verify `src/main/java` exists
- [ ] Verify `src/test/java` exists
- [ ] Verify `pom.xml` present
- [ ] No red X errors on files

---

## Phase 3: Build Configuration (10 minutes)

### Verify Source Folders
- [ ] Right-click `src/main/java` → **Properties**
- [ ] Verify "Source Folder" designation
- [ ] Same for `src/test/java`
- [ ] Same for `src/main/resources`
- [ ] Same for `src/test/resources`

### Configure JDK for Project
- [ ] Right-click project → **Properties**
- [ ] **Java Build Path → Libraries**
- [ ] Verify JDK 11+ listed
- [ ] Remove any invalid JRE entries

### First Build
- [ ] Right-click project
- [ ] **Build Project** (Ctrl+B)
- [ ] Monitor **Console** view for build output
- [ ] Verify: "Build success" or similar message

---

## Phase 4: Application Configuration (10 minutes)

### Create application.properties
- [ ] Verify exists: `src/main/resources/application.properties`
- [ ] If missing, create new file:
  1. Right-click `src/main/resources`
  2. **New → File**
  3. Name: `application.properties`

### Configure Properties
- [ ] Add Kafka bootstrap servers
- [ ] Add feature flags
- [ ] Add logging configuration
- [ ] Save file

### Example Configuration
```properties
spring.application.name=rfc-server
server.port=8080
kafka.bootstrap.servers=localhost:9092
kafka.acks=all
kafka.retries=3
feature.idoc.enabled=true
feature.bw_source_system.enabled=true
logging.level.org.dataingest.rfc.server=DEBUG
```

---

## Phase 5: Initial Application Test (15 minutes)

### Start Kafka (if testing locally)
- [ ] Kafka broker running on `localhost:9092`
- [ ] Kafka topics created or auto-created enabled
- [ ] Verify connection: `kafka-console-producer --broker-list localhost:9092 --topic test`

### Run Application
- [ ] Right-click project
- [ ] **Run As → Spring Boot App**
- [ ] Monitor **Console** for startup messages
- [ ] Look for: "Started RFCServerApplication"

### Verify Startup
- [ ] Application started without errors
- [ ] No exceptions in console
- [ ] No Spring configuration errors
- [ ] Application listening on port 8080

### Test Health Endpoint
- [ ] Open browser: `http://localhost:8080/rfc/actuator/health`
- [ ] Should return: `{"status":"UP"}`
- [ ] Kafka producer bean initialized

### Stop Application
- [ ] Red stop button in **Console** view
- [ ] Or: Ctrl+Alt+W

---

## Phase 6: Unit Tests Setup (15 minutes)

### Verify Test Directory Structure
- [ ] `src/test/java/org/dataingest/rfc/server/` exists
- [ ] Create subdirectories:
  - [ ] `publisher/`
  - [ ] `util/`
  - [ ] `config/`
  - [ ] `integration/`

### Create Test Configuration
- [ ] Create: `src/test/resources/application-test.properties`
- [ ] Add test-specific configuration
- [ ] Add Kafka test configuration

### Run Existing Tests
- [ ] Right-click `src/test/java`
- [ ] **Run As → Maven test**
- [ ] Monitor **Console** for test output
- [ ] Check **JUnit** view for results

### Verify JUnit View
- [ ] **Window → Show View → JUnit**
- [ ] Tests appear in tree structure
- [ ] Green checkmarks = passed tests

---

## Phase 7: First Code Contribution (20 minutes)

### Copy Generated Files
- [ ] Verify Java files in project root:
  - [ ] `ApplicationConfiguration.java`
  - [ ] `KafkaPublishException.java`
  - [ ] `IDocKafkaPublisher.java`
  - [ ] `BWDataKafkaPublisher.java`
  - [ ] And others...

### Create Package Structure
- [ ] Right-click `src/main/java/org/dataingest/rfc/server`
- [ ] **New → Package**
- [ ] Create: `config`
- [ ] Create: `publisher`
- [ ] Create: `util`
- [ ] Create: `factory`
- [ ] Create: `adapter`
- [ ] Create: `exception`

### Move/Copy Classes to Packages
- [ ] Copy `ApplicationConfiguration.java` to `config/`
- [ ] Copy `KafkaPublishException.java` to `exception/`
- [ ] Copy publisher classes to `publisher/`
- [ ] Copy utility classes to `util/`
- [ ] Copy factory classes to `factory/`
- [ ] Copy adapter classes to `adapter/`

### Update Package Declarations
- [ ] Open each file
- [ ] Verify package statement at top
- [ ] Update if needed to match location
- [ ] Organize imports: Ctrl+Shift+O

### Build Project
- [ ] **Build Project** (Ctrl+B)
- [ ] Verify no compilation errors
- [ ] **Problems** view should be empty

---

## Phase 8: Debugging Setup (10 minutes)

### Configure Debug Launcher
- [ ] **Run → Debug Configurations...**
- [ ] Create new **Java Application**
- [ ] **Project:** rfc-server
- [ ] **Main class:** org.dataingest.rfc.server.RFCServerApplication
- [ ] **Arguments tab:**
  - [ ] Add VM arguments for Kafka config
- [ ] Click **Apply**

### Test Debugging
- [ ] Open any Java file
- [ ] Click in left margin to set breakpoint
- [ ] **Run → Debug As → Debug Configurations** (select created config)
- [ ] Application starts in debug mode
- [ ] Stops at breakpoint

### Verify Debug Views
- [ ] **Debug** view shows execution stack
- [ ] **Variables** view shows current variables
- [ ] **Console** shows application output

---

## Phase 9: Maven Commands Practice (10 minutes)

### Maven Clean
- [ ] **Right-click project → Maven → Clean**
- [ ] Wait for completion
- [ ] Verify `target/` folder empty

### Maven Compile
- [ ] **Right-click project → Maven → Compile**
- [ ] Monitor **Console** for output
- [ ] Verify no compilation errors

### Maven Test
- [ ] **Right-click project → Maven → Test**
- [ ] Wait for test execution
- [ ] View results in **JUnit** view

### Maven Package
- [ ] **Right-click project → Maven → Package**
- [ ] Wait for build completion
- [ ] Verify JAR created: `target/rfc-server-*.jar`

---

## Phase 10: Project Organization (10 minutes)

### Organize Source Code
- [ ] Main application class in: `src/main/java/org/dataingest/rfc/server/`
- [ ] Packages organized by feature:
  - [ ] `config/` - Configuration classes
  - [ ] `publisher/` - Kafka publishers
  - [ ] `util/` - Utility classes
  - [ ] `factory/` - Factory implementations
  - [ ] `adapter/` - Adapter classes
  - [ ] `exception/` - Exception classes

### Organize Test Code
- [ ] Unit tests mirror source structure
- [ ] Integration tests in `integration/` package
- [ ] Test resources in `src/test/resources/`

### Cleanup Project
- [ ] Delete any duplicate files
- [ ] Remove old package folders
- [ ] Verify no broken imports

---

## Phase 11: Git Integration (10 minutes)

### Initialize or Clone Git Repository
- [ ] **File → Import... → Git → Projects from Git**
- [ ] Or use: **Team → Share Project** if folder already git repo

### Configure Git
- [ ] **Window → Preferences → Team → Git → Configuration**
- [ ] Set user.name
- [ ] Set user.email
- [ ] Apply settings

### Add to Git
- [ ] Right-click project
- [ ] **Team → Add to Index**

### Create Initial Commit
- [ ] Right-click project
- [ ] **Team → Commit...**
- [ ] Message: "Initial RFC Server project setup"
- [ ] Click **Commit**

---

## Phase 12: Documentation Review (10 minutes)

### Read Implementation Guide
- [ ] Open: `IMPLEMENTATION_GUIDE.md`
- [ ] Understand project refactoring goals
- [ ] Note remaining tasks

### Read Remaining Tasks
- [ ] Open: `REMAINING_TASKS.md`
- [ ] Identify Phase 1-6 tasks
- [ ] Plan implementation order

### Read Eclipse Setup Guide
- [ ] Reference: `ECLIPSE_SETUP_GUIDE.md`
- [ ] Bookmark for future reference
- [ ] Note keyboard shortcuts

---

## Phase 13: Daily Development Workflow

### Daily Startup
1. [ ] Launch Eclipse
2. [ ] Select workspace
3. [ ] Wait for project load
4. [ ] Verify no build errors

### Code Development
1. [ ] Edit Java files
2. [ ] Auto-build detects changes
3. [ ] Fix any compilation errors
4. [ ] Test changes with unit tests

### Before Committing
1. [ ] Run: `mvn clean test` (all tests pass)
2. [ ] Format code: Ctrl+Shift+F
3. [ ] Organize imports: Ctrl+Shift+O
4. [ ] Right-click project → **Team → Commit**

### End of Day
1. [ ] Commit changes with meaningful message
2. [ ] Push to remote: **Team → Push to Upstream**
3. [ ] Close Eclipse or leave running

---

## Phase 14: Troubleshooting Quick Links

| Issue | Solution |
|-------|----------|
| Project won't build | Maven → Clean, then Update Project |
| Can't find classes | Ctrl+B to rebuild, check package structure |
| Test won't run | Check test class name ends with "Test" |
| Kafka connection fails | Verify Kafka running, check properties |
| Import errors | Ctrl+Shift+O to organize imports |
| JDK not found | Check Java build path, verify JDK selected |

---

## Next Steps After Checklist

1. **Implement Remaining Tasks**
   - Follow REMAINING_TASKS.md
   - Complete in order (Phase 1-6)

2. **Create Unit Tests**
   - Use TESTING_GUIDE_ECLIPSE.md
   - Achieve 80%+ code coverage

3. **Integration Testing**
   - Set up embedded Kafka
   - Test message publishing
   - Verify JSON serialization

4. **Deploy to Test Environment**
   - Build JAR: `mvn package`
   - Configure application properties
   - Start application with Kafka

5. **Documentation**
   - Update README.md
   - Document configuration
   - Create deployment guide

---

## Support & Resources

### Official Documentation
- Eclipse IDE: https://www.eclipse.org/ide/
- Maven: https://maven.apache.org/
- Spring Boot: https://spring.io/projects/spring-boot
- Apache Kafka: https://kafka.apache.org/

### Project Documentation
- IMPLEMENTATION_GUIDE.md - Refactoring details
- REMAINING_TASKS.md - Task checklist
- ECLIPSE_SETUP_GUIDE.md - Detailed Eclipse setup
- TESTING_GUIDE_ECLIPSE.md - Testing procedures

### Troubleshooting
- Check **Problems** view for errors
- Monitor **Console** view for messages
- Use **Search → References** to find usage
- Use **Navigate → Open Type** to find classes

---

## Estimated Total Time

| Phase | Time |
|-------|------|
| Pre-Setup | 15 min |
| Phase 1-3 | 55 min |
| Phase 4-7 | 60 min |
| Phase 8-12 | 60 min |
| Phase 13-14 | 20 min |
| **Total** | **~3 hours** |

---

**Checklist Version:** 1.0.0
**Last Updated:** 2024-12-18
**Status:** Ready for Use

✅ **Start here if you're new to the project!**
