package com.devcycle.examples;

import java.util.Optional;

import com.devcycle.sdk.server.common.logging.SimpleDevCycleLogger;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;

public class LocalExample {
    public static String VARIABLE_KEY = "example-text";

    public static void main(String[] args) throws InterruptedException {
        String server_sdk_key = System.getenv("DEVCYCLE_SERVER_SDK_KEY");
        if (server_sdk_key == null) {
            System.err.println("Please set the DEVCYCLE_SERVER_SDK_KEY environment variable");
            System.exit(1);
        }

        // Create user object
        DevCycleUser user = DevCycleUser.builder()
                .userId("j_test")
                .build();

        // The default value can be of type string, boolean, number, or JSON
        String defaultValue = "false";

        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .customLogger(new SimpleDevCycleLogger(SimpleDevCycleLogger.Level.DEBUG))
                .build();

        // Initialize DevCycle Client
        DevCycleLocalClient client = new DevCycleLocalClient(server_sdk_key, options);

        client.addHook(new EvalHook<String>() {
            @Override
            public Optional<HookContext<String>> before(HookContext<String> ctx) {
                System.out.println("before");
                System.out.println(ctx.getMetadata().getProject().getKey());
                System.out.println(ctx.getMetadata().getEnvironment().getKey());
                return Optional.of(ctx);
            }

            @Override
            public void after(HookContext<String> ctx, Variable<String> variable) {
                System.out.println("after");
                System.out.println(variable.getValue());
                System.out.println(ctx.getMetadata().getProject().getKey());
                System.out.println(ctx.getMetadata().getEnvironment().getKey());
            }

            @Override
            public void onFinally(HookContext<String> ctx, Optional<Variable<String>> variable) {
                System.out.println("finally");
                System.out.println(ctx.getMetadata().getProject().getKey());
                System.out.println(ctx.getMetadata().getEnvironment().getKey());
            }
        });

        for (int i = 0; i < 10; i++) {
            if (client.isInitialized()) {
                break;
            }
            Thread.sleep(500);
        }

        // Fetch variable values using the identifier key, with a default value and user
        // object
        // The default value can be of type string, boolean, number, or JSON
        String variableValue = client.variableValue(user, VARIABLE_KEY, defaultValue);

        // Use variable value
        if (variableValue.equals("true")) {
            System.out.println("feature is enabled");
        } else {
            System.out.println("feature is NOT enabled: " + variableValue);
        }

        DevCycleEvent event = DevCycleEvent.builder().type("local-test").build();
        client.track(user, event);

        Thread.sleep(20000);
    }
}
