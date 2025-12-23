NEW

# Eclipse IDE Import Guide - RFC Server Project

## Overview

This guide provides step-by-step instructions to import the **RFC Server** project into Eclipse IDE as a Maven project.

**Project Status:** ✅ Ready to Import
- All Maven configuration complete (pom.xml)
- Eclipse project files created (.project, .classpath)
- Directory structure organized
- All Java source files in place

---

## Pre-Import Checklist

Before importing, verify you have:

- [ ] Eclipse IDE 2024-03 or later installed
- [ ] Java 11+ JDK installed and configured in Eclipse
- [ ] Maven 3.6+ installed and configured in Eclipse
- [ ] Internet connection (for Maven dependency download)
- [ ] Project path: `D:\RFC_SERVER\ProjectRFC`

---

## Part 1: Quick Import (5 minutes)

### Step 1: Open Eclipse
1. Launch Eclipse IDE
2. Select workspace (e.g., `D:\RFC_SERVER\workspace`)
3. Wait for Eclipse to fully load

### Step 2: Import Project
1. In Eclipse menu: **File → Import...**
2. In Import dialog: Expand **Maven** folder
3. Select **Existing Maven Projects**
4. Click **Next**

### Step 3: Browse to Project
1. Click **Browse...** button
2. Navigate to: `D:\RFC_SERVER\ProjectRFC`
3. Click **Select Folder**
4. Click **Finish**

### Step 4: Wait for Maven Processing
1. Eclipse shows: "Maven Configuration in progress..."
2. Progress bar in bottom-right corner
3. First import: 5-10 minutes (downloading dependencies)
4. Subsequent imports: <1 minute

### Step 5: Verify Import Success
1. Look for project: **rfc-server** in Project Explorer
2. No red X errors on project
3. Project tree expands to show packages
4. **Problems** view should be empty (or only warnings)

---

## Part 2: Detailed Import Steps

### Full Step-by-Step Import

#### Step 1: File Menu
```
File → Import...
```
- Eclipse opens Import Wizard dialog

#### Step 2: Select Import Source
```
In left tree panel:
  Expand: Maven
  Click: Existing Maven Projects
Click: Next
```

#### Step 3: Configure Source Directory
```
In "Select root directory":
  Click: Browse...
  Navigate to: D:\RFC_SERVER\ProjectRFC
  Click: OK
```

**You should see:**
- Root directory: `D:\RFC_SERVER\ProjectRFC`
- Checkbox: `pom.xml` (checked)
- Projects to import: `org.dataingest:rfc-server:1.0.0`

#### Step 4: Configure Import Options
```
Advanced Options (optional):
  ☑ Resolve workspace artifacts
  ☑ Add project(s) to working set
  ☐ Create separate project for each module
Click: Next (or Finish to use defaults)
```

#### Step 5: Configure Project Settings
```
If "Advanced" page appears:
  Organization: (leave blank)
  Project name: rfc-server (pre-filled)
  Click: Finish
```

#### Step 6: Wait for Maven
- Eclipse downloads all dependencies from Maven Central
- Download progress shown in Console view
- First time: 5-10 minutes
- Look for message: `BUILD SUCCESS` or `BUILD FAILURE`

#### Step 7: Verify Project Structure
```
In Project Explorer (left panel):
  rfc-server/
    ├── src/
    │   ├── main/java/org/dataingest/rfc/server/
    │   ├── main/resources/
    │   ├── test/java/
    │   └── test/resources/
    ├── target/
    ├── pom.xml
    └── .classpath
```

---

## Part 3: Troubleshooting Import Issues

### Issue 1: "Java compiler compliance level does not match"

**Solution:**
1. Right-click project → **Properties**
2. Go to: **Java Compiler**
3. Set: **Compiler compliance level: 11 or higher**
4. Click **Apply and Close**
5. Right-click project → **Build Project**

### Issue 2: "Cannot find Maven"

**Solution:**
1. **Window → Preferences → Maven → Installations**
2. Verify Maven installation is selected
3. If missing, click **Add...** and browse to Maven folder
4. Set as default
5. Retry import

### Issue 3: "JDK not found"

**Solution:**
1. **Window → Preferences → Java → Installed JREs**
2. Verify JDK 11+ is listed
3. If missing, click **Add...** and browse to JDK folder
4. Set as default
5. Right-click project → **Properties → Java Build Path → Libraries**
6. Remove invalid JRE, add correct one

### Issue 4: "Project cannot be imported - pom.xml error"

**Solution:**
1. Open `pom.xml` in Eclipse editor
2. Check for red squiggly lines (errors)
3. Right-click error → **Quick Fix**
4. Or delete project and retry import
5. Ensure file encoding is UTF-8

