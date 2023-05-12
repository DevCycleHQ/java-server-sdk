package com.devcycle.sdk.server.local.bucketing;

import static io.github.kawamuray.wasmtime.WasmValType.F64;
import static io.github.kawamuray.wasmtime.WasmValType.I32;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.kawamuray.wasmtime.*;
import io.github.kawamuray.wasmtime.Module;

public class LocalBucketing {
    Store<Void> store; // WASM compilation environment
    Linker linker; // used to read/write to WASM
    AtomicReference<Memory> memRef; // reference to start of WASM's memory
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Set<Integer> pinnedAddresses;
    private HashMap<String, Integer> sdkKeyAddresses;

    private HashMap<Variable.TypeEnum, Integer> variableTypeMap = new HashMap<Variable.TypeEnum, Integer>();

    private final int WASM_OBJECT_ID_STRING = 1;
    private final int WASM_OBJECT_ID_UINT8ARRAY = 9;

    public LocalBucketing() {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        pinnedAddresses = new HashSet<>();
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

        // WASM time seems problematic for getting global values so we'll just hardcode them
        variableTypeMap.put(Variable.TypeEnum.BOOLEAN, 0);
        variableTypeMap.put(Variable.TypeEnum.NUMBER, 1);
        variableTypeMap.put(Variable.TypeEnum.STRING, 2);
        variableTypeMap.put(Variable.TypeEnum.JSON, 3);
    }

    private Collection<Extern> setImportsOnLinker() {
        Func dateNowFn = WasmFunctions.wrap(store, F64, () -> {
            return (double) System.currentTimeMillis();
        });
        linker.define("env", "Date.now", Extern.fromFunc(dateNowFn));

        Func consoleLogFn = WasmFunctions.wrap(store, I32, (addr) -> {
            String message = readWasmString(((Number) addr).intValue());
            System.out.println(message);
        });
        linker.define("env", "console.log", Extern.fromFunc(consoleLogFn));

        Func abortFn = WasmFunctions.wrap(store, I32, I32, I32, I32, (messagePtr, filenamePtr, linenum, colnum) -> {
            String message = readWasmString(((Number) messagePtr).intValue());
            String fileName = readWasmString(((Number) filenamePtr).intValue());
            throw new RuntimeException("Exception in " + fileName + ":" + linenum + " : " + colnum + " " + message);
        });
        linker.define("env", "abort", Extern.fromFunc(abortFn));

        Func seedFn = WasmFunctions.wrap(store, F64, () -> {
            return System.currentTimeMillis() * Math.random();
        });
        linker.define("env", "seed", Extern.fromFunc(seedFn));

        return Arrays.asList(Extern.fromFunc(dateNowFn), Extern.fromFunc(consoleLogFn), Extern.fromFunc(abortFn));
    }

    private int newWasmString(String param) {

        int objectIdString = 1; // id 1 represents string class in wasm

        Func __newPtr = linker.get(store, "", "__new").get().func(); // get pointer to __new function
        WasmFunctions.Function2<Integer, Integer, Integer> __new = WasmFunctions.func(
                store, __newPtr, I32, I32, I32); // load __new function

        byte[] paramBytes = param.getBytes(StandardCharsets.UTF_8);
        int paramAddress = __new.call(paramBytes.length * 2, objectIdString); // allocate memory in store for a string with this length and get start address

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
        long stringLength = getUnsignedInt(headerBytes);
        String result = "";
        for (int i = 0; i < stringLength; i += 2) { // +=2 because the data is formatted as WTF-16, not UTF-8
            result += (char) buf.get(startAddress + i); // read each byte of string starting at address
        }

        return result;
    }

    public static byte[] intToBytesLittleEndian(int value) {
        byte[] encodedValue = new byte[4];
        encodedValue[3] = (byte) (value >> 8 * 3);
        encodedValue[2] = (byte) (value >> 8 * 2);
        encodedValue[1] = (byte) (value >> 8);
        encodedValue[0] = (byte) value;
        return encodedValue;
    }

    public static int bytesToIntLittleEndian(byte[] bytes) {
        int i = 4;
        int value = bytes[--i];
        while (--i >= 0) {
            value <<= 8;
            value |= bytes[i] & 0xFF;
        }
        return value;
    }

