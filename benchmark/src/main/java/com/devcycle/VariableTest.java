package com.devcycle;

import com.devcycle.sdk.server.common.model.PlatformData;
import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.api.DVCLocalClient;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;

import java.io.IOException;

/**
 * A simple loop to test and profile vriable evaulation of the DVC CLient
 */
public class VariableTest {
    public static void main(String[] args) throws IOException {
        MockServer server = new MockServer();
        DVCLocalOptions dvcLocalOptions = DVCLocalOptions.builder()
                .configPollingIntervalMS(10000)
                .configRequestTimeoutMs(5000)
                .eventFlushIntervalMS(5000)
                .configCdnBaseUrl("http://localhost:8000/config/")
                .eventsApiBaseUrl("http://localhost:8000/event/")
                .build();

        System.out.println("Initializing DVC Client version: " + PlatformData.builder().build().getSdkVersion());
        System.out.print("Setup Client");
        DVCLocalClient dvcClient = new DVCLocalClient("dvc_server_some_sdk_key_here", dvcLocalOptions);

        System.out.print("Wait config to load");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // no-op
        }

        long startTimeNS = System.nanoTime();
        int iterations = 10000;
        int defaultCount = 0;
        int nullVarCount = 0;
        for(int i = 0; i < iterations; i++) {
            User user = User.builder().userId("12345").email("chris.hoefgen@taplytics.com").build();
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
