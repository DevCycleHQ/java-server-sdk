package com.devcycle;

import com.devcycle.sdk.server.common.model.PlatformData;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;

import java.io.IOException;

/**
 * A simple loop to test and profile variable evaluation of the DevCycleLocalClient
 */
public class VariableTest {
    public static void main(String[] args) throws IOException {
        MockServer server = new MockServer();
        DevCycleLocalOptions dvcLocalOptions = DevCycleLocalOptions.builder()
                .configPollingIntervalMS(10000)
                .configRequestTimeoutMs(5000)
                .eventFlushIntervalMS(5000)
                .configCdnBaseUrl("http://localhost:8000/config/")
                .eventsApiBaseUrl("http://localhost:8000/event/")
                .disableCustomEventLogging(false)
                .disableAutomaticEventLogging(false)
                .build();

        System.out.println("Initializing DVC Client version: " + PlatformData.builder().build().getSdkVersion());
        System.out.print("Setup Client");
        DevCycleLocalClient dvcClient = new DevCycleLocalClient("dvc_server_some_sdk_key", dvcLocalOptions);

        System.out.print("Wait for config to load");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // no-op
        }

        long startTimeNS = System.nanoTime();
        int iterations = 100000;
        int defaultCount = 0;
        int nullVarCount = 0;
        DevCycleUser user = DevCycleUser.builder().userId("1234567890").email("some.user@gmail.com").build();
        for(int i = 0; i < iterations; i++) {
            Variable<Boolean> var = dvcClient.variable(user, "v-key-25", false);
            if( var == null) {
                nullVarCount++;
            }
            else if (var.getIsDefaulted()) {
                defaultCount++;
            }
        }
        long endTimeNS = System.nanoTime();

        System.out.println("Iterations: " + iterations);
        System.out.println("Null Vars: " + nullVarCount);
        System.out.println("Variables Defaulted: " + defaultCount);
        System.out.println("Average time: " + ((endTimeNS - startTimeNS) / (double)iterations) + " ns/op");

        dvcClient.close();
        server.close();
    }
}
