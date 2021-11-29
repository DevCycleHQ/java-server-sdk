package com.devcycle.sdk.server.api;

import com.devcycle.sdk.server.helpers.TestResponse;
import com.devcycle.sdk.server.model.*;
import retrofit2.Call;

import java.util.Map;

public class DVCApiMock implements DVCApi {

    @Override
    public Call<Map<String, Feature>> getFeatures(User user) {
        return TestResponse.getFeatures();
    }

    @Override
    public <T> Call<Variable<T>> getVariableByKey(User user, String key) {
        return TestResponse.getVariableByKey();
    }

    @Override
    public Call<Map<String, Variable<?>>> getVariables(User user) {
        return TestResponse.getVariables();
    }

    @Override
    public Call<DVCResponse> track(UserAndEvents userAndEvents) {
        return TestResponse.getTrackResponse(1);
    }
}
