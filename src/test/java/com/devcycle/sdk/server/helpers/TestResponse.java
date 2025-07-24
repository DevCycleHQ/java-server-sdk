package com.devcycle.sdk.server.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.devcycle.sdk.server.common.model.BaseVariable;
import com.devcycle.sdk.server.common.model.DevCycleResponse;
import com.devcycle.sdk.server.common.model.EvalReason;
import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.Variable;

import retrofit2.Call;
import retrofit2.mock.Calls;

public final class TestResponse {

    private TestResponse() {
    }

    public static Call<Map<String, Feature>> getFeatures() {
        Map<String, Feature> features = new HashMap<>();

        features.put("show-feature-history", Feature.builder()
                .id(UUID.randomUUID().toString())
                .key("show-feature-history")
                .type(Feature.TypeEnum.RELEASE)
                .build());
        features.put("show-insights", Feature.builder()
                .id(UUID.randomUUID().toString())
                .key("show-insights")
                .type(Feature.TypeEnum.RELEASE)
                .build());
        features.put("show-quickstart", Feature.builder()
                .id(UUID.randomUUID().toString())
                .key("show-quickstart")
                .type(Feature.TypeEnum.RELEASE)
                .build());
        features.put("show-change-plans", Feature.builder()
                .id(UUID.randomUUID().toString())
                .key("show-change-plans")
                .type(Feature.TypeEnum.RELEASE)
                .build());
        features.put("enable-gtm", Feature.builder()
                .id(UUID.randomUUID().toString())
                .key("enable-gtm")
                .type(Feature.TypeEnum.RELEASE)
                .build());

        return Calls.response(features);
    }

    public static Call<Map<String, BaseVariable>> getVariables() {
        HashMap<String, BaseVariable> variables = new HashMap<>();
        variables.put("test-false", BaseVariable.builder()
                .id(UUID.randomUUID().toString())
                .key("test-false")
                .value(false)
                .type(Variable.TypeEnum.BOOLEAN)
                .build());
        variables.put("test-true", BaseVariable.builder()
                .id(UUID.randomUUID().toString())
                .key("test-true")
                .value(true)
                .type(Variable.TypeEnum.BOOLEAN)
                .build());
        variables.put("test-number", BaseVariable.builder()
                .id(UUID.randomUUID().toString())
                .key("test-number")
                .value(100)
                .type(Variable.TypeEnum.NUMBER)
                .build());
        variables.put("test-json", BaseVariable.builder()
                .id(UUID.randomUUID().toString())
                .key("test-json")
                .value("{'some':'json''}")
                .type(Variable.TypeEnum.JSON)
                .build());

        return Calls.response(variables);
    }

    @SuppressWarnings("unchecked")
    public static <T> Call<Variable> getVariableByKey() {
        Variable<T> variable = (Variable<T>) Variable.builder()
                .key("test-false")
                .value(false)
                .type(Variable.TypeEnum.BOOLEAN)
                .eval(new EvalReason("TARGETING_MATCH", "All Users", "test_cloud_target_id"))
                .build();

        return Calls.response(variable);
    }

    public static Call<DevCycleResponse> getTrackResponse(int count) {
        DevCycleResponse dvcResponse = DevCycleResponse.builder()
                .message(String.format("Successfully received %d events", count))
                .build();

        return Calls.response(dvcResponse);
    }
}
