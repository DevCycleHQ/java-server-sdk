package com.devcycle.sdk.server.openfeature;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import dev.openfeature.sdk.exceptions.TypeMismatchError;

import java.util.Map;

public class DevCycleProvider implements FeatureProvider {
    private static final String PROVIDER_NAME = "DevCycle";

    private final IDevCycleClient devcycleClient;

    public DevCycleProvider(IDevCycleClient devcycleClient) {
        this.devcycleClient = devcycleClient;
    }

    @Override
    public Metadata getMetadata() {
        return () -> PROVIDER_NAME + " " + devcycleClient.getSDKPlatform();
    }

    @Override
    public ProviderEvaluation<Boolean> getBooleanEvaluation(String key, Boolean defaultValue, EvaluationContext ctx) {
        return resolvePrimitiveVariable(key, defaultValue, ctx);
    }

    @Override
    public ProviderEvaluation<String> getStringEvaluation(String key, String defaultValue, EvaluationContext ctx) {
        return resolvePrimitiveVariable(key, defaultValue, ctx);
    }

    @Override
    public ProviderEvaluation<Integer> getIntegerEvaluation(String key, Integer defaultValue, EvaluationContext ctx) {
        ProviderEvaluation<Integer> eval = resolvePrimitiveVariable(key, defaultValue, ctx);
        return eval;
    }

    @Override
    public ProviderEvaluation<Double> getDoubleEvaluation(String key, Double defaultValue, EvaluationContext ctx) {
        return resolvePrimitiveVariable(key, defaultValue, ctx);
    }

    @Override
    public ProviderEvaluation<Value> getObjectEvaluation(String key, Value defaultValue, EvaluationContext ctx) {
        /*
         * JSON objects have special rules in the DevCycle SDK and must be handled differently
         * - must always be an object, no lists or literal values
         * - must only contain strings, numbers, and booleans
         */
        if (!defaultValue.isStructure()) {
            throw new TypeMismatchError("Default value must be a OpenFeature structure");
        }

        for (String k : defaultValue.asStructure().keySet()) {
            Value v = defaultValue.asStructure().getValue(k);
            if (!(v.isString() || v.isNumber() || v.isBoolean() || v.isNull())) {
                throw new TypeMismatchError("DevCycle JSON objects may only contain strings, numbers, booleans and nulls");
            }
        }

        if (devcycleClient.isInitialized()) {
            try {
                DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);

                Variable<Object> variable = devcycleClient.variable(user, key, defaultValue.asStructure().asObjectMap());

                if (variable == null || variable.getIsDefaulted()) {
                    return ProviderEvaluation.<Value>builder()
                            .value(defaultValue)
                            .reason(Reason.DEFAULT.toString())
                            .build();
                } else {
                    if (variable.getValue() instanceof Map) {
                        // JSON objects are managed as Map implementations and must be converted to an OpenFeature structure
                        Value objectValue = new Value(Structure.mapToStructure((Map) variable.getValue()));
                        return ProviderEvaluation.<Value>builder()
                                .value(objectValue)
                                .reason(Reason.TARGETING_MATCH.toString())
                                .build();
                    } else {
                        throw new TypeMismatchError("DevCycle variable for key " + key + " is not a JSON object");
                    }
                }
            } catch (IllegalArgumentException e) {
                return ProviderEvaluation.<Value>builder()
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

    @Override
    public void shutdown() {
        devcycleClient.close();
    }

    <T> ProviderEvaluation<T> resolvePrimitiveVariable(String key, T defaultValue, EvaluationContext ctx) {
        if (devcycleClient.isInitialized()) {
            try {
                DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);

                Variable<T> variable = devcycleClient.variable(user, key, defaultValue);

                if (variable == null || variable.getIsDefaulted()) {
                    return ProviderEvaluation.<T>builder()
                            .value(defaultValue)
                            .reason(Reason.DEFAULT.toString())
                            .build();
                } else {
                    T value = variable.getValue();
                    if (variable.getType() == Variable.TypeEnum.NUMBER && defaultValue.getClass() == Integer.class) {
                        // Internally in the DevCycle SDK all number values are stored as Doubles
                        // need to explicitly convert to an Integer if the requested type is Integer
                        Number numVal = (Number) value;
                        value = (T) Integer.valueOf(numVal.intValue());
                    }

                    return ProviderEvaluation.<T>builder()
                            .value(value)
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
