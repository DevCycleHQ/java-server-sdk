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

    public LocalBucketing() {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

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

    private static long getUnsignedInt(byte[] data) {
        long result = 0;

        for (int i = 0; i < data.length; i++) {
            result = (result << 8) + (data[i] & 0xFF);
        }

        return result;
    }


    public void storeConfig(String token, String config) {
        int tokenAddress = newWasmString(token);
        int configAddress = newWasmString(config);

        Func setConfigDataPtr = linker.get(store, "", "setConfigData").get().func();
        WasmFunctions.Consumer2<Integer, Integer> fn = WasmFunctions.consumer(store, setConfigDataPtr, I32, I32);
        fn.accept(tokenAddress, configAddress);
    }

    public void setPlatformData(String platformData) {
        int platformDataAddress = newWasmString(platformData);

        Func setPlatformDataPtr = linker.get(store, "", "setPlatformData").get().func();
        WasmFunctions.Consumer1<Integer> fn = WasmFunctions.consumer(store, setPlatformDataPtr, I32);
        fn.accept(platformDataAddress);
    }

    public BucketedUserConfig generateBucketedConfig(String token, User user) throws JsonProcessingException {
        String userString = OBJECT_MAPPER.writeValueAsString(user);

        int tokenAddress = newWasmString(token);
        int userAddress = newWasmString(userString);

        Func generateBucketedConfigForUserPtr = linker.get(store, "", "generateBucketedConfigForUser").get().func();
        WasmFunctions.Function2<Integer, Integer, Integer> generateBucketedConfigForUser = WasmFunctions.func(
                store, generateBucketedConfigForUserPtr, I32, I32, I32);

        int resultAddress = generateBucketedConfigForUser.call(tokenAddress, userAddress);
        String bucketedConfigString = readWasmString(resultAddress);

        ObjectMapper objectMapper = new ObjectMapper();
        BucketedUserConfig config = objectMapper.readValue(bucketedConfigString, BucketedUserConfig.class);
        return config;
    }

    public void initEventQueue(String token, String options) {
        int tokenAddress = newWasmString(token);
        int optionsAddress = newWasmString(options);

        Func initEventQueuePtr = linker.get(store, "", "initEventQueue").get().func();
        WasmFunctions.Consumer2<Integer, Integer> fn = WasmFunctions.consumer(store, initEventQueuePtr, I32, I32);
        fn.accept(tokenAddress, optionsAddress);
    }

    public void queueEvent(String token, String user, String event) {
        int tokenAddress = newWasmString(token);
        int userAddress = newWasmString(user);
        int eventAddress = newWasmString(event);

        Func queueEventPtr = linker.get(store, "", "queueEvent").get().func();
        WasmFunctions.Consumer3<Integer, Integer, Integer> fn = WasmFunctions.consumer(store, queueEventPtr, I32, I32, I32);
        fn.accept(tokenAddress, userAddress, eventAddress);
    }

    public void queueAggregateEvent(String token, String event, String variableVariationMap) {
        int tokenAddress = newWasmString(token);
        int eventAddress = newWasmString(event);
        int variableVariationMapAddress = newWasmString(variableVariationMap);

        Func queueAggregateEventPtr = linker.get(store, "", "queueAggregateEvent").get().func();
        WasmFunctions.Consumer3<Integer, Integer, Integer> fn = WasmFunctions.consumer(store, queueAggregateEventPtr, I32, I32, I32);
        fn.accept(tokenAddress, eventAddress, variableVariationMapAddress);
    }

    public FlushPayload[] flushEventQueue(String token) throws JsonProcessingException {
        int tokenAddress = newWasmString(token);

        Func flushEventQueuePtr = linker.get(store, "", "flushEventQueue").get().func();
        WasmFunctions.Function1<Integer, Integer> fn = WasmFunctions.func(
                store, flushEventQueuePtr, I32, I32);

        int resultAddress = fn.call(tokenAddress);
        String flushPayloadsStr = readWasmString(resultAddress);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); //2022-09-08T20:16:31.741Z
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.setDateFormat(df);

        FlushPayload[] payloads = objectMapper.readValue(flushPayloadsStr, FlushPayload[].class);

        return payloads;
    }

    public void onPayloadFailure(String token, String payloadId, boolean retryable) {
        int tokenAddress = newWasmString(token);
        int payloadIdAddress = newWasmString(payloadId);

        Func onPayloadFailurePtr = linker.get(store, "", "onPayloadFailure").get().func();
        WasmFunctions.Consumer3<Integer, Integer, Integer> fn = WasmFunctions.consumer(store, onPayloadFailurePtr, I32, I32, I32);
        fn.accept(tokenAddress, payloadIdAddress, retryable ? 1 : 0);
    }

    public void onPayloadSuccess(String token, String payloadId) {
        int tokenAddress = newWasmString(token);
        int payloadIdAddress = newWasmString(payloadId);

        Func onPayloadSuccessPtr = linker.get(store, "", "onPayloadSuccess").get().func();
        WasmFunctions.Consumer2<Integer, Integer> fn = WasmFunctions.consumer(store, onPayloadSuccessPtr, I32, I32);
        fn.accept(tokenAddress, payloadIdAddress);
    }

    public int getEventQueueSize(String token) {
        int tokenAddress = newWasmString(token);

        Func getEventQueueSizePtr = linker.get(store, "", "eventQueueSize").get().func();
        WasmFunctions.Function1<Integer, Integer> getEventQueueSize = WasmFunctions.func(
                store, getEventQueueSizePtr, I32, I32);

        return getEventQueueSize.call(tokenAddress);
    }
}

