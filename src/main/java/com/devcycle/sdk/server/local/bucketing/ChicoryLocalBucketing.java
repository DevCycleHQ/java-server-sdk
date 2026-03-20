package com.devcycle.sdk.server.local.bucketing;

import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.devcycle.sdk.server.local.utils.ByteConversionUtils;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static com.dylibso.chicory.wasm.types.ValType.F64;
import static com.dylibso.chicory.wasm.types.ValType.I32;

final class ChicoryLocalBucketing implements LocalBucketingBackend {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final int WASM_OBJECT_ID_STRING = 1;
    private final int WASM_OBJECT_ID_UINT8ARRAY = 9;
    private final Instance instance;
    private final Set<Integer> pinnedAddresses;
    private final HashMap<String, Integer> sdkKeyAddresses;
    @SuppressWarnings("unused")
    private final HashMap<com.devcycle.sdk.server.common.model.Variable.TypeEnum, Integer> variableTypeMap =
            new HashMap<>();

    ChicoryLocalBucketing() {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        pinnedAddresses = new HashSet<>();
        sdkKeyAddresses = new HashMap<>();

        InputStream wasmInput = getClass().getClassLoader().getResourceAsStream("bucketing-lib.release.wasm");
        if (wasmInput == null) {
            throw new RuntimeException("bucketing-lib.release.wasm not found on classpath");
        }
        final byte[] wasmBytes;
        try {
            wasmBytes = wasmInput.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var module = Parser.parse(wasmBytes);
        ImportValues imports = buildHostImports();
        this.instance =
                Instance.builder(module).withImportValues(imports).withStart(false).build();

        variableTypeMap.put(com.devcycle.sdk.server.common.model.Variable.TypeEnum.BOOLEAN, 0);
        variableTypeMap.put(com.devcycle.sdk.server.common.model.Variable.TypeEnum.NUMBER, 1);
        variableTypeMap.put(com.devcycle.sdk.server.common.model.Variable.TypeEnum.STRING, 2);
        variableTypeMap.put(com.devcycle.sdk.server.common.model.Variable.TypeEnum.JSON, 3);
    }

    private ImportValues buildHostImports() {
        HostFunction abortFn =
                new HostFunction(
                        "env",
                        "abort",
                        FunctionType.of(List.of(I32, I32, I32, I32), List.of()),
                        (inst, args) -> {
                            String message = readWasmString(inst.memory(), (int) args[0]);
                            String fileName = readWasmString(inst.memory(), (int) args[1]);
                            int linenum = (int) args[2];
                            int colnum = (int) args[3];
                            throw new RuntimeException(
                                    "Exception in " + fileName + ":" + linenum + " : " + colnum + " " + message);
                        });

        HostFunction dateNowFn =
                new HostFunction(
                        "env",
                        "Date.now",
                        FunctionType.of(List.of(), List.of(F64)),
                        (inst, args) ->
                                new long[] {
                                    Double.doubleToRawLongBits((double) System.currentTimeMillis())
                                });

        HostFunction consoleLogFn =
                new HostFunction(
                        "env",
                        "console.log",
                        FunctionType.of(List.of(I32), List.of()),
                        (inst, args) -> {
                            String message = readWasmString(inst.memory(), (int) args[0]);
                            DevCycleLogger.warning("WASM error: " + message);
                            return null;
                        });

        HostFunction seedFn =
                new HostFunction(
                        "env",
                        "seed",
                        FunctionType.of(List.of(), List.of(F64)),
                        (inst, args) ->
                                new long[] {
                                    Double.doubleToRawLongBits(
                                            System.currentTimeMillis() * Math.random())
                                });

        return ImportValues.builder()
                .addFunction(abortFn, dateNowFn, consoleLogFn, seedFn)
                .build();
    }

    private static String readWasmString(Memory mem, int startAddress) {
        byte[] headerBytes = {
            mem.read(startAddress - 1),
            mem.read(startAddress - 2),
            mem.read(startAddress - 3),
            mem.read(startAddress - 4)
        };
        long stringLength = ByteConversionUtils.getUnsignedInt(headerBytes);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < stringLength; i += 2) {
            result.append((char) mem.read(startAddress + i));
        }
        return result.toString();
    }

