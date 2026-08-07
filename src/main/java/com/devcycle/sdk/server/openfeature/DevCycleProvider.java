package com.devcycle.sdk.server.openfeature;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.exception.DevCycleException;
import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.EvalReason;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.managers.ConfigUpdateListener;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.EventProvider;
import dev.openfeature.sdk.ImmutableMetadata;
import dev.openfeature.sdk.ImmutableMetadata.ImmutableMetadataBuilder;
import dev.openfeature.sdk.Metadata;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.ProviderEventDetails;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.TrackingEventDetails;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.FatalError;
import dev.openfeature.sdk.exceptions.GeneralError;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import dev.openfeature.sdk.exceptions.TypeMismatchError;

public class DevCycleProvider extends EventProvider implements ConfigUpdateListener {
    private static final String PROVIDER_NAME = "DevCycle";
    private static final long DEFAULT_INIT_TIMEOUT_MS = 2000;

    private final IDevCycleClient devcycleClient;
    private final long initTimeoutMS;

    /**
     * Released once the client has a config to serve, or once we know it never will.
     */
    private final CountDownLatch initialConfigLatch = new CountDownLatch(1);

    private final Object stateLock = new Object();

    /**
     * True while the last config fetch attempt was a failure, so recovery is reported once rather
     * than emitting a duplicate event on every failed poll. Guarded by {@link #stateLock}.
     */
    private boolean degraded;

    /**
     * True once {@link #initialize(EvaluationContext)} has failed. The SDK will not call it again,
     * so a later successful fetch has to emit PROVIDER_READY itself. Guarded by {@link #stateLock}.
     */
    private boolean initializeFailed;

    /**
     * Set when the config can never be fetched, ie. the SDK key is unauthorized. Guarded by
     * {@link #stateLock}.
     */
    private DevCycleException fatalError;

    public DevCycleProvider(IDevCycleClient devcycleClient) {
        this(devcycleClient, DEFAULT_INIT_TIMEOUT_MS);
    }

    DevCycleProvider(IDevCycleClient devcycleClient, long initTimeoutMS) {
        this.devcycleClient = devcycleClient;
        this.initTimeoutMS = initTimeoutMS;
    }

    @Override
    public Metadata getMetadata() {
        return () -> PROVIDER_NAME + " " + devcycleClient.getSDKPlatform();
    }

    /**
     * The OpenFeature SDK emits PROVIDER_READY when this returns and PROVIDER_ERROR when it throws,
     * so this method never emits those events itself. Throwing a {@link FatalError} tells the SDK
     * the provider is not recoverable, which is the right signal for an unauthorized SDK key.
     */
    @Override
    public void initialize(EvaluationContext evaluationContext) throws Exception {
        if (devcycleClient.isInitialized()) {
            return;
        }

        try {
            initialConfigLatch.await(initTimeoutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // await() clears the interrupt flag, restore it so callers can still see the interrupt
            Thread.currentThread().interrupt();
            throw e;
        }

        synchronized (stateLock) {
            if (fatalError != null) {
                throw new FatalError("DevCycle client cannot be initialized: " + fatalError.getMessage());
            }

            if (!devcycleClient.isInitialized()) {
                initializeFailed = true;
                throw new GeneralError("DevCycle client not initialized within " + initTimeoutMS + "ms");
            }
        }
    }

    @Override
    public void shutdown() {
        // drains the event emitter executor owned by EventProvider
        super.shutdown();
        devcycleClient.close();
    }

    /**
     * Called by the DevCycle Local client when a config fetch succeeds. Not intended to be called
     * directly.
     */
    @Override
    public void onConfigLoaded(String configETag, boolean firstLoad, boolean changed) {
        boolean recovered;
        synchronized (stateLock) {
            recovered = degraded || initializeFailed;
            degraded = false;
            initializeFailed = false;
            if (firstLoad) {
                initialConfigLatch.countDown();
            }
        }

        if (recovered) {
            // clears the ERROR or STALE state the SDK recorded for the earlier failure. Not needed
            // on a clean first load, where the SDK emits PROVIDER_READY once initialize() returns
            emitProviderReady(ProviderEventDetails.builder()
                    .message("DevCycle config fetching has recovered")
                    .eventMetadata(configEventMetadata(configETag))
                    .build());
        }

        if (changed && !firstLoad) {
            // flagsChanged is intentionally left unset: DevCycle resolves variables per-user at
            // evaluation time, so the set of keys whose value changed is not knowable here
            emitProviderConfigurationChanged(ProviderEventDetails.builder()
                    .message("DevCycle config was updated")
                    .eventMetadata(configEventMetadata(configETag))
                    .build());
        }
    }

    /**
     * Called by the DevCycle Local client when a config fetch fails. Not intended to be called
     * directly.
     */
    @Override
    public void onConfigError(DevCycleException error, boolean fatal) {
        boolean report;
        synchronized (stateLock) {
            if (fatal) {
                if (fatalError != null) {
                    // already reported, the SDK is holding the provider in the FATAL state
                    return;
                }
                fatalError = error;
                // unblocks initialize() so it fails immediately rather than waiting out the timeout
                initialConfigLatch.countDown();
            }
            report = fatal || !degraded;
            degraded = true;
        }

        if (fatal) {
            emitProviderError(ProviderEventDetails.builder()
                    .errorCode(ErrorCode.PROVIDER_FATAL)
                    .message(error.getMessage())
                    .build());
            return;
        }

        if (!report) {
            // already reported, don't emit an event for every subsequent failed poll
            DevCycleLogger.debug("DevCycle config fetch still failing: " + error.getMessage());
            return;
        }

        if (devcycleClient.isInitialized()) {
            // a previously fetched config is still being served, so evaluations remain usable
            emitProviderStale(ProviderEventDetails.builder()
                    .message("DevCycle config could not be refreshed, serving the last known config: "
                            + error.getMessage())
                    .build());
        } else {
            emitProviderError(ProviderEventDetails.builder()
                    .errorCode(ErrorCode.GENERAL)
                    .message(error.getMessage())
                    .build());
        }
    }

    private ImmutableMetadata configEventMetadata(String configETag) {
        ImmutableMetadataBuilder builder = ImmutableMetadata.builder();
        if (configETag != null && !configETag.isEmpty()) {
            builder.addString("configETag", configETag);
        }
        return builder.build();
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
