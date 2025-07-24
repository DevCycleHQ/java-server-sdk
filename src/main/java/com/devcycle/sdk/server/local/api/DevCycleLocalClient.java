package com.devcycle.sdk.server.local.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.exception.BeforeHookError;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.BaseVariable;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.EvalHooksRunner;
import com.devcycle.sdk.server.common.model.EvalReason;
import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.PlatformData;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.common.model.Variable.TypeEnum;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EnvironmentConfigManager;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.BucketedUserConfig;
import com.devcycle.sdk.server.local.model.ConfigMetadata;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.devcycle.sdk.server.local.protobuf.SDKVariable_PB;
import com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB;
import com.devcycle.sdk.server.local.protobuf.VariableType_PB;
import com.devcycle.sdk.server.local.utils.ProtobufUtils;
import com.devcycle.sdk.server.openfeature.DevCycleProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.openfeature.sdk.FeatureProvider;

public final class DevCycleLocalClient implements IDevCycleClient {

    private final String sdkKey;
    private final LocalBucketing localBucketing = new LocalBucketing();
    private final EnvironmentConfigManager configManager;
    private EventQueueManager eventQueueManager;
    private final String clientUUID;
    // raw type here is okay because we're using a generic type for the variable
    private EvalHooksRunner evalHooksRunner;

    public DevCycleLocalClient(String sdkKey) {
        this(sdkKey, DevCycleLocalOptions.builder().build());
    }

    public DevCycleLocalClient(String sdkKey, DevCycleLocalOptions dvcOptions) {
        clientUUID = UUID.randomUUID().toString();
        if (sdkKey == null || sdkKey.equals("")) {
            throw new IllegalArgumentException("Missing SDK key! Call initialize with a valid SDK key");
        }
        if (!isValidServerKey(sdkKey)) {
            throw new IllegalArgumentException("Invalid SDK key provided. Please call initialize with a valid server SDK key");
        }

        if (dvcOptions.getCustomLogger() != null) {
            DevCycleLogger.setCustomLogger(dvcOptions.getCustomLogger());
        }

        if (!isValidRuntime()) {
            DevCycleLogger.warning("The DevCycleLocalClient requires a 64-bit, x86 or aarch64 runtime environment. This architecture may not be supported: " + System.getProperty("os.arch") + " | " + System.getProperty("sun.arch.data.model"));
        }

        localBucketing.setPlatformData(PlatformData.builder().build().toString());

        configManager = new EnvironmentConfigManager(sdkKey, localBucketing, dvcOptions);
        this.sdkKey = sdkKey;
        try {
            eventQueueManager = new EventQueueManager(sdkKey, localBucketing, clientUUID, dvcOptions);
        } catch (Exception e) {
            DevCycleLogger.error("Error creating event queue due to error: " + e.getMessage());
        }
        this.evalHooksRunner = new EvalHooksRunner(dvcOptions.getHooks());
    }

    /**
     * @return true if the DevCycleLocalClient is fully initialized and has successfully loaded a configuration
     */
    public boolean isInitialized() {
        if (configManager != null) {
            return configManager.isConfigInitialized();
        }
        return false;
    }

    /**
     * Get all features for user data
     *
     * @param user (required)
     */
    public Map<String, Feature> allFeatures(DevCycleUser user) {
        if (!isInitialized()) {
            return Collections.emptyMap();
        }

        validateUser(user);
        BucketedUserConfig bucketedUserConfig = null;

        try {
            bucketedUserConfig = localBucketing.generateBucketedConfig(sdkKey, user);
        } catch (JsonProcessingException e) {
            DevCycleLogger.error("Unable to parse JSON for allFeatures due to error: " + e.getMessage());
            return Collections.emptyMap();
        }
        return bucketedUserConfig.features;
    }

    /**
     * Get variable value by key for user data
     *
     * @param user         (required)
     * @param key          Feature key (required)
     * @param defaultValue Default value to use if the variable could not be fetched
     *                     (required)
     * @return Variable value
     */
    public <T> T variableValue(DevCycleUser user, String key, T defaultValue) {
        return variable(user, key, defaultValue).getValue();
    }

