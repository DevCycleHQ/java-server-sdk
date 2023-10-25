package com.devcycle.sdk.server.openfeature;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;

public class DevCycleProvider implements FeatureProvider {
    private static final String PROVIDER_NAME = "DevCycleProvider";

    private final IDevCycleClient devcycleClient;

    public DevCycleProvider(IDevCycleClient devcycleClient) {
        this.devcycleClient = devcycleClient;
    }

    @Override
    public Metadata getMetadata() {
        return () -> PROVIDER_NAME;
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultValue, EvaluationContext ctx) {
        return resolve(key, defaultValue, ctx);
    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String key, String defaultValue, EvaluationContext ctx) {
        return resolve(key, defaultValue, ctx);
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultValue, EvaluationContext ctx) {
        return resolve(key, defaultValue, ctx);
    }

    @Override
    public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultValue, EvaluationContext ctx) {
        return resolve(key, defaultValue, ctx);
    }

    @Override
    public ProviderEvaluation<Value> getObjectEvaluation(String key, Value defaultValue, EvaluationContext ctx) {
        return resolve(key, defaultValue, ctx);
    }

    @Override
    public void shutdown() {
        devcycleClient.close();
    }

    <T> ProviderEvaluation<T> resolve(String key, T defaultValue, EvaluationContext ctx) {
        if (devcycleClient.isInitialized()) {
            try {
                DevCycleUser user = DevCycleUser.createUserFromContext(ctx);

                Variable<T> variable = devcycleClient.variable(user, key, defaultValue);

                if (variable == null || variable.getIsDefaulted()) {
                    return ProviderEvaluation.<T>builder()
                            .value(defaultValue)
                            .reason(Reason.DEFAULT.toString())
                            .build();
                } else {
                    return ProviderEvaluation.<T>builder()
                            .value(variable.getValue())
                            .reason(Reason.TARGETING_MATCH.toString())
                            .build();
                }
            } catch (IllegalArgumentException e) {
                return ProviderEvaluation.<T>builder()
                        .value(defaultValue)
                        .reason(Reason.ERROR.toString())
                        .errorCode(ErrorCode.GENERAL)
                        .errorMessage(e.getMessage())
                        .build();
            }
        } else {
            throw new ProviderNotReadyError("DevCycle client not initialized");
        }
    }
}
