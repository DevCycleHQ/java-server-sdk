package com.devcycle.sdk.server.local;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.devcycle.sdk.server.common.model.PlatformData;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.devcycle.sdk.server.common.model.DevCycleEvent;
import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.model.FlushPayload;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class LocalBucketingTest {

    final String testConfigString = "{\"project\":{\"_id\":\"61f97628ff4afcb6d057dbf0\",\"key\":\"emma-project\",\"a0_organization\":\"org_tPyJN5dvNNirKar7\",\"settings\":{\"edgeDB\":{\"enabled\":false},\"optIn\":{\"enabled\":true,\"title\":\"EarlyAccess\",\"description\":\"Getearlyaccesstobetafeaturesbelow!\",\"imageURL\":\"\",\"colors\":{\"primary\":\"#531cd9\",\"secondary\":\"#16dec0\"}}}},\"environment\":{\"_id\":\"61f97628ff4afcb6d057dbf2\",\"key\":\"development\"},\"features\":[{\"_id\":\"62fbf6566f1ba302829f9e32\",\"key\":\"a-cool-new-feature\",\"type\":\"release\",\"variations\":[{\"key\":\"variation-on\",\"name\":\"VariationOn\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":true},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"variationOff\"}],\"_id\":\"62fbf6566f1ba302829f9e38\"},{\"key\":\"variation-off\",\"name\":\"VariationOff\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":false},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"variationOn\"}],\"_id\":\"62fbf6566f1ba302829f9e39\"}],\"configuration\":{\"_id\":\"62fbf6576f1ba302829f9e4d\",\"targets\":[{\"_audience\":{\"_id\":\"63125321d31c601f992288b6\",\"filters\":{\"filters\":[{\"type\":\"user\",\"subType\":\"email\",\"comparator\":\"=\",\"values\":[\"giveMeVariationOff@email.com\"],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e38\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bb\"},{\"_audience\":{\"_id\":\"63125321d31c601f992288b7\",\"filters\":{\"filters\":[{\"type\":\"all\",\"values\":[],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e39\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bc\"}],\"forcedUsers\":{}}}],\"variables\":[{\"_id\":\"62fbf6566f1ba302829f9e34\",\"key\":\"a-cool-new-feature\",\"type\":\"Boolean\"},{\"_id\":\"63125320a4719939fd57cb2b\",\"key\":\"string-var\",\"type\":\"String\"}],\"variableHashes\":{\"a-cool-new-feature\":1868656757,\"string-var\":2413071944}}";
    final String varMap = "{\"a-cool-new-feature\":{\"_feature\":\"62fbf6566f1ba302829f9e32\",\"_variation\":\"62fbf6566f1ba302829f9e39\"},\"string-var\":{\"_feature\":\"62fbf6566f1ba302829f9e32\",\"_variation\":\"62fbf6566f1ba302829f9e39\"}}";

    final String apiKey = String.format("server-%s", UUID.randomUUID());
    final LocalBucketing localBucketing = new LocalBucketing();

    final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup(){
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        localBucketing.setPlatformData(getUser().getPlatformData().toString());
        localBucketing.storeConfig(apiKey, testConfigString);
    }

    @Test
    public void testSetClientCustomData() {
        Map<String,Object> testData = new HashMap();
        testData.put("stringProp", "test");
        testData.put("intProp", 1);
        testData.put("booleanProp", true);

        try {
            String customData = mapper.writeValueAsString(testData);
            localBucketing.setClientCustomData(apiKey, customData);
        } catch (JsonProcessingException e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testSetPlatformData(){
        try {
            PlatformData platformData = PlatformData.builder().build();
            String platformDataJSON = mapper.writeValueAsString(platformData);
            localBucketing.setPlatformData(platformDataJSON);
        }catch(Exception e){
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testStoreConfig(){
        try {
            localBucketing.storeConfig(apiKey, testConfigString);
        }catch(Exception e){
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testEventQueue() throws JsonProcessingException {
        DevCycleEvent event = DevCycleEvent.builder().type("test").target("target").build();

        localBucketing.initEventQueue(apiKey, "{}");

        // Add 2 events, aggregated by same target (should create 1 event with eventCount 2)
        localBucketing.queueEvent(apiKey, mapper.writeValueAsString(getUser()), mapper.writeValueAsString(event));
        localBucketing.queueAggregateEvent(apiKey, mapper.writeValueAsString(event), varMap);
        FlushPayload[] payloads = localBucketing.flushEventQueue(apiKey);
        Assert.assertEquals(payloads.length, 1);
        Assert.assertEquals(payloads[0].eventCount, 2);

        // Check event queue size
        int eventQueueSize = localBucketing.getEventQueueSize(apiKey);
        Assert.assertEquals(eventQueueSize, 2);

        // Callback payload failure, retryable (should keep events)
        localBucketing.onPayloadFailure(apiKey, payloads[0].payloadId, true);
        payloads = localBucketing.flushEventQueue(apiKey);
        Assert.assertEquals(payloads.length, 1); // failed events not deleted

        // Callback payload failure, NOT retryable (should clear events)
        localBucketing.onPayloadFailure(apiKey, payloads[0].payloadId, false);
        payloads = localBucketing.flushEventQueue(apiKey);
        Assert.assertEquals(payloads.length, 0); // failed events deleted

        // Add another event
        localBucketing.queueEvent(apiKey, mapper.writeValueAsString(getUser()), mapper.writeValueAsString(event));
        payloads = localBucketing.flushEventQueue(apiKey);
        Assert.assertEquals(payloads.length, 1);

        //Callback payload success, should clear events
        localBucketing.onPayloadSuccess(apiKey, payloads[0].payloadId);
        payloads = localBucketing.flushEventQueue(apiKey);
        Assert.assertEquals(payloads.length, 0); // succeeded events deleted
    }

    private DevCycleUser getUser() {
        return DevCycleUser.builder()
                .userId("j_test")
                .build();
    }

}