### Issue 5: "BUILD FAILURE during Maven download"

**Solution:**
1. Check internet connection
2. Verify Maven proxy settings (if corporate firewall)
3. **Window → Preferences → Maven → Repositories**
4. Click **Verify** to check repository connectivity
5. Force Maven update: Right-click project → **Maven → Update Project (Force)**

### Issue 6: "Cannot find symbol" errors after import

**Solution:**
1. Right-click project → **Maven → Update Project** (Alt+F5)
2. Check: "Force Update of Snapshots/Releases"
3. Click: **OK**
4. Wait for download to complete
5. Right-click project → **Build Project** (Ctrl+B)

---

## Part 4: Post-Import Steps

### Step 1: Verify Maven Build
```bash
# In Eclipse Terminal or Windows Command Prompt:
cd D:\RFC_SERVER\ProjectRFC
mvn clean package
```

**Expected:** BUILD SUCCESS

### Step 2: Run Initial Build in Eclipse
1. Right-click project
2. Select: **Build Project** (Ctrl+B)
3. Monitor: **Console** view
4. Wait for: "Build success" message

### Step 3: Check Project Structure
1. Expand project tree in Project Explorer
2. Verify you see:
   - `src/main/java/org/dataingest/rfc/server/` with all packages
   - `src/main/resources/` with `application.properties`
   - `src/test/java/org/dataingest/rfc/server/`
   - `pom.xml` in root

### Step 4: Verify No Build Errors
1. Open **Problems** view: **Window → Show View → Problems**
2. Should show: no errors (only warnings OK)
3. If errors exist, see Troubleshooting section above

### Step 5: Test Application Start
1. Right-click project
2. Select: **Run As → Spring Boot App**
3. Monitor **Console** view
4. Look for: `Started RFCServerApplication`
5. Verify: No startup errors
6. Stop application (red X in Console)

### Step 6: Run Tests
1. Right-click project
2. Select: **Run As → Maven test**
3. Monitor **Console** for test results
4. Check **JUnit** view for test status

---

## Part 5: Project Structure Verification

After successful import, verify complete structure:

```
rfc-server/
├── .classpath                  ✓ Eclipse classpath
├── .project                    ✓ Eclipse project file
├── .settings/                  ✓ Eclipse settings folder
│   ├── org.eclipse.core.resources.prefs
│   ├── org.eclipse.jdt.core.prefs
│   └── org.eclipse.m2e.core.prefs
├── .gitignore                  ✓ Git ignore file
│
├── src/
│   ├── main/
│   │   ├── java/org/dataingest/rfc/server/
│   │   │   ├── RFCServerApplication.java       ✓ Main class
│   │   │   ├── config/
│   │   │   │   └── ApplicationConfiguration.java
│   │   │   ├── exception/
│   │   │   │   └── KafkaPublishException.java
│   │   │   ├── publisher/
│   │   │   │   ├── IDocKafkaPublisher.java
│   │   │   │   └── BWDataKafkaPublisher.java
│   │   │   ├── util/
│   │   │   │   ├── IDocTopicNameUtil.java
│   │   │   │   └── BWDataTopicNameUtil.java
│   │   │   ├── factory/
│   │   │   │   ├── IDocReceiverFactoryImpl.java
│   │   │   │   └── BWDataSourceFactoryImpl.java
│   │   │   ├── adapter/
│   │   │   │   ├── IDocReceiverAdapter.java
│   │   │   │   └── BWDataSourceAdapter.java
│   │   │   └── (other package folders)
│   │   │
│   │   └── resources/
│   │       ├── application.properties     ✓ Config file
│   │       └── data/                      ✓ Data folder
│   │
│   └── test/
│       ├── java/org/dataingest/rfc/server/
│       │   ├── publisher/
│       │   ├── util/
│       │   ├── config/
│       │   └── integration/
│       │
│       └── resources/
│           ├── application-test.properties
│           └── fixtures/
│
├── target/                      ✓ Build output (auto-generated)
│   ├── classes/
│   ├── test-classes/
│   ├── rfc-server-1.0.0.jar    ✓ Executable JAR
│   └── site/
│
├── pom.xml                      ✓ Maven configuration
├── ECLIPSE_IMPORT_GUIDE.md      ✓ This file
├── QUICK_START_CHECKLIST.md
├── ECLIPSE_SETUP_GUIDE.md
├── TESTING_GUIDE_ECLIPSE.md
└── PROJECT_STRUCTURE_TEMPLATE.md
```

---

## Part 6: Common Post-Import Tasks

### Configure Run Configuration
```
Right-click project → Run As → Run Configurations...
  1. Main class: org.dataingest.rfc.server.RFCServerApplication
  2. VM arguments: -Dspring.profiles.active=dev
  3. Apply → Run
```