    private int newUint8ArrayParameter(byte[] paramData)
    {
        int length = paramData.length;

        Func __newPtr = linker.get(store, "", "__new").get().func(); // get pointer to __new function
        WasmFunctions.Function2<Integer, Integer, Integer> __new = WasmFunctions.func(
                store, __newPtr, I32, I32, I32); // load __new function

        int headerAddr = __new.call(12, WASM_OBJECT_ID_UINT8ARRAY);
        try
        {
            pinParameter(headerAddr);
            int dataBufferAddr = __new.call(length, WASM_OBJECT_ID_STRING);

            byte[] headerData = new byte[12];
            byte[] bufferAddrBytes = intToBytesLittleEndian(dataBufferAddr);
            byte[] lengthBytes = intToBytesLittleEndian(length << 0);
            // Into the header need to write 12 bytes
            for(int i = 0; i < 4; i++)
            {
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
            for(int i = 0; i < length; i++)
            {
                buf.put(dataBufferAddr + i, paramData[i]);
            }
        }
        finally
        {
            unpinParameter(headerAddr);
        }
        return headerAddr;
    }

    private byte[] readFromWasmMemory(int address, int length)
    {
        ByteBuffer buf = memRef.get().buffer(store);
        byte[] data = new byte[length];
        for(int i = 0; i < length; i++)
        {
            data[i] = buf.get(address + i);
        }
        return data;
    }

    private byte[] readAssemblyScriptUint8Array(int address)
    {
        // The header is 12 bytes long, need to pull out the location of the array's data buffer
        // and the length of the data buffer
        byte[] bufferDataAddressBytes = readFromWasmMemory(address, 4);
        int bufferAddress = bytesToIntLittleEndian(bufferDataAddressBytes);

        byte[] lengthAddressBytes = readFromWasmMemory(address + 8, 4);
        int dataLength = bytesToIntLittleEndian(lengthAddressBytes);

        byte[] bufferData = readFromWasmMemory(bufferAddress, dataLength);
        return bufferData;
    }

    private static long getUnsignedInt(byte[] data) {
        long result = 0;

        for (int i = 0; i < data.length; i++) {
            result = (result << 8) + (data[i] & 0xFF);
        }

        return result;
    }

    public void storeConfig(String sdkKey, String config) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int configAddress = newWasmString(config);

        Func setConfigDataPtr = linker.get(store, "", "setConfigData").get().func();
        WasmFunctions.Consumer2<Integer, Integer> fn = WasmFunctions.consumer(store, setConfigDataPtr, I32, I32);
        fn.accept(sdkKeyAddress, configAddress);
    }

    public void setPlatformData(String platformData) {
        unpinAll();
        int platformDataAddress = newWasmString(platformData);

        Func setPlatformDataPtr = linker.get(store, "", "setPlatformData").get().func();
        WasmFunctions.Consumer1<Integer> fn = WasmFunctions.consumer(store, setPlatformDataPtr, I32);
        fn.accept(platformDataAddress);
    }

    public void setClientCustomData(String sdkKey, String customData) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int customDataAddress = newWasmString(customData);

