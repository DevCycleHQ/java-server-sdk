package com.devcycle.sdk.server.common.api;

import java.util.Map;

import com.devcycle.sdk.server.common.model.BaseVariable;
import com.devcycle.sdk.server.common.model.DevCycleResponse;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.DevCycleUserAndEvents;
import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.ProjectConfig;
import com.devcycle.sdk.server.common.model.Variable;
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
        return TestResponse.getVariableByKey(key);
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
    public Call<ProjectConfig> getConfig(String sdkToken, String etag, String lastModified) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Call<DevCycleResponse> publishEvents(EventsBatch eventsBatch) {
        // TODO Auto-generated method stub
        return null;
    }
}