### Configure Debug Configuration
```
Right-click project → Debug As → Debug Configurations...
  1. Same as Run Configuration above
  2. Apply → Debug
  3. Set breakpoints as needed
```

### Run Maven Commands from Eclipse
```
Right-click project → Run As → Maven build...
  1. Goals: clean install
  2. Or: test
  3. Or: package
```

### Update Maven Dependencies
```
Right-click project → Maven → Update Project
  1. Check: Force Update of Snapshots/Releases
  2. Click: OK
  3. Wait for download
```

---

## Part 7: Verification Checklist

After import, verify:

- [ ] Project appears in Project Explorer
- [ ] No red X errors on project
- [ ] pom.xml file visible and correct
- [ ] src/main/java contains all packages
- [ ] src/test/java contains test packages
- [ ] Maven dependencies downloaded (no errors)
- [ ] Project builds successfully: Ctrl+B
- [ ] JUnit view shows no errors
- [ ] Application starts: Run As → Spring Boot App
- [ ] Health endpoint responds: http://localhost:8080/rfc/actuator/health

---

## Part 8: Next Steps

Once project is successfully imported:

1. **Read Documentation** (10 min)
   - QUICK_START_CHECKLIST.md
   - ECLIPSE_SETUP_GUIDE.md

2. **Configure Application** (15 min)
   - Update `application.properties` with Kafka broker
   - Set feature flags
   - Configure logging

3. **Implement Tasks** (4-8 hours)
   - Follow REMAINING_TASKS.md
   - Complete refactoring phases

4. **Create Tests** (4-8 hours)
   - Follow TESTING_GUIDE_ECLIPSE.md
   - Write unit and integration tests

5. **Build & Deploy** (1-2 hours)
   - Package JAR: `mvn clean package`
   - Deploy to test environment
   - Verify functionality

---

## Quick Reference Commands

### Eclipse Menu Actions
```
File → Import...              Import existing project
File → Exit                   Close Eclipse
Project → Build Project       Build current project (Ctrl+B)
Run → Run As → Spring Boot    Start application
Run → Debug As → Debug        Debug application
Window → Show View → Console  Show console output
Help → Eclipse Marketplace    Install plugins
```

### Maven Commands (in Terminal)
```bash
cd D:\RFC_SERVER\ProjectRFC

mvn clean                      # Clean build
mvn package                    # Build JAR
mvn clean package              # Clean + build
mvn test                       # Run tests
mvn spring-boot:run            # Run application
mvn -X clean package           # Debug mode
```

### Eclipse Shortcuts
```
Ctrl+B           Build project
Ctrl+Shift+O     Organize imports
Ctrl+Shift+F     Format code
F5               Step into (debug)
F6               Step over (debug)
F8               Resume (debug)
Alt+F5           Maven update
```

---

## Support & Troubleshooting

### Check Eclipse Log
```
Window → Show View → Error Log
  Shows Eclipse errors and warnings
  Helpful for debugging import issues
```

### View Maven Console Output
```
Window → Show View → Console
  Shows Maven build output
  Useful for dependency download issues
```

### Validate Maven POM
```
Right-click pom.xml → Validate
  Checks pom.xml syntax
  Reports any configuration errors
```

### Reset Eclipse Workspace
```
1. Close Eclipse
2. Delete: workspace/.metadata/
3. Reopen Eclipse
4. Retry import (slower first time)
```

---

## FAQ

**Q: How long does import take?**
A: First import: 5-10 minutes (downloading 100+ MB dependencies)
   Subsequent: <1 minute

**Q: Do I need internet connection?**
A: Yes, for downloading Maven dependencies from Maven Central Repository

**Q: Can I use a proxy/firewall?**
A: Yes, configure in Window → Preferences → Maven → Repositories

**Q: What if import fails?**
A: See Part 3: Troubleshooting Import Issues

**Q: How do I know import succeeded?**
A: Project appears in Project Explorer with no red X errors

**Q: Can I import multiple times?**
A: Yes, right-click project → Maven → Update Project (Force)

---

## Success Message

Once import completes, you should see:

```
✓ Project 'rfc-server' imported successfully
✓ Maven dependencies resolved (0 errors, 0 warnings)
✓ Project builds successfully (Ctrl+B)
✓ All source packages visible
✓ Application ready to develop and test
```

**You're ready to start developing! 🚀**

---

## Document Information

- **Version:** 1.0.0
- **Created:** 2024-12-18
- **Status:** Ready for Use
- **Estimated Import Time:** 10-15 minutes total
- **Project Ready:** ✅ YES