        Func setCustomClientDataPtr = linker.get(store, "", "setClientCustomData").get().func();
        WasmFunctions.Consumer2<Integer, Integer> fn = WasmFunctions.consumer(store, setCustomClientDataPtr, I32, I32);
        fn.accept(sdkKeyAddress, customDataAddress);
    }

    public BucketedUserConfig generateBucketedConfig(String sdkKey, User user) throws JsonProcessingException {
        unpinAll();
        String userString = OBJECT_MAPPER.writeValueAsString(user);

        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int userAddress = newWasmString(userString);

        Func generateBucketedConfigForUserPtr = linker.get(store, "", "generateBucketedConfigForUser").get().func();
        WasmFunctions.Function2<Integer, Integer, Integer> generateBucketedConfigForUser = WasmFunctions.func(
                store, generateBucketedConfigForUserPtr, I32, I32, I32);

        int resultAddress = generateBucketedConfigForUser.call(sdkKeyAddress, userAddress);
        String bucketedConfigString = readWasmString(resultAddress);

        ObjectMapper objectMapper = new ObjectMapper();
        BucketedUserConfig config = objectMapper.readValue(bucketedConfigString, BucketedUserConfig.class);
        return config;
    }

    public String getVariable(String sdkKey, User user, String key, Variable.TypeEnum variableTypeEnum, boolean shouldTrackEvent) throws JsonProcessingException {
        // need some kind of mutex?
        String userString = OBJECT_MAPPER.writeValueAsString(user);

        int wasmVariableType = this.variableTypeMap.get(variableTypeEnum);

        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int userAddress = newWasmString(userString);
        int keyAddress = newWasmString(key);

        Func getVariablePtr = linker.get(store, "", "variableForUser").get().func();
        WasmFunctions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> variableForUser = WasmFunctions.func(
                store, getVariablePtr, I32, I32, I32, I32, I32, I32);

        int resultAddress = variableForUser.call(sdkKeyAddress, userAddress, keyAddress, wasmVariableType, shouldTrackEvent ? 1 : 0);
        if (resultAddress == 0){
            return null;
        }
        String variableString = readWasmString(resultAddress);
        return variableString;
    }

    public void initEventQueue(String sdkKey, String options) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int optionsAddress = newWasmString(options);

        Func initEventQueuePtr = linker.get(store, "", "initEventQueue").get().func();
        WasmFunctions.Consumer2<Integer, Integer> fn = WasmFunctions.consumer(store, initEventQueuePtr, I32, I32);
        fn.accept(sdkKeyAddress, optionsAddress);
    }

    public void queueEvent(String sdkKey, String user, String event) {
        unpinAll();
        int sdkKeyAddress = newWasmString(sdkKey);
        int userAddress = getPinnedParameter(user);
        int eventAddress = newWasmString(event);

        Func queueEventPtr = linker.get(store, "", "queueEvent").get().func();
        WasmFunctions.Consumer3<Integer, Integer, Integer> fn = WasmFunctions.consumer(store, queueEventPtr, I32, I32, I32);
        fn.accept(sdkKeyAddress, userAddress, eventAddress);
    }

    public void queueAggregateEvent(String sdkKey, String event, String variableVariationMap) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int eventAddress = getPinnedParameter(event);
        int variableVariationMapAddress = newWasmString(variableVariationMap);

        Func queueAggregateEventPtr = linker.get(store, "", "queueAggregateEvent").get().func();
        WasmFunctions.Consumer3<Integer, Integer, Integer> fn = WasmFunctions.consumer(store, queueAggregateEventPtr, I32, I32, I32);
        fn.accept(sdkKeyAddress, eventAddress, variableVariationMapAddress);
    }

    public FlushPayload[] flushEventQueue(String sdkKey) throws JsonProcessingException {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);

        Func flushEventQueuePtr = linker.get(store, "", "flushEventQueue").get().func();
        WasmFunctions.Function1<Integer, Integer> fn = WasmFunctions.func(
                store, flushEventQueuePtr, I32, I32);

        int resultAddress = fn.call(sdkKeyAddress);
        String flushPayloadsStr = readWasmString(resultAddress);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); //2022-09-08T20:16:31.741Z
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.setDateFormat(df);

        FlushPayload[] payloads = objectMapper.readValue(flushPayloadsStr, FlushPayload[].class);

        return payloads;
    }

    public void onPayloadFailure(String sdkKey, String payloadId, boolean retryable) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int payloadIdAddress = newWasmString(payloadId);

        Func onPayloadFailurePtr = linker.get(store, "", "onPayloadFailure").get().func();
        WasmFunctions.Consumer3<Integer, Integer, Integer> fn = WasmFunctions.consumer(store, onPayloadFailurePtr, I32, I32, I32);
        fn.accept(sdkKeyAddress, payloadIdAddress, retryable ? 1 : 0);
    }

    public void onPayloadSuccess(String sdkKey, String payloadId) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int payloadIdAddress = newWasmString(payloadId);

        Func onPayloadSuccessPtr = linker.get(store, "", "onPayloadSuccess").get().func();
        WasmFunctions.Consumer2<Integer, Integer> fn = WasmFunctions.consumer(store, onPayloadSuccessPtr, I32, I32);
        fn.accept(sdkKeyAddress, payloadIdAddress);
    }

    public int getEventQueueSize(String sdkKey) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);

        Func getEventQueueSizePtr = linker.get(store, "", "eventQueueSize").get().func();
        WasmFunctions.Function1<Integer, Integer> getEventQueueSize = WasmFunctions.func(
                store, getEventQueueSizePtr, I32, I32);

        return getEventQueueSize.call(sdkKeyAddress);
    }

    private void pinParameter(int address) {
        Func pinPtr = linker.get(store, "", "__pin").get().func();
        WasmFunctions.Consumer1<Integer> pin = WasmFunctions.consumer(store, pinPtr, I32);
        pin.accept(address);
    }

    private void unpinParameter(int address) {
        Func unpinPtr = linker.get(store, "", "__unpin").get().func();
        WasmFunctions.Consumer1<Integer> unpin = WasmFunctions.consumer(store, unpinPtr, I32);
        unpin.accept(address);
    }

    private void unpinAll() {
        for(int address : pinnedAddresses) {
            unpinParameter(address);
        }
        pinnedAddresses.clear();
    }

    private int getPinnedParameter(String param) {
        int address = newWasmString(param);
        pinParameter(address);
        pinnedAddresses.add(address);
        return address;
    }

    private int getSDKKeyAddress(String sdkKey) {
        if(!sdkKeyAddresses.containsKey(sdkKey)) {
            int sdkKeyAddress = newWasmString(sdkKey);
            pinParameter(sdkKeyAddress);
            sdkKeyAddresses.put(sdkKey, sdkKeyAddress);
        }

        return sdkKeyAddresses.get(sdkKey);
    }
}

