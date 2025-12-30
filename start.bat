@echo off
REM Start IDoc Capture Server (Windows)

REM Check if SAP native library exists


REM Add lib directory to PATH for SAP JCo native library
set PATH=%CD%\lib;%PATH%

REM Run the standalone JAR (SAP libraries loaded from manifest Class-Path)
java -jar idoc-capture-standalone.jar idoc_capture.properties

echo IDoc Capture Server stopped
pause
