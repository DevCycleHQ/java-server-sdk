package com.devcycle.sdk.server.local;

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.devcycle.sdk.server.common.model.Feature;
import com.devcycle.sdk.server.common.model.PlatformData;
import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.helpers.WhiteBox;
import com.devcycle.sdk.server.local.api.DVCLocalClient;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.DVCLocalOptions;

@RunWith(MockitoJUnitRunner.class)
public class DVCLocalClientTest {

    private static DVCLocalClient client;
    static final String testConfigString = "{\"project\":{\"_id\":\"61f97628ff4afcb6d057dbf0\",\"key\":\"emma-project\",\"a0_organization\":\"org_tPyJN5dvNNirKar7\",\"settings\":{\"edgeDB\":{\"enabled\":false},\"optIn\":{\"enabled\":true,\"title\":\"EarlyAccess\",\"description\":\"Getearlyaccesstobetafeaturesbelow!\",\"imageURL\":\"\",\"colors\":{\"primary\":\"#531cd9\",\"secondary\":\"#16dec0\"}}}},\"environment\":{\"_id\":\"61f97628ff4afcb6d057dbf2\",\"key\":\"development\"},\"features\":[{\"_id\":\"62fbf6566f1ba302829f9e32\",\"key\":\"a-cool-new-feature\",\"type\":\"release\",\"variations\":[{\"key\":\"variation-on\",\"name\":\"VariationOn\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":true},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"variationOff\"}],\"_id\":\"62fbf6566f1ba302829f9e38\"},{\"key\":\"variation-off\",\"name\":\"VariationOff\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":false},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"variationOn\"}],\"_id\":\"62fbf6566f1ba302829f9e39\"}],\"configuration\":{\"_id\":\"62fbf6576f1ba302829f9e4d\",\"targets\":[{\"_audience\":{\"_id\":\"63125321d31c601f992288b6\",\"filters\":{\"filters\":[{\"type\":\"user\",\"subType\":\"email\",\"comparator\":\"=\",\"values\":[\"giveMeVariationOff@email.com\"],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e38\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bb\"},{\"_audience\":{\"_id\":\"63125321d31c601f992288b7\",\"filters\":{\"filters\":[{\"type\":\"all\",\"values\":[],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e39\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bc\"}],\"forcedUsers\":{}}}],\"variables\":[{\"_id\":\"62fbf6566f1ba302829f9e34\",\"key\":\"a-cool-new-feature\",\"type\":\"Boolean\"},{\"_id\":\"63125320a4719939fd57cb2b\",\"key\":\"string-var\",\"type\":\"String\"}],\"variableHashes\":{\"a-cool-new-feature\":1868656757,\"string-var\":2413071944}}";
    static final String apiKey = String.format("server-%s", UUID.randomUUID());
    private static LocalBucketing localBucketing;
    private static EventQueueManager eventQueueManager;

    @BeforeClass
    public static void setup() throws Exception {
        client = new DVCLocalClient(apiKey);
        localBucketing = new LocalBucketing();
        localBucketing.storeConfig(apiKey, testConfigString);
        localBucketing.setPlatformData(PlatformData.builder().build().toString());
        eventQueueManager = new EventQueueManager(apiKey, localBucketing, DVCLocalOptions.builder().build());
        WhiteBox.setInternalState(client, "localBucketing", localBucketing);
        WhiteBox.setInternalState(client, "eventQueueManager", eventQueueManager);
    }
    @Test
    public void variableTest() {
        User user = getUser();
        user.setEmail("giveMeVariationOff@email.com");
        Variable<String> var = client.variable(user, "string-var", "default string");
        Assert.assertEquals("variationOff", var.getValue());

        user.setEmail("giveMeVariationOn@email.com");
        var = client.variable(user, "string-var", "default string");
        Assert.assertEquals("variationOn", var.getValue());
    }

    @Test
    public void allFeaturesTest() {
        User user = getUser();
        Map<String, Feature> features = client.allFeatures(user);
        Assert.assertEquals(features.get("a-cool-new-feature").getId(), "62fbf6566f1ba302829f9e32");
        Assert.assertEquals(features.size(), 1);
    }

    @Test
    public void allVariablesTest() {
        User user = getUser();
        Map<String, Variable> variables = client.allVariables(user);
        Assert.assertEquals(variables.get("string-var").getId(), "63125320a4719939fd57cb2b");
        Assert.assertEquals(variables.get("a-cool-new-feature").getId(), "62fbf6566f1ba302829f9e34");
        Assert.assertEquals(variables.size(), 2);
    }

    private User getUser() {
        return User.builder()
                .userId("j_test")
                .build();
    }
}