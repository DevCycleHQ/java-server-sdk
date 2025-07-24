package com.devcycle.sdk.server.cloud.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.devcycle.sdk.server.cloud.model.DevCycleCloudOptions;
import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.api.ObjectMapperUtils;
import com.devcycle.sdk.server.common.exception.AfterHookError;
import com.devcycle.sdk.server.common.exception.BeforeHookError;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.BaseVariable;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleResponse;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.DevCycleUserAndEvents;
import com.devcycle.sdk.server.common.model.ErrorResponse;
import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.EvalHooksRunner;
import com.devcycle.sdk.server.common.model.EvalReason;
import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.HttpResponseCode;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.common.model.Variable.TypeEnum;
import com.devcycle.sdk.server.openfeature.DevCycleProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import dev.openfeature.sdk.FeatureProvider;
import retrofit2.Call;
import retrofit2.Response;

public final class DevCycleCloudClient implements IDevCycleClient {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperUtils.createDefaultObjectMapper();
    private final IDevCycleApi api;
    private final DevCycleCloudOptions dvcOptions;
    private final DevCycleProvider openFeatureProvider;
    private final EvalHooksRunner evalHooksRunner;

    public DevCycleCloudClient(String sdkKey) {
        this(sdkKey, DevCycleCloudOptions.builder().build());
    }

    public DevCycleCloudClient(String sdkKey, DevCycleCloudOptions options) {
        if (sdkKey == null || sdkKey.equals("")) {
            throw new IllegalArgumentException("Missing environment key! Call initialize with a valid environment key");
        }

        if (!isValidServerKey(sdkKey)) {
            throw new IllegalArgumentException("Invalid environment key provided. Please call initialize with a valid server environment key");
        }

        if (options.getCustomLogger() != null) {
            DevCycleLogger.setCustomLogger(options.getCustomLogger());
        }

        this.dvcOptions = options;
        api = new DevCycleCloudApiClient(sdkKey, options).initialize();

        this.openFeatureProvider = new DevCycleProvider(this);
        this.evalHooksRunner = new EvalHooksRunner(dvcOptions.getHooks());
    }

    /**
     * Get all features for user data
     *
     * @param user (required)
     * @return Map&gt;String, Feature&lt;
     */
    public Map<String, Feature> allFeatures(DevCycleUser user) throws DevCycleException {
        validateUser(user);

        Call<Map<String, Feature>> response = api.getFeatures(user, dvcOptions.getEnableEdgeDB());
        return getResponse(response);
    }

    @Override
    public boolean isInitialized() {
        // Cloud Bucketing SDKs always count as initialized
        return true;
    }

    /**
     * Get variable value by key for user data
     *
     * @param user         (required)
     * @param key          Feature key (required)
     * @param defaultValue Default value to use if the variable could not be fetched
     *                     (required)
     * @return Variable value
     * @throws IllegalArgumentException If there are any issues with the key or default value provided
     */
    public <T> T variableValue(DevCycleUser user, String key, T defaultValue) {
        return variable(user, key, defaultValue).getValue();
    }

