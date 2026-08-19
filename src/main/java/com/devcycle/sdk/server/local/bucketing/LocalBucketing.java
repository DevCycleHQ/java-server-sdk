package com.devcycle.sdk.server.local.bucketing;

import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.devcycle.sdk.server.local.utils.ByteConversionUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static io.github.kawamuray.wasmtime.WasmValType.F64;
import static io.github.kawamuray.wasmtime.WasmValType.I32;

public class LocalBucketing {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final int WASM_OBJECT_ID_STRING = 1;
    private final int WASM_OBJECT_ID_UINT8ARRAY = 9;
    Store<Void> store; // WASM compilation environment
    Linker linker; // used to read/write to WASM
    AtomicReference<Memory> memRef; // reference to start of WASM's memory
    private final Set<Integer> pinnedAddresses;
    private final Set<Integer> pinnedEventIds;
    private final HashMap<String, Integer> sdkKeyAddresses;
    private final HashMap<Variable.TypeEnum, Integer> variableTypeMap = new HashMap<>();
    private final Logger logger = Logger.getLogger(LocalBucketing.class.getName());
    private final Map<String, String> configMetadataCache = new HashMap<>();

    private final WasmFunctions.Function2<Integer, Integer, Integer> newFn;
    private final WasmFunctions.Consumer1<Integer> pinFn;
    private final WasmFunctions.Consumer1<Integer> unpinFn;
    private final WasmFunctions.Function1<Integer, Integer> variableForUserPBFn;
    private final WasmFunctions.Consumer2<Integer, Integer> setConfigDataFn;
    private final WasmFunctions.Consumer1<Integer> setPlatformDataFn;
    private final WasmFunctions.Consumer2<Integer, Integer> setClientCustomDataFn;
    private final WasmFunctions.Function2<Integer, Integer, Integer> generateBucketedConfigFn;
    private final WasmFunctions.Consumer3<Integer, Integer, Integer> initEventQueueFn;
    private final WasmFunctions.Consumer3<Integer, Integer, Integer> queueEventFn;
    private final WasmFunctions.Consumer3<Integer, Integer, Integer> queueAggregateEventFn;
    private final WasmFunctions.Function1<Integer, Integer> flushEventQueueFn;
    private final WasmFunctions.Consumer3<Integer, Integer, Integer> onPayloadFailureFn;
    private final WasmFunctions.Consumer2<Integer, Integer> onPayloadSuccessFn;
    private final WasmFunctions.Function1<Integer, Integer> eventQueueSizeFn;
    private final WasmFunctions.Function1<Integer, Integer> getConfigMetadataFn;

