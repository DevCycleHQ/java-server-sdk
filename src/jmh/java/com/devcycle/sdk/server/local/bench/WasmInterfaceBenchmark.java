package com.devcycle.sdk.server.local.bench;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.PlatformData;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB;
import com.devcycle.sdk.server.local.protobuf.VariableType_PB;
import com.devcycle.sdk.server.local.utils.ProtobufUtils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * JMH microbenchmarks comparing {@code wasmtime-java} vs {@code Chicory} for the same {@link
 * LocalBucketing} WASM calls. Run: {@code ./gradlew jmh}
 */
public class WasmInterfaceBenchmark {

    private static String readClasspathResource(String name) throws IOException {
        try (InputStream in =
                WasmInterfaceBenchmark.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("Missing classpath resource: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static LocalBucketing newBucketingForParam(String wasmRuntime) {
        if ("chicory".equals(wasmRuntime)) {
            return LocalBucketing.forChicory();
        }
        if ("wasmtime".equals(wasmRuntime)) {
            return LocalBucketing.forWasmtime();
        }
        throw new IllegalArgumentException("wasmRuntime must be wasmtime or chicory: " + wasmRuntime);
    }

    @State(Scope.Benchmark)
    public static class SmallFixtureState {
        @Param({"wasmtime", "chicory"})
        public String wasmRuntime;

        LocalBucketing bucketing;
        String sdkKey;
        DevCycleUser user;
        byte[] variableParamsProtobuf;

        @Setup
        public void setup() throws IOException, JsonProcessingException {
            String config = readClasspathResource("fixture_small_config.json");
            bucketing = newBucketingForParam(wasmRuntime);
            bucketing.setPlatformData(PlatformData.builder().build().toString());
            sdkKey = "server-jmh-small-" + wasmRuntime;
            bucketing.storeConfig(sdkKey, config);
            user =
                    DevCycleUser.builder()
                            .userId("jmh-user")
                            .email("bench@example.com")
                            .build();
            VariableForUserParams_PB params =
                    VariableForUserParams_PB.newBuilder()
                            .setSdkKey(sdkKey)
                            .setUser(ProtobufUtils.createDVCUserPB(user))
                            .setVariableKey("a-cool-new-feature")
                            .setVariableType(VariableType_PB.Boolean)
                            .setShouldTrackEvent(false)
                            .build();
            variableParamsProtobuf = params.toByteArray();
        }
    }

    @State(Scope.Benchmark)
    public static class LargeFixtureState {
        @Param({"wasmtime", "chicory"})
        public String wasmRuntime;

        LocalBucketing bucketing;
        String sdkKey;
        DevCycleUser user;

        @Setup
        public void setup() throws IOException {
            String config = readClasspathResource("fixture_large_config.json");
            bucketing = newBucketingForParam(wasmRuntime);
            bucketing.setPlatformData(PlatformData.builder().build().toString());
            sdkKey = "server-jmh-large-" + wasmRuntime;
            bucketing.storeConfig(sdkKey, config);
            user =
                    DevCycleUser.builder()
                            .userId("jmh-user")
                            .email("some.user@gmail.com")
                            .build();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 2, time = 1)
    @Measurement(iterations = 5, time = 1)
    @Fork(1)
    public void generateBucketedConfig_smallFixture(SmallFixtureState state)
            throws JsonProcessingException {
        state.bucketing.generateBucketedConfig(state.sdkKey, state.user);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 2, time = 1)
    @Measurement(iterations = 5, time = 1)
    @Fork(1)
    public void generateBucketedConfig_largeFixture(LargeFixtureState state)
            throws JsonProcessingException {
        state.bucketing.generateBucketedConfig(state.sdkKey, state.user);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 2, time = 1)
    @Measurement(iterations = 5, time = 1)
    @Fork(1)
    public void variableForUserProtobuf_smallFixture(SmallFixtureState state) {
        state.bucketing.getVariableForUserProtobuf(state.variableParamsProtobuf);
    }
}
