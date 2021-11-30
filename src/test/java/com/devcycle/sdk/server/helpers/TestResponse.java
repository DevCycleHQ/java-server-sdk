package com.devcycle.sdk.server.helpers;

import com.devcycle.sdk.server.model.DVCResponse;
import com.devcycle.sdk.server.model.Feature;
import com.devcycle.sdk.server.model.Variable;
import retrofit2.Call;
import retrofit2.mock.Calls;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public static Call<Map<String, Variable>> getVariables() {
        HashMap<String, Variable> variables = new HashMap<>();
        variables.put("test-false", Variable.builder()
                .id(UUID.randomUUID().toString())
                .key("test-false")
                .value(false)
                .type(Variable.TypeEnum.BOOLEAN)
                .build());
        variables.put("test-true", Variable.builder()
                .id(UUID.randomUUID().toString())
                .key("test-true")
                .value(true)
                .type(Variable.TypeEnum.BOOLEAN)
                .build());
        variables.put("test-number", Variable.builder()
                .id(UUID.randomUUID().toString())
                .key("test-number")
                .value(100)
                .type(Variable.TypeEnum.NUMBER)
                .build());
        variables.put("test-json", Variable.builder()
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
                .id(UUID.randomUUID().toString())
                .key("test-false")
                .value(false)
                .type(Variable.TypeEnum.BOOLEAN)
                .build();

        return Calls.response(variable);
    }

    public static Call<DVCResponse> getTrackResponse(int count) {
        DVCResponse dvcResponse = DVCResponse.builder()
                .message(String.format("Successfully received %d events", count))
                .build();

        return Calls.response(dvcResponse);
    }
}
