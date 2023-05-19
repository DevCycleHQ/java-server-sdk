package com.devcycle;

import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.api.DVCLocalClient;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SDKBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        public MockServer mockServer;

        public DVCLocalClient client;

        @Setup(Level.Iteration)
        public void setup() {
            try {
                mockServer = new MockServer();
            } catch (IOException e) {
                System.out.print("Unable to setup mock HTTP server");
                e.printStackTrace();
            }

            DVCLocalOptions dvcLocalOptions = DVCLocalOptions.builder()
                    .configPollingIntervalMS(10000)
                    .configRequestTimeoutMs(5000)
                    .eventFlushIntervalMS(5000)
                    .disableAutomaticEventLogging(true)
                    .disableCustomEventLogging(true)
                    .configCdnBaseUrl("http://localhost:8000/config/")
                    .eventsApiBaseUrl("http://localhost:8000/event/")
                    .build();

            client = new DVCLocalClient("dvc_server_some_real_sdk_key", dvcLocalOptions);

            try {
                System.out.println("Waiting for DVC client to load");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        @TearDown(Level.Iteration)
        public void tearDown() {
            System.out.println("Cleaning up DVC client");
            client.close();
            System.out.println("Stop mock HTTP server");
            mockServer.close();
        }
    }
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 5)
    @Fork(1)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void variableBenchmark(BenchmarkState state) {
        User user = User.builder().userId("12345").email("some.user@gmail.com").build();
        Variable<Boolean> var = state.client.variable(user, "v-key-25", false);

        if( var == null) {
            System.err.println("Unexpected null variable");
        }
        else if (var.getIsDefaulted()) {
            System.err.println("Unexpected default variable");
        }
    }
    /**
     * Main method is used when running the benchmark manually from the IDE
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SDKBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(5)
                .build();
        new Runner(opt).run();
    }
}