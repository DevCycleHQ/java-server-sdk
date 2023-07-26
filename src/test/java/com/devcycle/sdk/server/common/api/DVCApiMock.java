package com.devcycle.sdk.server.common.api;

import java.util.Map;

import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.helpers.TestResponse;
import com.devcycle.sdk.server.local.model.EventsBatch;

import retrofit2.Call;

public class DVCApiMock implements IDevCycleApi {

    @Override
    public Call<Map<String, Feature>> getFeatures(DevCycleUser user, Boolean enabledEdgeDB) {
        return TestResponse.getFeatures();
    }

    @Override
    public Call<Variable> getVariableByKey(DevCycleUser user, String key, Boolean enabledEdgeDB) {
        return TestResponse.getVariableByKey();
    }

    @Override
    public Call<Map<String, BaseVariable>> getVariables(DevCycleUser user, Boolean enabledEdgeDB) {
        return TestResponse.getVariables();
    }

    @Override
    public Call<DevCycleResponse> track(DevCycleUserAndEvents userAndEvents, Boolean enableEdgeDB) {
        return TestResponse.getTrackResponse(1);
    }

    @Override
    public Call<ProjectConfig> getConfig(String sdkToken, String etag) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Call<DevCycleResponse> publishEvents(EventsBatch eventsBatch) {
        // TODO Auto-generated method stub
        return null;
    }
}