    public LocalBucketing() {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        pinnedAddresses = new HashSet<>();
        pinnedEventIds = new HashSet<>();
        sdkKeyAddresses = new HashMap<>();

        store = Store.withoutData();
        linker = new Linker(store.engine());
        memRef = new AtomicReference<>();
        InputStream wasmInput = getClass().getClassLoader().getResourceAsStream("bucketing-lib.release.wasm");
        Module module = null;
        try {
            module = Module.fromBinary(store.engine(), wasmInput.readAllBytes()); // compile the file
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setImportsOnLinker(); // get linker ready to instantiate the file by passing in required imports
        linker.module(store, "", module); // linker now has a runnable instance of the module

        Memory mem = linker.get(store, "", "memory").get().memory();
        memRef.set(mem);

        newFn = WasmFunctions.func(store, export("__new"), I32, I32, I32);
        pinFn = WasmFunctions.consumer(store, export("__pin"), I32);
        unpinFn = WasmFunctions.consumer(store, export("__unpin"), I32);
        variableForUserPBFn = WasmFunctions.func(store, export("variableForUser_PB"), I32, I32);
        setConfigDataFn = WasmFunctions.consumer(store, export("setConfigDataUTF8"), I32, I32);
        setPlatformDataFn = WasmFunctions.consumer(store, export("setPlatformDataUTF8"), I32);
        setClientCustomDataFn = WasmFunctions.consumer(store, export("setClientCustomDataUTF8"), I32, I32);
        generateBucketedConfigFn =
                WasmFunctions.func(store, export("generateBucketedConfigForUserUTF8"), I32, I32, I32);
        initEventQueueFn = WasmFunctions.consumer(store, export("initEventQueue"), I32, I32, I32);
        queueEventFn = WasmFunctions.consumer(store, export("queueEvent"), I32, I32, I32);
        queueAggregateEventFn = WasmFunctions.consumer(store, export("queueAggregateEvent"), I32, I32, I32);
        flushEventQueueFn = WasmFunctions.func(store, export("flushEventQueue"), I32, I32);
        onPayloadFailureFn = WasmFunctions.consumer(store, export("onPayloadFailure"), I32, I32, I32);
        onPayloadSuccessFn = WasmFunctions.consumer(store, export("onPayloadSuccess"), I32, I32);
        eventQueueSizeFn = WasmFunctions.func(store, export("eventQueueSize"), I32, I32);
        getConfigMetadataFn = WasmFunctions.func(store, export("getConfigMetadata"), I32, I32);

        // WASM time seems problematic for getting global values so we'll just hardcode them
        variableTypeMap.put(Variable.TypeEnum.BOOLEAN, 0);
        variableTypeMap.put(Variable.TypeEnum.NUMBER, 1);
        variableTypeMap.put(Variable.TypeEnum.STRING, 2);
        variableTypeMap.put(Variable.TypeEnum.JSON, 3);
    }

    /**
     * Resolves a WASM export once, at construction time.
     *
     * <p>The returned {@link Func} owns a native handle that is only released by
     * {@code Func.dispose()}, so these must not be fetched per call.
     *
     * <p>Resolving here rather than per call moves a missing-export failure from first use to
     * construction. That is deliberate: {@code DevCycleLocalClient.variable} catches
     * {@link Throwable} and falls back to the default value, so a missing export used to be
     * swallowed into silently-defaulted flags for the lifetime of the process. The bundled
     * {@code bucketing-lib.release.wasm} is pinned by the build, so a missing export means a broken
     * build and should fail loudly and immediately.
     *
     * <p>Runtime error behaviour is otherwise unchanged: WASM traps still surface as
     * {@code WasmtimeException} from the calling method, and a trap does not invalidate these
     * bindings. See {@code LocalBucketingErrorHandlingTest}.
     */
    private Func export(String name) {
        return linker.get(store, "", name)
                .orElseThrow(() -> new IllegalStateException(
                        "bucketing-lib.release.wasm is missing the '" + name + "' export"))
                .func();
    }

    private Collection<Extern> setImportsOnLinker() {
        Func dateNowFn = WasmFunctions.wrap(store, F64, () -> {
            return (double) System.currentTimeMillis();
        });
        linker.define(store, "env", "Date.now", Extern.fromFunc(dateNowFn));

        Func consoleLogFn = WasmFunctions.wrap(store, I32, (addr) -> {
            String message = readWasmString(((Number) addr).intValue());
            DevCycleLogger.warning("WASM error: " + message);
        });
        linker.define(store, "env", "console.log", Extern.fromFunc(consoleLogFn));

        Func abortFn = WasmFunctions.wrap(store, I32, I32, I32, I32, (messagePtr, filenamePtr, linenum, colnum) -> {
            String message = readWasmString(((Number) messagePtr).intValue());
            String fileName = readWasmString(((Number) filenamePtr).intValue());
            throw new RuntimeException("Exception in " + fileName + ":" + linenum + " : " + colnum + " " + message);
        });
        linker.define(store, "env", "abort", Extern.fromFunc(abortFn));

        Func seedFn = WasmFunctions.wrap(store, F64, () -> {
            return System.currentTimeMillis() * Math.random();
        });
        linker.define(store, "env", "seed", Extern.fromFunc(seedFn));

        return Arrays.asList(Extern.fromFunc(dateNowFn), Extern.fromFunc(consoleLogFn), Extern.fromFunc(abortFn));
    }

    private int newWasmString(String param) {

        int objectIdString = 1; // id 1 represents string class in wasm

        byte[] paramBytes = param.getBytes(StandardCharsets.UTF_8);
        int paramAddress = newFn.call(paramBytes.length * 2, objectIdString); // allocate memory in store for a string with this length and get start address

        ByteBuffer buf = memRef.get().buffer(store);
        for (int i = 0; i < paramBytes.length; i++) {
            buf.put(paramAddress + (i * 2), paramBytes[i]); // write each byte of string starting at address
        }

        return paramAddress;
    }

    private String readWasmString(int startAddress) {
        ByteBuffer buf = memRef.get().buffer(store);

        // objects in wasm memory have a 20 byte header before the start pointer
        // the 4 bytes right before the object pointer store the length of the object as an unsigned int
        // see assemblyscript.org/runtime.html#memory-layout
        byte[] headerBytes = {buf.get(startAddress - 1), buf.get(startAddress - 2), buf.get(startAddress - 3), buf.get(startAddress - 4)};
        long stringLength = ByteConversionUtils.getUnsignedInt(headerBytes);
        String result = "";
        for (int i = 0; i < stringLength; i += 2) { // +=2 because the data is formatted as WTF-16, not UTF-8
            result += (char) buf.get(startAddress + i); // read each byte of string starting at address
        }

        return result;
    }

    private int newUint8ArrayParameter(byte[] paramData) {
        int length = paramData.length;

        int headerAddr = newFn.call(12, WASM_OBJECT_ID_UINT8ARRAY);
        try {
            pinParameter(headerAddr);
            int dataBufferAddr = newFn.call(length, WASM_OBJECT_ID_STRING);

            byte[] headerData = new byte[12];
            byte[] bufferAddrBytes = ByteConversionUtils.intToBytesLittleEndian(dataBufferAddr);
            byte[] lengthBytes = ByteConversionUtils.intToBytesLittleEndian(length << 0);
            // Into the header need to write 12 bytes
            for (int i = 0; i < 4; i++) {
                // 0-3 = buffer address,little endian
                headerData[i] = bufferAddrBytes[i];
                // 4-7 = buffer address again, little endian
                headerData[i + 4] = bufferAddrBytes[i];
                // 8-11 = length, little endian, aligned 0
                headerData[i + 8] = lengthBytes[i];
            }

            ByteBuffer buf = memRef.get().buffer(store);

            // write the header to the WASM memory
            for (int i = 0; i < headerData.length; i++) {
                buf.put(headerAddr + i, headerData[i]); // write each byte of string starting at address
            }

            // write the param data into WASM memory
            for (int i = 0; i < length; i++) {
                buf.put(dataBufferAddr + i, paramData[i]);
            }
        } finally {
            unpinParameter(headerAddr);
        }
        return headerAddr;
    }

    private byte[] readFromWasmMemory(int address, int length) {
        ByteBuffer buf = memRef.get().buffer(store);
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.get(address + i);
        }
        return data;
    }