    private int newWasmString(String param) {
        int objectIdString = 1;

        ExportFunction __newExport = instance.export("__new");
        byte[] paramBytes = param.getBytes(StandardCharsets.UTF_8);
        int paramAddress =
                (int) __newExport.apply((long) (paramBytes.length * 2), (long) objectIdString)[0];

        Memory mem = instance.memory();
        for (int i = 0; i < paramBytes.length; i++) {
            mem.writeByte(paramAddress + (i * 2), paramBytes[i]);
        }

        return paramAddress;
    }

    private String readWasmString(int startAddress) {
        return readWasmString(instance.memory(), startAddress);
    }

    private int newUint8ArrayParameter(byte[] paramData) {
        int length = paramData.length;

        ExportFunction __newExport = instance.export("__new");
        int headerAddr = (int) __newExport.apply(12L, (long) WASM_OBJECT_ID_UINT8ARRAY)[0];
        try {
            pinParameter(headerAddr);
            int dataBufferAddr =
                    (int) __newExport.apply((long) length, (long) WASM_OBJECT_ID_STRING)[0];

            byte[] headerData = new byte[12];
            byte[] bufferAddrBytes = ByteConversionUtils.intToBytesLittleEndian(dataBufferAddr);
            byte[] lengthBytes = ByteConversionUtils.intToBytesLittleEndian(length << 0);
            for (int i = 0; i < 4; i++) {
                headerData[i] = bufferAddrBytes[i];
                headerData[i + 4] = bufferAddrBytes[i];
                headerData[i + 8] = lengthBytes[i];
            }

            Memory mem = instance.memory();
            for (int i = 0; i < headerData.length; i++) {
                mem.writeByte(headerAddr + i, headerData[i]);
            }
            for (int i = 0; i < length; i++) {
                mem.writeByte(dataBufferAddr + i, paramData[i]);
            }
        } finally {
            unpinParameter(headerAddr);
        }
        return headerAddr;
    }

