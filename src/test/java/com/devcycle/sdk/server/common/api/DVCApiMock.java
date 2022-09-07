package com.devcycle.sdk.server.common.api;

import com.devcycle.sdk.server.helpers.TestResponse;
import com.devcycle.sdk.server.common.model.*;
import retrofit2.Call;

import java.util.Map;

public class DVCApiMock implements IDVCApi {

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
    public Call<DVCResponse> track(UserAndEvents userAndEvents, Boolean enableEdgeDB) {
        return TestResponse.getTrackResponse(1);
    }

    @Override
    public Call<ProjectConfig> getConfig(String sdkToken, String etag) {
        // TODO Auto-generated method stub
        return null;
    }
}