    private byte[] readAssemblyScriptUint8Array(int address) {
        // The header is 12 bytes long, need to pull out the location of the array's data buffer
        // and the length of the data buffer
        byte[] bufferDataAddressBytes = readFromWasmMemory(address, 4);
        int bufferAddress = ByteConversionUtils.bytesToIntLittleEndian(bufferDataAddressBytes);

        byte[] lengthAddressBytes = readFromWasmMemory(address + 8, 4);
        int dataLength = ByteConversionUtils.bytesToIntLittleEndian(lengthAddressBytes);

        byte[] bufferData = readFromWasmMemory(bufferAddress, dataLength);
        return bufferData;
    }

    public synchronized void storeConfig(String sdkKey, String config) {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int configAddress = newUint8ArrayParameter(config.getBytes(StandardCharsets.UTF_8));

        setConfigDataFn.accept(sdkKeyAddress, configAddress);
        configMetadataCache.put(sdkKey, internalGetConfigMetadata(sdkKeyAddress));
    }

    public synchronized void setPlatformData(String platformData) {
        unPinAllEventIds();
        int platformDataAddress = newUint8ArrayParameter(platformData.getBytes(StandardCharsets.UTF_8));
        setPlatformDataFn.accept(platformDataAddress);
    }

    public synchronized void setClientCustomData(String sdkKey, String customData) {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int customDataAddress = newUint8ArrayParameter(customData.getBytes(StandardCharsets.UTF_8));
        setClientCustomDataFn.accept(sdkKeyAddress, customDataAddress);
    }

