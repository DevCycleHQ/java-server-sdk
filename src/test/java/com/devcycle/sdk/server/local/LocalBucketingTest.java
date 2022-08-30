package com.devcycle.sdk.server.local;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalBucketingTest {

    private LocalBucketing localBucketing;
    private final String testConfigString = "{\"project\":{\"_id\":\"61f97628ff4afcb6d057dbf0\",\"key\":\"emma-project\",\"a0_organization\":\"org_tPyJN5dvNNirKar7\",\"settings\":{\"edgeDB\":{\"enabled\":false},\"optIn\":{\"enabled\":true,\"title\":\"EarlyAccess\",\"description\":\"Getearlyaccesstobetafeaturesbelow!\",\"imageURL\":\"https://a-url.url.com\",\"colors\":{\"primary\":\"#531cd9\",\"secondary\":\"#16dec0\"}}}},\"environment\":{\"_id\":\"61f97628ff4afcb6d057dbf2\",\"key\":\"development\"},\"features\":[{\"_id\":\"62fbf6566f1ba302829f9e32\",\"key\":\"a-cool-new-feature\",\"type\":\"release\",\"variations\":[{\"key\":\"variation-on\",\"name\":\"VariationOn\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":true}],\"_id\":\"62fbf6566f1ba302829f9e38\"},{\"key\":\"variation-off\",\"name\":\"VariationOff\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":false}],\"_id\":\"62fbf6566f1ba302829f9e39\"}],\"configuration\":{\"_id\":\"62fbf6576f1ba302829f9e4d\",\"targets\":[{\"_audience\":{\"_id\":\"630fa3fab2eadb342048ec2a\",\"filters\":{\"filters\":[{\"type\":\"all\",\"values\":[],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e38\",\"percentage\":1}],\"_id\":\"630fa3fab2eadb342048ec2c\"}],\"forcedUsers\":{}}}],\"variables\":[{\"_id\":\"62fbf6566f1ba302829f9e34\",\"key\":\"a-cool-new-feature\",\"type\":\"Boolean\"}],\"variableHashes\":{\"a-cool-new-feature\":1868656757}}";
    private final String user = "{\"user_id\":\"hi\",\"platform\":\"platform\",\"platformVersion\":\"string\",\"sdkType\":\"string\",\"sdkVersion\":\"string\",\"deviceModel\":\"string\"}";
    private final String platformData = "{\"platform\":\"platform\",\"platformVersion\":\"string\",\"sdkType\":\"string\",\"sdkVersion\":\"string\",\"deviceModel\":\"string\"}";
    @Test
    public void storeConfigTest() throws JsonProcessingException {
        localBucketing = new LocalBucketing();
        localBucketing.storeConfig("test-token", testConfigString);
        localBucketing.setPlatformData(platformData);
        localBucketing.generateBucketedConfig("test-token", user);
    }

}