    private byte[] readFromWasmMemory(int address, int length) {
        Memory mem = instance.memory();
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = mem.read(address + i);
        }
        return data;
    }

    private byte[] readAssemblyScriptUint8Array(int address) {
        byte[] bufferDataAddressBytes = readFromWasmMemory(address, 4);
        int bufferAddress = ByteConversionUtils.bytesToIntLittleEndian(bufferDataAddressBytes);

        byte[] lengthAddressBytes = readFromWasmMemory(address + 8, 4);
        int dataLength = ByteConversionUtils.bytesToIntLittleEndian(lengthAddressBytes);

        return readFromWasmMemory(bufferAddress, dataLength);
    }

    @Override
    public synchronized void storeConfig(String sdkKey, String config) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int configAddress = newUint8ArrayParameter(config.getBytes(StandardCharsets.UTF_8));

        ExportFunction setConfigData = instance.export("setConfigDataUTF8");
        setConfigData.apply((long) sdkKeyAddress, (long) configAddress);
    }

    @Override
    public synchronized void setPlatformData(String platformData) {
        unpinAll();
        int platformDataAddress = newUint8ArrayParameter(platformData.getBytes(StandardCharsets.UTF_8));
        ExportFunction fn = instance.export("setPlatformDataUTF8");
        fn.apply((long) platformDataAddress);
    }

    @Override
    public synchronized void setClientCustomData(String sdkKey, String customData) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int customDataAddress = newUint8ArrayParameter(customData.getBytes(StandardCharsets.UTF_8));
        ExportFunction fn = instance.export("setClientCustomDataUTF8");
        fn.apply((long) sdkKeyAddress, (long) customDataAddress);
    }

    @Override
    public synchronized BucketedUserConfig generateBucketedConfig(String sdkKey, DevCycleUser user)
            throws JsonProcessingException {
        unpinAll();
        String userString = OBJECT_MAPPER.writeValueAsString(user);

        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int userAddress = newUint8ArrayParameter(userString.getBytes(StandardCharsets.UTF_8));

        ExportFunction generateBucketedConfigForUser = instance.export("generateBucketedConfigForUserUTF8");
        int resultAddress =
                (int) generateBucketedConfigForUser.apply((long) sdkKeyAddress, (long) userAddress)[0];

        byte[] bucketConfigBytes = readAssemblyScriptUint8Array(resultAddress);
        String bucketedConfigString = new String(bucketConfigBytes, StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(bucketedConfigString, BucketedUserConfig.class);
    }

    @Override
    public synchronized byte[] getVariableForUserProtobuf(byte[] serializedParams) {
        int paramsAddr = newUint8ArrayParameter(serializedParams);

        ExportFunction variableForUserPB = instance.export("variableForUser_PB");
        int variableAddress = (int) variableForUserPB.apply((long) paramsAddr)[0];

        if (variableAddress > 0) {
            return readAssemblyScriptUint8Array(variableAddress);
        }
        return null;
    }

    @Override
    public synchronized void initEventQueue(String sdkKey, String clientUUID, String options) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int clientUUIDAddress = newWasmString(clientUUID);
        int optionsAddress = newWasmString(options);

        ExportFunction fn = instance.export("initEventQueue");
        fn.apply((long) sdkKeyAddress, (long) clientUUIDAddress, (long) optionsAddress);
    }

    @Override
    public synchronized void queueEvent(String sdkKey, String user, String event) {
        unpinAll();
        int sdkKeyAddress = newWasmString(sdkKey);
        int userAddress = getPinnedParameter(user);
        int eventAddress = newWasmString(event);

        ExportFunction fn = instance.export("queueEvent");
        fn.apply((long) sdkKeyAddress, (long) userAddress, (long) eventAddress);
    }

    @Override
    public synchronized void queueAggregateEvent(String sdkKey, String event, String variableVariationMap) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int eventAddress = getPinnedParameter(event);
        int variableVariationMapAddress = newWasmString(variableVariationMap);

        ExportFunction fn = instance.export("queueAggregateEvent");
        fn.apply((long) sdkKeyAddress, (long) eventAddress, (long) variableVariationMapAddress);
    }

    @Override
    public synchronized FlushPayload[] flushEventQueue(String sdkKey) throws JsonProcessingException {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);

        ExportFunction fn = instance.export("flushEventQueue");
        int resultAddress = (int) fn.apply((long) sdkKeyAddress)[0];
        String flushPayloadsStr = readWasmString(resultAddress);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.setDateFormat(df);

        return objectMapper.readValue(flushPayloadsStr, FlushPayload[].class);
    }

    @Override
    public synchronized void onPayloadFailure(String sdkKey, String payloadId, boolean retryable) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int payloadIdAddress = newWasmString(payloadId);

        ExportFunction fn = instance.export("onPayloadFailure");
        fn.apply((long) sdkKeyAddress, (long) payloadIdAddress, retryable ? 1L : 0L);
    }

    @Override
    public synchronized void onPayloadSuccess(String sdkKey, String payloadId) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        int payloadIdAddress = newWasmString(payloadId);

        ExportFunction fn = instance.export("onPayloadSuccess");
        fn.apply((long) sdkKeyAddress, (long) payloadIdAddress);
    }

    @Override
    public synchronized int getEventQueueSize(String sdkKey) {
        unpinAll();
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);

        ExportFunction fn = instance.export("eventQueueSize");
        return (int) fn.apply((long) sdkKeyAddress)[0];
    }

    private void pinParameter(int address) {
        instance.export("__pin").apply((long) address);
    }

    private void unpinParameter(int address) {
        instance.export("__unpin").apply((long) address);
    }

    private void unpinAll() {
        for (int address : pinnedAddresses) {
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
        if (!sdkKeyAddresses.containsKey(sdkKey)) {
            int sdkKeyAddress = newWasmString(sdkKey);
            pinParameter(sdkKeyAddress);
            sdkKeyAddresses.put(sdkKey, sdkKeyAddress);
        }

        return sdkKeyAddresses.get(sdkKey);
    }

    @Override
    public String getConfigMetadata(String sdkKey) {
        int sdkKeyAddress = getSDKKeyAddress(sdkKey);
        ExportFunction getConfigMetadata = instance.export("getConfigMetadata");
        int resultAddress = (int) getConfigMetadata.apply((long) sdkKeyAddress)[0];
        return readWasmString(resultAddress);
    }
}
