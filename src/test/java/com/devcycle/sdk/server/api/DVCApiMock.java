package com.devcycle.sdk.server.api;

import com.devcycle.sdk.server.helpers.TestResponse;
import com.devcycle.sdk.server.model.*;
import retrofit2.Call;

import java.util.Map;

public class DVCApiMock implements DVCApi {

    @Override
    public Call<Map<String, Feature>> getFeatures(User user, Boolean enabledEdgeDB) {
        return TestResponse.getFeatures();
    }

    @Override
    public Call<Variable> getVariableByKey(User user, String key, Boolean enabledEdgeDB) {
        return TestResponse.getVariableByKey();
    }

    @Override
    public Call<Map<String, Variable>> getVariables(User user, Boolean enabledEdgeDB) {
        return TestResponse.getVariables();
    }

    @Override
    public Call<DVCResponse> track(UserAndEvents userAndEvents) {
        return TestResponse.getTrackResponse(1);
    }
}
