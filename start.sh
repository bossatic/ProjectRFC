#!/bin/bash
# Start IDoc Capture Server (Linux/macOS)

# Check if SAP native library exists
if [ ! -f "lib/libsapjco3.so" ] && [ ! -f "lib/libsapjco3.jnilib" ]; then
    echo "ERROR: SAP native library not found in lib/"
    echo "Please ensure libsapjco3.so (Linux) or libsapjco3.jnilib (macOS) is in the lib/ directory"
    exit 1
fi

# Set library path for SAP JCo native libraries
export LD_LIBRARY_PATH=./lib:$LD_LIBRARY_PATH
export DYLD_LIBRARY_PATH=./lib:$DYLD_LIBRARY_PATH

# Run the standalone JAR (SAP libraries loaded from manifest Class-Path)
java -jar idoc-capture-standalone.jar idoc_capture.properties

echo "IDoc Capture Server stopped"