    /**
     * Get variable by key for user data
     *
     * @param user         (required)
     * @param key          Variable key (required)
     * @param defaultValue Default value to use if the variable could not be fetched
     *                     (required)
     * @return Variable
     */
    public <T> Variable<T> variable(DevCycleUser user, String key, T defaultValue) {
        validateUser(user);

        if (key == null || key.equals("")) {
            throw new IllegalArgumentException("Missing parameter: key");
        }

        if (defaultValue == null) {
            throw new IllegalArgumentException("Missing parameter: defaultValue");
        }

        TypeEnum variableType = TypeEnum.fromClass(defaultValue.getClass());
        Variable<T> defaultVariable = (Variable<T>) Variable.builder()
                .key(key)
                .type(variableType)
                .value(defaultValue)
                .defaultValue(defaultValue)
                .isDefaulted(true)
                .build();

        if (!isInitialized()) {
            DevCycleLogger.info("Variable called before DevCycleLocalClient has initialized, returning default value");
            try {
                eventQueueManager.queueAggregateEvent(DevCycleEvent.builder().type("aggVariableDefaulted").target(key).build(), null);
            } catch (Exception e) {
                DevCycleLogger.error("Unable to parse aggVariableDefaulted event for Variable " + key + " due to error: " + e, e);
            }
            defaultVariable.setEval(EvalReason.defaultReason(EvalReason.DefaultReasonDetailsEnum.MISSING_CONFIG));
            return defaultVariable;
        }

        VariableType_PB pbVariableType = ProtobufUtils.convertTypeEnumToVariableType(variableType);
        VariableForUserParams_PB params = VariableForUserParams_PB.newBuilder()
                .setSdkKey(sdkKey)
                .setUser(ProtobufUtils.createDVCUserPB(user))
                .setVariableKey(key)
                .setVariableType(pbVariableType)
                .setShouldTrackEvent(true)
                .build();

        HookContext<T> hookContext = new HookContext<T>(user, key, defaultValue, getMetadata());
        Variable<T> variable = null;
        ArrayList<EvalHook<T>> hooks = new ArrayList<EvalHook<T>>(evalHooksRunner.getHooks());
        ArrayList<EvalHook<T>> reversedHooks = new ArrayList<EvalHook<T>>(evalHooksRunner.getHooks());
        Collections.reverse(reversedHooks);

        try {
            byte[] paramsBuffer = params.toByteArray();
            byte[] variableData = localBucketing.getVariableForUserProtobuf(paramsBuffer);
            Throwable beforeError = null;
            try {
                evalHooksRunner.executeBefore(hooks, hookContext);
            } catch (Throwable e) {
                beforeError = e;
            }


            if (variableData == null || variableData.length == 0) {
                variable = defaultVariable;
                variable.setEval(EvalReason.defaultReason(EvalReason.DefaultReasonDetailsEnum.USER_NOT_TARGETED));
            } else {
                SDKVariable_PB sdkVariable = SDKVariable_PB.parseFrom(variableData);
                if (sdkVariable.getType() != pbVariableType) {
                    DevCycleLogger.warning("Variable type mismatch, returning default value");
                    variable = defaultVariable;
                    variable.setEval(EvalReason.defaultReason(EvalReason.DefaultReasonDetailsEnum.VARIABLE_TYPE_MISMATCH));
                } else {
                    variable = ProtobufUtils.createVariable(sdkVariable, defaultValue);
                }
            }
            if (beforeError != null) {
                throw beforeError;
            }
            evalHooksRunner.executeAfter(reversedHooks, hookContext, variable);
        } catch (Throwable e) {
            if (!(e instanceof BeforeHookError)) {
                DevCycleLogger.error("Unable to evaluate Variable " + key + " due to error: " + e, e);
            }
            // For BeforeHookError, pass the original cause to error hooks, not the wrapper
            Throwable errorToPass = (e instanceof BeforeHookError && e.getCause() != null) ? e.getCause() : e;
            evalHooksRunner.executeError(reversedHooks, hookContext, errorToPass);
        } finally {
            if (variable == null) {
                variable = defaultVariable;
                variable.setEval(EvalReason.defaultReason(EvalReason.DefaultReasonDetailsEnum.USER_NOT_TARGETED));
            }
            evalHooksRunner.executeFinally(reversedHooks, hookContext, Optional.of(variable));
        }
        return variable;
    }

