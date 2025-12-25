# Simple IDoc Capture Program

A standalone Java program that receives IDocs from SAP and stores them as XML files on disk.

## Features

- ✅ Receives IDocs from SAP systems
- ✅ Automatically saves each IDoc as an XML file
- ✅ Files named with timestamp and transaction ID
- ✅ Simple console output showing progress
- ✅ Based on SAP's official IDocServerExample

## Directory Structure

```
idocs/                          # Output directory for captured IDocs
  ├── 20231215_143022_TID123.xml
  ├── 20231215_143045_TID124.xml
  └── ...
```

## Configuration

Before running, you need to configure the SAP server connection. Create or update the JCo destination configuration file.

### Option 1: Using jcoDestination file

Create a file named `IDOC_SERVER.jcoDestination` in your working directory:

```properties
# SAP Gateway Connection
jco.server.gwhost=your-sap-gateway-host
jco.server.gwserv=your-gateway-service
jco.server.progid=YOUR_PROGRAM_ID
jco.server.repository_destination=SAP_SYSTEM
jco.server.connection_count=2

# Optional: Connection pool settings
jco.server.max_startup_delay=0
```

### Option 2: Programmatic Configuration

Alternatively, you can use `SAPServerDataProvider` (already in your project) to configure the server programmatically.

## Running the Program

### Compile and Run

```bash
# Compile
javac -cp "lib/*" src/main/java/org/dataingest/rfc/server/SimpleIDocCapture.java

# Run
java -cp "lib/*:src/main/java" org.dataingest.rfc.server.SimpleIDocCapture
```

### Using Maven

```bash
mvn exec:java -Dexec.mainClass="org.dataingest.rfc.server.SimpleIDocCapture"
```

## Output

When an IDoc is received, you'll see:

```
→ Received IDoc(s):
  Transaction ID: A1B2C3D4E5F6
  Number of IDocs: 1
  Saving to: idocs/20231215_143022_A1B2C3D4E5F6.xml
  ✓ IDoc saved successfully!
```

## XML File Format

The captured IDocs are saved in SAP's standard IDoc XML format, which includes:

- Control record (EDI_DC40)
- All data segments
- Field values and metadata

Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<IDOC BEGIN="1">
  <EDI_DC40 SEGMENT="1">
    <TABNAM>EDI_DC40</TABNAM>
    <MANDT>100</MANDT>
    <DOCNUM>0000000001234567</DOCNUM>
    <MESTYP>ORDERS</MESTYP>
    ...
  </EDI_DC40>
  <E1EDKA1 SEGMENT="1">
    ...
  </E1EDKA1>
</IDOC>
```

## Customization

You can easily customize the program by editing `SimpleIDocCapture.java`:

- **Output directory**: Change the `OUTPUT_DIR` constant (default: "idocs")
- **Server name**: Change the `SERVER_NAME` constant (default: "IDOC_SERVER")
- **File naming**: Modify the filename pattern in `SimpleIDocReceiveHandler.handleRequest()`
- **XML formatting**: Change `IDocXMLProcessor.RENDER_WITH_TABS_AND_CRLF` to other options

## Troubleshooting

### Server doesn't start
- Check that the SAP Gateway is reachable
- Verify the Program ID is registered in SAP (SM59/SMGW)
- Ensure JCo libraries are in the classpath

### No IDocs received
- Verify the Program ID matches what SAP is sending to
- Check SAP transaction WE21 for partner profile configuration
- Review SAP transaction WE02/WE05 for IDoc monitoring

### Files not created
- Check write permissions for the output directory
- Verify the OUTPUT_DIR path is correct

## Dependencies

- SAP JCo (Java Connector) library
- SAP IDoc library (sapidoc3.jar)

## See Also

- SAP IDoc examples: `SapIDOC/examples/com/sap/conn/idoc/examples/`
- Your existing IDoc implementation: `src/main/java/org/dataingest/rfc/server/idoc/`