    /**
     * Get variable by key for user data
     *
     * @param user         (required)
     * @param key          Variable key (required)
     * @param defaultValue Default value to use if the variable could not be fetched (required)
     * @return Variable
     * @throws IllegalArgumentException If there are any issues with the key or default value provided
     */
    @SuppressWarnings("unchecked")
    public <T> Variable<T> variable(DevCycleUser user, String key, T defaultValue) {
        validateUser(user);

        if (key == null || key.equals("")) {
            ErrorResponse errorResponse = new ErrorResponse(500, "Missing parameter: key", null);
            throw new IllegalArgumentException("Missing parameter: key");
        }

        if (defaultValue == null) {
            ErrorResponse errorResponse = new ErrorResponse(500, "Missing parameter: defaultValue", null);
            throw new IllegalArgumentException("Missing parameter: defaultValue");
        }

        TypeEnum variableType = TypeEnum.fromClass(defaultValue.getClass());
        Variable<T> variable = null;
        HookContext<T> context = new HookContext<T>(user, key, defaultValue, null);
        ArrayList<EvalHook<T>> hooks = new ArrayList<EvalHook<T>>(evalHooksRunner.getHooks());
        ArrayList<EvalHook<T>> reversedHooks = new ArrayList<>(hooks);
        Collections.reverse(reversedHooks);

        try {
            Throwable beforeError = null;

            try {
                context = context.merge(evalHooksRunner.executeBefore(hooks, context));
            } catch (Throwable e) {
                beforeError = e;
            }

            Call<Variable> response = api.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB());
            variable = getResponseWithRetries(response, 5);
            if (variable.getType() != variableType) {
                throw new IllegalArgumentException("Variable type mismatch, returning default value");
            }
            if (beforeError != null) {
                throw beforeError;
            }

            variable.setIsDefaulted(false);
            evalHooksRunner.executeAfter(reversedHooks, context, variable);
        } catch (Throwable exception) {
            if (!(exception instanceof BeforeHookError || exception instanceof AfterHookError)) {
                variable = (Variable<T>) Variable.builder()
                        .key(key)
                        .type(variableType)
                        .value(defaultValue)
                        .defaultValue(defaultValue)
                        .isDefaulted(true)
                        .build();

                if (exception.getMessage().equals("Variable type mismatch, returning default value")) {
                    variable.setEval(EvalReason.defaultReason(EvalReason.DefaultReasonDetailsEnum.VARIABLE_TYPE_MISMATCH));
                } else {
                    variable.setEval(EvalReason.defaultReason(EvalReason.DefaultReasonDetailsEnum.ERROR));
                }
            }

            evalHooksRunner.executeError(reversedHooks, context, exception);
        } finally {
            evalHooksRunner.executeFinally(reversedHooks, context, Optional.ofNullable(variable));
        }
        return variable;
    }

    @Override
    public void close() {
        // no-op - Cloud SDKs don't hold onto any resources
    }


    /**
     * @return the OpenFeature provider for this client.
     */
    @Override
    public FeatureProvider getOpenFeatureProvider() {
        return this.openFeatureProvider;
    }

    @Override
    public String getSDKPlatform() {
        return "Cloud";
    }

    /**
     * Get all variables by key for user data
     *
     * @param user (required)
     * @return Map&gt;String, BaseVariable&lt;
     */
    public Map<String, BaseVariable> allVariables(DevCycleUser user) throws DevCycleException {
        validateUser(user);

        Call<Map<String, BaseVariable>> response = api.getVariables(user, dvcOptions.getEnableEdgeDB());
        try {
            Map<String, BaseVariable> variablesResponse = getResponse(response);
            return variablesResponse;
        } catch (DevCycleException exception) {
            if (exception.getHttpResponseCode() == HttpResponseCode.SERVER_ERROR) {
                return new HashMap<>();
            }
            throw exception;
        }
    }

    /**
     * Post events to DevCycle for user
     *
     * @param user  (required)
     * @param event (required)
     */
    public void track(DevCycleUser user, DevCycleEvent event) throws DevCycleException {
        validateUser(user);

        if (event == null || event.getType() == null || event.getType().equals("")) {
            throw new IllegalArgumentException("Invalid DevCycleEvent");
        }

        DevCycleUserAndEvents userAndEvents = DevCycleUserAndEvents.builder()
                .user(user)
                .events(Collections.singletonList(event))
                .build();

        Call<DevCycleResponse> response = api.track(userAndEvents, dvcOptions.getEnableEdgeDB());
        getResponseWithRetries(response, 5);
    }


    private <T> T getResponseWithRetries(Call<T> call, int maxRetries) throws DevCycleException {
        // attempt 0 is the initial request, attempt > 0 are all retries
        int attempt = 0;
        do {
            try {
                return getResponse(call);
            } catch (DevCycleException e) {
                attempt++;

                // if out of retries or this is an unauthorized error, throw up exception
                if (!e.isRetryable() || attempt > maxRetries) {
                    throw e;
                }

                try {
                    // exponential backoff
                    long waitIntervalMS = (long) (10 * Math.pow(2, attempt));
                    Thread.sleep(waitIntervalMS);
                } catch (InterruptedException ex) {
                    // no-op
                }

                // prep the call for a retry
                call = call.clone();
            }
        } while (attempt <= maxRetries);

        // getting here should not happen, but is technically possible
        ErrorResponse errorResponse = ErrorResponse.builder().build();
        errorResponse.setMessage("Out of retry attempts");
        throw new DevCycleException(HttpResponseCode.SERVER_ERROR, errorResponse);
    }

    public void addHook(EvalHook hook) {
        this.evalHooksRunner.addHook(hook);
    }

    public void clearHooks() {
        this.evalHooksRunner.clearHooks();
    }

    private <T> T getResponse(Call<T> call) throws DevCycleException {
        ErrorResponse errorResponse = ErrorResponse.builder().build();
        Response<T> response;

        try {
            response = call.execute();
        } catch (MismatchedInputException mie) {
            // got a badly formatted JSON response from the server
            errorResponse.setMessage(mie.getMessage());
            throw new DevCycleException(HttpResponseCode.NO_CONTENT, errorResponse);
        } catch (IOException e) {
            // issues reaching the server or reading the response
            errorResponse.setMessage(e.getMessage());
            throw new DevCycleException(HttpResponseCode.byCode(500), errorResponse);
        }

        HttpResponseCode httpResponseCode = HttpResponseCode.byCode(response.code());
        errorResponse.setMessage("Unknown error");

        if (response.errorBody() != null) {
            try {
                errorResponse = OBJECT_MAPPER.readValue(response.errorBody().string(), ErrorResponse.class);
            } catch (IOException e) {
                errorResponse.setMessage(e.getMessage());
                throw new DevCycleException(httpResponseCode, errorResponse);
            }
            throw new DevCycleException(httpResponseCode, errorResponse);
        }

        if (response.body() == null) {
            throw new DevCycleException(httpResponseCode, errorResponse);
        }

        if (response.isSuccessful()) {
            return response.body();
        } else {
            if (httpResponseCode == HttpResponseCode.UNAUTHORIZED) {
                errorResponse.setMessage("Invalid SDK Key");
            } else if (!response.message().equals("")) {
                try {
                    errorResponse = OBJECT_MAPPER.readValue(response.message(), ErrorResponse.class);
                } catch (JsonProcessingException e) {
                    errorResponse.setMessage(e.getMessage());
                    throw new DevCycleException(httpResponseCode, errorResponse);
                }
            }

            throw new DevCycleException(httpResponseCode, errorResponse);
        }
    }

    private boolean isValidServerKey(String serverKey) {
        return serverKey.startsWith("server") || serverKey.startsWith("dvc_server");
    }

    private void validateUser(DevCycleUser user) {
        if (user == null) {
            throw new IllegalArgumentException("DevCycleUser cannot be null");
        }
        if (user.getUserId().equals("")) {
            throw new IllegalArgumentException("userId cannot be empty");
        }
    }
}