    public ConfigMetadata getMetadata() {
        return configManager.getConfigMetadata();
    }


    /**
     * Get all variables by key for user data
     *
     * @param user (required)
     */
    public Map<String, BaseVariable> allVariables(DevCycleUser user) {
        validateUser(user);

        if (!isInitialized()) {
            return Collections.emptyMap();
        }

        BucketedUserConfig bucketedUserConfig = null;
        try {
            bucketedUserConfig = localBucketing.generateBucketedConfig(sdkKey, user);
        } catch (JsonProcessingException e) {
            DevCycleLogger.error("Unable to parse JSON for allVariables due to error: " + e.getMessage());
            return Collections.emptyMap();
        }
        return bucketedUserConfig.variables;
    }

    /**
     * Post events to DevCycle for user
     *
     * @param user  (required)
     * @param event (required)
     */
    public void track(DevCycleUser user, DevCycleEvent event) {
        validateUser(user);

        if (event == null || event.getType().equals("")) {
            throw new IllegalArgumentException("Invalid DevCycleEvent");
        }

        try {
            eventQueueManager.queueEvent(user, event);
        } catch (Exception e) {
            DevCycleLogger.error("Failed to queue event due to error: " + e.getMessage());
        }
    }

    public void setClientCustomData(Map<String, Object> customData) {
        if (!isInitialized()) {
            DevCycleLogger.error("SetClientCustomData called before DevCycleClient has initialized");
            return;
        }

        if (customData != null && !customData.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String customDataJSON = mapper.writeValueAsString(customData);
                localBucketing.setClientCustomData(this.sdkKey, customDataJSON);
            } catch (Exception e) {
                DevCycleLogger.error("Failed to set custom data due to error: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Gracefully close the client
     */
    public void close() {
        if (configManager != null) {
            configManager.cleanup();
        }
        if (eventQueueManager != null) {
            eventQueueManager.cleanup();
        }
    }

    /**
     * Add an evaluation hook to the client
     *
     * @param hook The hook to add
     */
    public void addHook(EvalHook hook) {
        this.evalHooksRunner.addHook(hook);
    }

    /**
     * Remove all evaluation hooks from the client
     */
    public void clearHooks() {
        this.evalHooksRunner.clearHooks();
    }

    private static DevCycleProvider openFeatureProvider = null;

    /**
     * @return the OpenFeature provider for this client.
     */
    @Override
    public FeatureProvider getOpenFeatureProvider() {
        if (openFeatureProvider == null) {
            synchronized (DevCycleLocalClient.class) {
                if (openFeatureProvider == null) {
                    openFeatureProvider = new DevCycleProvider(this);
                }
                PlatformData platformData = PlatformData.builder().sdkPlatform("java-of").build();
                localBucketing.setPlatformData(platformData.toString());
            }
        }
        return openFeatureProvider;
    }

    @Override
    public String getSDKPlatform() {
        return "Local";
    }

    private void validateUser(DevCycleUser user) {
        if (user == null) {
            throw new IllegalArgumentException("DevCycleUser cannot be null");
        }
        if (user.getUserId().equals("")) {
            throw new IllegalArgumentException("userId cannot be empty");
        }
    }

    private boolean isValidServerKey(String serverKey) {
        return serverKey.startsWith("server") || serverKey.startsWith("dvc_server");
    }

    private boolean isValidRuntime() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String model = System.getProperty("sun.arch.data.model").toLowerCase();
        if (arch.contains("x86") && model.contains("64")) {
            // Assume support for all x86_64 platforms
            return true;
        }
        if (os.contains("mac os") || os.contains("darwin")) {
            // Specific case of Apple Silicon
            return arch.equals("aarch64");
        }
        return false;
    }
}