    public synchronized BucketedUserConfig generateBucketedConfig(String sdkKey, DevCycleUser user) throws JsonProcessingException {
        unPinAllEventIds();
        String userString = OBJECT_MAPPER.writeValueAsString(user);

        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int userAddress = newUint8ArrayParameter(userString.getBytes(StandardCharsets.UTF_8));

        int resultAddress = generateBucketedConfigFn.call(sdkKeyAddress, userAddress);

        byte[] bucketConfigBytes = readAssemblyScriptUint8Array(resultAddress);
        String bucketedConfigString = new String(bucketConfigBytes, StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        BucketedUserConfig config = objectMapper.readValue(bucketedConfigString, BucketedUserConfig.class);
        return config;
    }

    public synchronized byte[] getVariableForUserProtobuf(byte[] serializedParams) {
        int paramsAddr = newUint8ArrayParameter(serializedParams);

        int variableAddress = variableForUserPBFn.call(paramsAddr);

        byte[] varBytes = null;
        if (variableAddress > 0) {
            varBytes = readAssemblyScriptUint8Array(variableAddress);
        }

        return varBytes;
    }

    public synchronized void initEventQueue(String sdkKey, String clientUUID, String options) {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int clientUUIDAddress = newWasmString(clientUUID);
        int optionsAddress = newWasmString(options);

        initEventQueueFn.accept(sdkKeyAddress, clientUUIDAddress, optionsAddress);
    }

    public synchronized void queueEvent(String sdkKey, String user, String event) {
        unPinAllEventIds();
        int sdkKeyAddress = newWasmString(sdkKey);
        int userAddress = getPinnedEventId(user);
        int eventAddress = newWasmString(event);

        queueEventFn.accept(sdkKeyAddress, userAddress, eventAddress);
    }

    public synchronized void queueAggregateEvent(String sdkKey, String event, String variableVariationMap) {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int eventAddress = getPinnedEventId(event);
        int variableVariationMapAddress = newWasmString(variableVariationMap);

        queueAggregateEventFn.accept(sdkKeyAddress, eventAddress, variableVariationMapAddress);
    }

    public synchronized FlushPayload[] flushEventQueue(String sdkKey) throws JsonProcessingException {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);

        int resultAddress = flushEventQueueFn.call(sdkKeyAddress);
        String flushPayloadsStr = readWasmString(resultAddress);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); //2022-09-08T20:16:31.741Z
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.setDateFormat(df);

        FlushPayload[] payloads = objectMapper.readValue(flushPayloadsStr, FlushPayload[].class);

        return payloads;
    }

    public synchronized void onPayloadFailure(String sdkKey, String payloadId, boolean retryable) {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int payloadIdAddress = newWasmString(payloadId);

        onPayloadFailureFn.accept(sdkKeyAddress, payloadIdAddress, retryable ? 1 : 0);
    }

    public synchronized void onPayloadSuccess(String sdkKey, String payloadId) {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int payloadIdAddress = newWasmString(payloadId);

        onPayloadSuccessFn.accept(sdkKeyAddress, payloadIdAddress);
    }

    public synchronized int getEventQueueSize(String sdkKey) {
        unPinAllEventIds();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);

        return eventQueueSizeFn.call(sdkKeyAddress);
    }

    private void pinParameter(int address) {
        pinFn.accept(address);
    }

    private void unpinParameter(int address) {
        unpinFn.accept(address);
    }

    private void unPinAllEventIds() {
        for (int address : pinnedEventIds) {
            unpinParameter(address);
        }
        pinnedEventIds.clear();
    }

    private void unPinAllParameters() {
        for (int address : pinnedAddresses) {
            unpinParameter(address);
        }
        for (int address : pinnedEventIds) {
            unpinParameter(address);
        }
        pinnedAddresses.clear();
        pinnedEventIds.clear();
    }

    private int getPinnedParameter(String param) {
        int address = newWasmString(param);
        pinParameter(address);
        pinnedAddresses.add(address);
        return address;
    }

    private int getPinnedEventId(String param) {
        int address = newWasmString(param);
        pinParameter(address);
        pinnedEventIds.add(address);
        return address;
    }

    private int getSDKKeyAddress(String sdkKey) {
        if (!sdkKeyAddresses.containsKey(sdkKey)) {
            int sdkKeyAddress = newWasmString(sdkKey);
            pinParameter(sdkKeyAddress);
            sdkKeyAddresses.put(sdkKey, sdkKeyAddress);
        }

        return sdkKeyAddresses.get(sdkKey);
    }

    public String getConfigMetadata(String sdkKey) {
        if (configMetadataCache.containsKey(sdkKey)) {
            return configMetadataCache.get(sdkKey);
        } else {
            int sdkKeyAddress = getSDKKeyAddress(sdkKey);
            String metadata = internalGetConfigMetadata(sdkKeyAddress);
            configMetadataCache.put(sdkKey, metadata);
            return metadata;
        }
    }

    private String internalGetConfigMetadata(int sdkKeyAddress) {
        int resultAddress = getConfigMetadataFn.call(sdkKeyAddress);
        return readWasmString(resultAddress);
    }
}

