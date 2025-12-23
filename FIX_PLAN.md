# IDOC Handler Registration Issue - Fix Plan

## Current Status
- **Problem**: `getCallHandler()` never invoked despite correct registration
- **Evidence**: TID handler IS called, but function handler is NOT
- **Proof it should work**: Talend uses identical SAP config and works fine
- **Conclusion**: Issue is in OUR code, not SAP/config

## Current Implementation
- UnifiedIDOCReceiver: Implements both JCoServerFunctionHandler + JCoServerFunctionHandlerFactory
- Registration: DefaultServerHandlerFactory.FunctionHandlerFactory pattern
- Order: CallHandlerFactory FIRST (line 252), then TIDHandler (line 257) ✓

## Test Plan (IMMEDIATE)

1. **Run and trigger IDOC send from SAP**
   ```bash
   mvn clean compile && mvn spring-boot:run
   ```

2. **Look for in logs**:
   - ✓ "!!!!! Successfully registered IDOC_INBOUND_ASYNCHRONOUS handler" → Registration worked
   - ✓ "!!!!! getCallHandler() CALLED !!!!!!..." → Factory method invoked
   - ✓ "!!!!! handleRequest() CALLED !!!!!!..." → Handler processing started
   - ? See TID/Function Name/Sender System output → Confirms data arrived

## If getCallHandler() NOT called (Most Likely)

**Root Cause Analysis**:
- DefaultServerHandlerFactory.FunctionHandlerFactory might not be the right API
- OR SAP JCo expects handler registered differently
- OR there's a reference/classloader issue

**Fix Attempt #1**: Try DefaultServerHandlerFactory static method
```java
// Instead of: new DefaultServerHandlerFactory.FunctionHandlerFactory()
JCoServerCallHandlerFactory factory = DefaultServerHandlerFactory.createCallHandlerFactory();
factory.registerHandler("IDOC_INBOUND_ASYNCHRONOUS", unifiedIDOCReceiver);
rfcServer.setCallHandlerFactory(factory);
```

**Fix Attempt #2**: Register handler directly without factory pre-registration
```java
// Pass the handler directly as CallHandlerFactory (implements both interfaces)
rfcServer.setCallHandlerFactory(unifiedIDOCReceiver);
```

**Fix Attempt #3**: Check Talend's actual pattern more carefully
- Talend uses ISAPServerFunction (extends JCoServerFunctionHandler)
- Maybe need to implement additional interface or method

## If handleRequest() IS called ✓

**Immediate Next Steps**:
1. Verify IDOC_CONTROL_REC_40 table extraction works
2. Verify IDOC_DATA_REC_40 table extraction works
3. Add IDocKafkaPublisher.publishSAPDocument() call
4. Test end-to-end IDOC → Kafka publishing

## Files to Modify (if needed)
- `SAPRFCServerImpl.java` - Handler registration pattern (lines 225-258)
- `UnifiedIDOCReceiver.java` - If additional interface/method needed
