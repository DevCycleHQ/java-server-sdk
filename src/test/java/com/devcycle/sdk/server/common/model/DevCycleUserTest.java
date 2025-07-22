package com.devcycle.sdk.server.common.model;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class DevCycleUserTest {

    @Test
    public void testCreateUserNoUserID() {
        MutableContext ctx = new MutableContext();

        try {
            DevCycleUser.fromEvaluationContext(ctx);
            Assert.fail("Expected exception");
        } catch (TargetingKeyMissingError e) {
            // expected
        }

        Map<String, Value> attribs = new LinkedHashMap();
        ctx = new MutableContext(null, attribs);

        try {
            DevCycleUser.fromEvaluationContext(ctx);
            Assert.fail("Expected exception");
        } catch (TargetingKeyMissingError e) {
            // expected
        }

        attribs = new LinkedHashMap();
        attribs.put("user_id", new Value(999));
        ctx = new MutableContext(null, attribs);

        try {
            DevCycleUser.fromEvaluationContext(ctx);
            Assert.fail("Expected exception");
        } catch (TargetingKeyMissingError e) {
            // expected
        }
    }

    @Test
    public void testCreateUserOnlyUserId() {
        EvaluationContext ctx = new MutableContext("test-1234");
        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "test-1234");

        Map<String, Value> apiAttrs = new LinkedHashMap();
        apiAttrs.put("user_id", new Value("test-6789"));

        // ensure fallback to user_id when target key is null
        ctx = new MutableContext(null, apiAttrs);
        user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "test-6789");

        // ensure fallback to user_id when target key is empty
        ctx = new MutableContext("", apiAttrs);
        user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "test-6789");

        // ensure target key takes precedence over user_id
        ctx = new MutableContext("user-4567", apiAttrs);
        user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "user-4567");
    }

    @Test
    public void testFromEvaluationContextWithUserId() {
        Map<String, Value> apiAttrs = new LinkedHashMap();
        apiAttrs.put("userId", new Value("test-userId-123"));

        // ensure fallback to userId when target key and user_id are null
        EvaluationContext ctx = new MutableContext(null, apiAttrs);
        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "test-userId-123");

        // ensure fallback to userId when target key and user_id are empty
        ctx = new MutableContext("", apiAttrs);
        user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "test-userId-123");
    }

    @Test
    public void testFromEvaluationContextUserIdPriorityOrder() {
        Map<String, Value> apiAttrs = new LinkedHashMap();
        apiAttrs.put("user_id", new Value("user_id_value"));
        apiAttrs.put("userId", new Value("userId_value"));

        // Test priority: targetingKey > user_id > userId
        // When all three are present, targetingKey should win
        EvaluationContext ctx = new MutableContext("targetingKey_value", apiAttrs);
        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "targetingKey_value");

        // When targetingKey is null, user_id should win over userId
        ctx = new MutableContext(null, apiAttrs);
        user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "user_id_value");

        // When targetingKey is empty, user_id should win over userId
        ctx = new MutableContext("", apiAttrs);
        user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "user_id_value");

        // When only userId is present, it should be used
        Map<String, Value> userIdOnlyAttrs = new LinkedHashMap();
        userIdOnlyAttrs.put("userId", new Value("userId_only_value"));
        ctx = new MutableContext(null, userIdOnlyAttrs);
        user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "userId_only_value");
    }

    @Test
    public void testFromEvaluationContextUserIdExcludedFromCustomData() {
        Map<String, Value> apiAttrs = new LinkedHashMap();
        apiAttrs.put("userId", new Value("test-userId-123"));
        apiAttrs.put("customField", new Value("customValue"));

        // When userId is used as the user ID, it should be excluded from custom data
        EvaluationContext ctx = new MutableContext(null, apiAttrs);
        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        
        Assert.assertEquals(user.getUserId(), "test-userId-123");
        Assert.assertNotNull(user.getCustomData());
        Assert.assertEquals(user.getCustomData().size(), 1);
        Assert.assertEquals(user.getCustomData().get("customField"), "customValue");
        Assert.assertFalse(user.getCustomData().containsKey("userId"));
    }

    @Test
    public void testFromEvaluationContextAllUserIdFieldsExcludedFromCustomData() {
        Map<String, Value> apiAttrs = new LinkedHashMap();
        apiAttrs.put("user_id", new Value("user_id_value"));
        apiAttrs.put("userId", new Value("userId_value"));
        apiAttrs.put("customField", new Value("customValue"));

        // All user ID fields should be excluded from custom data regardless of which is used
        EvaluationContext ctx = new MutableContext(null, apiAttrs);
        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        
        Assert.assertEquals(user.getUserId(), "user_id_value");
        Assert.assertNotNull(user.getCustomData());
        Assert.assertEquals(user.getCustomData().size(), 1);
        Assert.assertEquals(user.getCustomData().get("customField"), "customValue");
        Assert.assertFalse(user.getCustomData().containsKey("userId"));
        Assert.assertFalse(user.getCustomData().containsKey("user_id"));
        Assert.assertFalse(user.getCustomData().containsKey("targetingKey"));
    }

    @Test
    public void testFromEvaluationContextInvalidUserIdTypes() {
        Map<String, Value> apiAttrs = new LinkedHashMap();
        
        // Test with non-string userId value - should be ignored
        apiAttrs.put("userId", new Value(123));
        EvaluationContext ctx = new MutableContext(null, apiAttrs);
        
        try {
            DevCycleUser.fromEvaluationContext(ctx);
            Assert.fail("Expected TargetingKeyMissingError");
        } catch (TargetingKeyMissingError e) {
            // expected
        }

        // Test with non-string user_id value but valid userId string - should use userId
        apiAttrs.put("user_id", new Value(456));
        apiAttrs.put("userId", new Value("valid-userId"));
        ctx = new MutableContext(null, apiAttrs);
        
        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "valid-userId");
    }

    @Test
    public void testCreateUserWithAttributes() {
        Map<String, Value> apiAttrs = new LinkedHashMap();
        apiAttrs.put("email", new Value("test-user@domain.com"));
        apiAttrs.put("country", new Value("US"));

        apiAttrs.put("name", new Value("John Doe"));
        apiAttrs.put("language", new Value("en"));
        apiAttrs.put("appVersion", new Value("1.0.0"));
        apiAttrs.put("appBuild", new Value("1"));

        EvaluationContext ctx = new MutableContext("test-1234", apiAttrs);

        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "test-1234");
        Assert.assertEquals(user.getEmail(), "test-user@domain.com");
        Assert.assertEquals(user.getCountry(), "US");
        Assert.assertEquals(user.getName(), "John Doe");
        Assert.assertEquals(user.getLanguage(), "en");
        Assert.assertEquals(user.getAppVersion(), "1.0.0");
        Assert.assertEquals(user.getAppBuild(), "1");
    }

    @Test
    public void testCreateUserWithCustomData() {
        Map<String, Value> apiAttrs = new LinkedHashMap();

        Map<String, Object> customData = new LinkedHashMap();
        customData.put("strValue", "hello");
        customData.put("intValue", 123);
        customData.put("floatValue", 3.1456);
        customData.put("boolValue", true);
        apiAttrs.put("customData", new Value(Structure.mapToStructure(customData)));

        Map<String, Object> privateCustomData = new LinkedHashMap();
        privateCustomData.put("strValue", "world");
        privateCustomData.put("intValue", 789);
        privateCustomData.put("floatValue", 0.0001);
        privateCustomData.put("boolValue", false);

        apiAttrs.put("privateCustomData", new Value(Structure.mapToStructure(privateCustomData)));

        EvaluationContext ctx = new MutableContext("test-1234", apiAttrs);

        DevCycleUser user = DevCycleUser.fromEvaluationContext(ctx);
        Assert.assertEquals(user.getUserId(), "test-1234");

        Assert.assertEquals(user.getCustomData().size(), 4);
        Assert.assertEquals(user.getCustomData().get("strValue"), "hello");
        Assert.assertEquals(user.getCustomData().get("intValue"), 123.0);
        Assert.assertEquals(user.getCustomData().get("floatValue"), 3.1456);
        Assert.assertEquals(user.getCustomData().get("boolValue"), true);

        Assert.assertEquals(user.getPrivateCustomData().size(), 4);
        Assert.assertEquals(user.getPrivateCustomData().get("strValue"), "world");
        Assert.assertEquals(user.getPrivateCustomData().get("intValue"), 789.0);
        Assert.assertEquals(user.getPrivateCustomData().get("floatValue"), 0.0001);
        Assert.assertEquals(user.getPrivateCustomData().get("boolValue"), false);
    }

    @Test
    public void testSetCustomValueBadData() {
        Assert.assertThrows(NullPointerException.class, () -> {
            DevCycleUser.setCustomValue(null, "test", new Value(true));
        });

        Map<String, Object> customData = new HashMap();
        DevCycleUser.setCustomValue(customData, null, new Value(true));
        Assert.assertEquals(customData.size(), 0);

        DevCycleUser.setCustomValue(customData, "test", null);
        Assert.assertEquals(customData.size(), 0);

        List list = new ArrayList();
        list.add("one");
        list.add("two");
        list.add("three");

        customData = new HashMap();
        DevCycleUser.setCustomValue(customData, "test", new Value(list));
        Assert.assertEquals(customData.size(), 0);

        Map map = new HashMap();
        map.put("p1", "one");
        map.put("p2", "two");
        map.put("p3", "three");

        customData = new HashMap();
        DevCycleUser.setCustomValue(customData, "test", new Value(Structure.mapToStructure(map)));
        Assert.assertEquals(customData.size(), 0);
    }

    @Test
    public void testSetCustomValueNull() {
        Map<String, Object> customData = new HashMap();

        DevCycleUser.setCustomValue(customData, "test", new Value());
        Assert.assertEquals(customData.size(), 1);
        Assert.assertNull(customData.get("test"));
    }

    @Test
    public void testSetCustomValueBoolean() {
        Map<String, Object> customData = new HashMap();

        DevCycleUser.setCustomValue(customData, "test", new Value(true));
        Assert.assertEquals(customData.size(), 1);
        Assert.assertEquals(customData.get("test"), true);
    }

    @Test
    public void testSetCustomValueString() {
        Map<String, Object> customData = new HashMap();

        DevCycleUser.setCustomValue(customData, "test", new Value("some string"));
        Assert.assertEquals(customData.size(), 1);
        Assert.assertEquals(customData.get("test"), "some string");
    }

    @Test
    public void testSetCustomValueInt() {
        Map<String, Object> customData = new HashMap();

        DevCycleUser.setCustomValue(customData, "test", new Value(999));
        Assert.assertEquals(customData.size(), 1);
        Assert.assertEquals(customData.get("test"), 999.0);
    }

    @Test
    public void testSetCustomValueDouble() {
        Map<String, Object> customData = new HashMap();

        DevCycleUser.setCustomValue(customData, "test", new Value(3.14));
        Assert.assertEquals(customData.size(), 1);
        Assert.assertEquals(customData.get("test"), 3.14);
    }
}
