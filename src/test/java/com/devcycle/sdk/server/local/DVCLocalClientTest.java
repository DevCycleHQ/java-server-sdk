package com.devcycle.sdk.server.local;

import com.devcycle.sdk.server.common.model.User;
import com.devcycle.sdk.server.common.model.Variable;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DVCLocalClientTest {

    private DVCLocalClient client;
    String testConfigString = "{\"project\":{\"_id\":\"61f97628ff4afcb6d057dbf0\",\"key\":\"emma-project\",\"a0_organization\":\"org_tPyJN5dvNNirKar7\",\"settings\":{\"edgeDB\":{\"enabled\":false},\"optIn\":{\"enabled\":true,\"title\":\"EarlyAccess\",\"description\":\"Getearlyaccesstobetafeaturesbelow!\",\"imageURL\":\"https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR68cgQT_BTgnhWTdfjUXSN8zM9Vpxgq82dhw&usqp=CAU\",\"colors\":{\"primary\":\"#531cd9\",\"secondary\":\"#16dec0\"}}}},\"environment\":{\"_id\":\"61f97628ff4afcb6d057dbf2\",\"key\":\"development\"},\"features\":[{\"_id\":\"62fbf6566f1ba302829f9e32\",\"key\":\"a-cool-new-feature\",\"type\":\"release\",\"variations\":[{\"key\":\"variation-on\",\"name\":\"VariationOn\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":true},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"iamlocal\"}],\"_id\":\"62fbf6566f1ba302829f9e38\"},{\"key\":\"variation-off\",\"name\":\"VariationOff\",\"variables\":[{\"_var\":\"62fbf6566f1ba302829f9e34\",\"value\":false},{\"_var\":\"63125320a4719939fd57cb2b\",\"value\":\"iamcloud\"}],\"_id\":\"62fbf6566f1ba302829f9e39\"}],\"configuration\":{\"_id\":\"62fbf6576f1ba302829f9e4d\",\"targets\":[{\"_audience\":{\"_id\":\"63125321d31c601f992288b6\",\"filters\":{\"filters\":[{\"type\":\"user\",\"subType\":\"platform\",\"comparator\":\"=\",\"values\":[\"java-local\"],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e38\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bb\"},{\"_audience\":{\"_id\":\"63125321d31c601f992288b7\",\"filters\":{\"filters\":[{\"type\":\"all\",\"values\":[],\"filters\":[]}],\"operator\":\"and\"}},\"distribution\":[{\"_variation\":\"62fbf6566f1ba302829f9e39\",\"percentage\":1}],\"_id\":\"63125321d31c601f992288bc\"}],\"forcedUsers\":{}}}],\"variables\":[{\"_id\":\"62fbf6566f1ba302829f9e34\",\"key\":\"a-cool-new-feature\",\"type\":\"Boolean\"},{\"_id\":\"63125320a4719939fd57cb2b\",\"key\":\"string-var\",\"type\":\"String\"}],\"variableHashes\":{\"a-cool-new-feature\":1868656757,\"string-var\":2413071944}}";

    @Test
    public void variableTest() throws JsonProcessingException {
        client = new DVCLocalClient("test-token");
        // TODO actually mock the config with testConfigString instead of hard coding it in DVCLocalCLient
        User user = getUser();
        user.setPlatform("java-local"); //this test config buckets 'java-local' platform to variation on, else off
        Variable<String> var = client.variable(user, "string-var", "default string");
        Assert.assertEquals("iamlocal", var.getValue());
    }

    private User getUser() {
        return User.builder()
                .userId("j_test")
                .build();
    }

}