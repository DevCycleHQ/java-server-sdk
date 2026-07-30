package com.devcycle.sdk.server.openfeature;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.EvalReason;
import com.devcycle.sdk.server.common.model.Variable;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ImmutableMetadata;
import dev.openfeature.sdk.ImmutableMetadata.ImmutableMetadataBuilder;
import dev.openfeature.sdk.Metadata;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.TrackingEventDetails;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.GeneralError;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import dev.openfeature.sdk.exceptions.TypeMismatchError;

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
    public void initialize(EvaluationContext evaluationContext) throws Exception {
        if (devcycleClient.isInitialized()) {
            return;
        }

        long deadline = 2 * 1000; // Delay in milliseconds
        long start = System.currentTimeMillis();

        do {
            if (deadline <= System.currentTimeMillis() - start) {
                throw new GeneralError("DevCycle client not initialized within 2 seconds");
            }
            Thread.sleep(5);
        } while (!devcycleClient.isInitialized());
    }

    @Override
    public void shutdown() {
        devcycleClient.close();
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

        if (!devcycleClient.isInitialized()) {
            throw new ProviderNotReadyError("DevCycle client not initialized");
        }

        try {
            DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);

            Variable<Object> variable = devcycleClient.variable(user, key, defaultValue.asStructure().asObjectMap());
            

            if (variable == null || variable.getIsDefaulted()) {
                ImmutableMetadata flagMetadata = null;
                if (variable != null && variable.getEval() != null) {
                    EvalReason eval = variable.getEval();
                    flagMetadata = getFlagMetadata(eval);
                }
                return ProviderEvaluation.<Value>builder()
                        .value(defaultValue)
                        .reason(Reason.DEFAULT.toString())
                        .flagMetadata(flagMetadata)
                        .build();
            } else {
                if (variable.getValue() instanceof Map) {
                    // JSON objects are managed as Map implementations and must be converted to an OpenFeature structure
                    Value objectValue = new Value(Structure.mapToStructure((Map) variable.getValue()));

                    ImmutableMetadata flagMetadata = null;
                    String evalReason = Reason.TARGETING_MATCH.toString();
                    if (variable.getEval() != null) {
                        EvalReason eval = variable.getEval();
                        evalReason = eval.getReason();
                        flagMetadata = getFlagMetadata(eval);
                    }

                    return ProviderEvaluation.<Value>builder()
                            .value(objectValue)
                            .reason(evalReason)
                            .flagMetadata(flagMetadata)
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
    }

    <T> ProviderEvaluation<T> resolvePrimitiveVariable(String key, T defaultValue, EvaluationContext ctx) {
        if (!devcycleClient.isInitialized()) {
            throw new ProviderNotReadyError("DevCycle client not initialized");
        }

        try {
            DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);

            Variable<T> variable = devcycleClient.variable(user, key, defaultValue);

            if (variable == null || variable.getIsDefaulted()) {
                ImmutableMetadata flagMetadata = null;
                if (variable != null && variable.getEval() != null) {
                    EvalReason eval = variable.getEval();
                    flagMetadata = getFlagMetadata(eval);
                }
                return ProviderEvaluation.<T>builder()
                        .value(defaultValue)
                        .reason(Reason.DEFAULT.toString())
                        .flagMetadata(flagMetadata)
                        .build();
            } else {
                T value = variable.getValue();
                if (variable.getType() == Variable.TypeEnum.NUMBER && defaultValue.getClass() == Integer.class) {
                    // Internally in the DevCycle SDK all number values are stored as Doubles
                    // need to explicitly convert to an Integer if the requested type is Integer
                    Number numVal = (Number) value;
                    value = (T) Integer.valueOf(numVal.intValue());
                }

                ImmutableMetadata flagMetadata = null;
                String evalReason = Reason.TARGETING_MATCH.toString();
                if (variable.getEval() != null) {
                    EvalReason eval = variable.getEval();
                    evalReason = eval.getReason();
                    flagMetadata = getFlagMetadata(eval);
                }

                return ProviderEvaluation.<T>builder()
                        .value(value)
                        .reason(evalReason)
                        .flagMetadata(flagMetadata)
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
    }

    @Override
    public void track(String eventName, EvaluationContext context, TrackingEventDetails details) {
        if (!devcycleClient.isInitialized()) {
            throw new ProviderNotReadyError("DevCycle client not initialized");
        }

        DevCycleUser user = DevCycleUser.fromEvaluationContext(context);
        try {
            BigDecimal eventValue = extractEventValue(details);
            Map<String, Object> metaData = getMetadataWithoutValue(details);

            DevCycleEvent event = DevCycleEvent.builder()
                    .type(eventName)
                    .value(eventValue)
                    .metaData(metaData)
                    .build();
            devcycleClient.track(user, event);
        } catch (DevCycleException e) {
            throw new GeneralError(e);
        }
    }

    private BigDecimal extractEventValue(TrackingEventDetails details) {
        Optional<Number> rawValue = details.getValue();
        if (rawValue.isEmpty()) {
            return null;
        }

        Number numberValue = rawValue.get();
        if (numberValue == null) {
            return null;
        }

        Value value = Value.objectToValue(numberValue);
        return value.isNumber() ? new BigDecimal(Double.toString(value.asDouble())) : null;
    }

    private Map<String, Object> getMetadataWithoutValue(TrackingEventDetails details) {
        Map<String, Object> metaData = details.asObjectMap();
        metaData.remove("value");
        return metaData;
    }

    private ImmutableMetadata getFlagMetadata(EvalReason evalReason) {
        ImmutableMetadataBuilder flagMetadataBuilder = ImmutableMetadata.builder();

        if (evalReason.getDetails() != null) {
            flagMetadataBuilder.addString("evalReasonDetails", evalReason.getDetails());
        }

        if (evalReason.getTargetId() != null) {
            flagMetadataBuilder.addString("evalReasonTargetId", evalReason.getTargetId());
        }
        return flagMetadataBuilder.build();
    }
}
