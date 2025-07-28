package com.devcycle.sdk.server.openfeature;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.model.EvalReason;
import com.devcycle.sdk.server.common.model.Variable;

import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
import dev.openfeature.sdk.exceptions.TypeMismatchError;

@RunWith(MockitoJUnitRunner.class)
public class DevCycleProviderTest {

    @Test
    public void testResolveClientNotInitialized() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(false);

        DevCycleProvider provider = new DevCycleProvider(dvcClient);
        Assert.assertThrows(
                ProviderNotReadyError.class,
                () -> provider.resolvePrimitiveVariable("some-flag", false, new ImmutableContext("test-1234")));
    }

    @Test
    public void testResolveNoUserInContext() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        Assert.assertThrows(
                TargetingKeyMissingError.class,
                () -> provider.resolvePrimitiveVariable("some-flag", false, new ImmutableContext()));
    }

    @Test
    public void testResolveNoKey() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        when(dvcClient.variable(any(), any(), any())).thenThrow(IllegalArgumentException.class);

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolvePrimitiveVariable(null, false, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), false);
        Assert.assertEquals(result.getReason(), Reason.ERROR.toString());
        Assert.assertEquals(result.getErrorCode(), ErrorCode.GENERAL);
    }

    @Test
    public void testResolveNoDefaultValue() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        when(dvcClient.variable(any(), any(), any())).thenThrow(IllegalArgumentException.class);
        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolvePrimitiveVariable("some-flag", null, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertNull(result.getValue());
        Assert.assertEquals(result.getReason(), Reason.ERROR.toString());
        Assert.assertEquals(result.getErrorCode(), ErrorCode.GENERAL);
    }


    @Test
    public void testResolveNoVariableFound() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        when(dvcClient.variable(any(), any(), any())).thenReturn(null);

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolvePrimitiveVariable("some-flag", false, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), false);
        Assert.assertEquals(result.getReason(), Reason.DEFAULT.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testResolveVariableDefaulted() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value("unused value 1").defaultValue("default value").isDefaulted(true).type(Variable.TypeEnum.STRING).eval(EvalReason.defaultReason(EvalReason.DefaultReasonDetailsEnum.USER_NOT_TARGETED)).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<String> result = provider.resolvePrimitiveVariable("some-flag", "default value", new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), "default value");
        Assert.assertEquals(result.getReason(), Reason.DEFAULT.toString());
        Assert.assertNull(result.getErrorCode());
        Assert.assertNotNull(result.getFlagMetadata());
        Assert.assertEquals(result.getFlagMetadata().getString("evalReasonDetails"), EvalReason.DefaultReasonDetailsEnum.USER_NOT_TARGETED.getValue());
    }

    @Test
    public void testResolveBooleanVariable() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(true).defaultValue(false).type(Variable.TypeEnum.BOOLEAN).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolvePrimitiveVariable("some-flag", false, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), true);
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testResolveBooleanVariableWithDevCycleEvalReason() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(true).defaultValue(false).type(Variable.TypeEnum.BOOLEAN).eval(new EvalReason("SPLIT", "User ID", "bool_target_id")).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolvePrimitiveVariable("some-flag", false, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), true);
        Assert.assertEquals(result.getReason(), "SPLIT");
        Assert.assertNull(result.getErrorCode());
        Assert.assertNotNull(result.getFlagMetadata());
        Assert.assertEquals(result.getFlagMetadata().getString("evalReasonDetails"), "User ID");
        Assert.assertEquals(result.getFlagMetadata().getString("evalReasonTargetId"), "bool_target_id");
    }

    @Test
    public void testResolveIntegerVariable() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        Integer variableValue = 1234;
        Integer defaultValue = 0;

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(variableValue).defaultValue(defaultValue).type(Variable.TypeEnum.NUMBER).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Integer> result = provider.resolvePrimitiveVariable("some-flag", defaultValue, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), variableValue);
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testResolveDoubleVariable() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        Double variableValue = 1.234;
        Double defaultValue = 0.0;

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(variableValue).defaultValue(defaultValue).type(Variable.TypeEnum.NUMBER).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Double> result = provider.resolvePrimitiveVariable("some-flag", defaultValue, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), variableValue);
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testResolveStringVariable() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        String variableValue = "some value";
        String defaultValue = "";

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(variableValue).defaultValue(defaultValue).type(Variable.TypeEnum.STRING).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<String> result = provider.resolvePrimitiveVariable("some-flag", defaultValue, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), variableValue);
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testGetObjectEvaluationBadDefault() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        String key = "json-var";

        Assert.assertThrows(
                TypeMismatchError.class,
                () -> provider.getObjectEvaluation(key, new Value(1234), new ImmutableContext("user-1234")));


        Assert.assertThrows(
                TypeMismatchError.class,
                () -> provider.getObjectEvaluation(key, new Value("basic string"), new ImmutableContext("user-1234")));


        Assert.assertThrows(
                TypeMismatchError.class,
                () -> provider.getObjectEvaluation(key, new Value(true), new ImmutableContext("user-1234")));

        List<Value> someList = new ArrayList<>();
        someList.add(new Value("some string"));
        someList.add(new Value(1234));
        Assert.assertThrows(
                TypeMismatchError.class,
                () -> provider.getObjectEvaluation(key, new Value(someList), new ImmutableContext("user-1234")));

        // Test a valid structure but with data DevCycle can't use
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedKey", "nestedValue");

        Map<String, Object> someMap = new HashMap<>();
        someMap.put("nestedMap", new Value(Structure.mapToStructure(nestedMap)));

        List<Value> nestedList = new ArrayList<>();
        nestedList.add(new Value("string"));
        nestedList.add(new Value(1234));
        someMap.put("nestedList", new Value(nestedList));

        Assert.assertThrows(
                TypeMismatchError.class,
                () -> provider.getObjectEvaluation(key, new Value(Structure.mapToStructure(someMap)), new ImmutableContext("user-1234")));
    }

    @Test
    public void testGetObjectEvaluation() {
        Map<String, Object> jsonData = new LinkedHashMap<>();
        jsonData.put("strVal", "some string");
        jsonData.put("boolVal", true);
        jsonData.put("numVal", 123);

        Map<String, Object> defaultJsonData = new LinkedHashMap<>();

        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(jsonData).defaultValue(defaultJsonData).type(Variable.TypeEnum.JSON).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        Value defaultValue = new Value(Structure.mapToStructure(defaultJsonData));

        ProviderEvaluation<Value> result = provider.getObjectEvaluation("some-flag", defaultValue, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getValue());
        Assert.assertTrue(result.getValue().isStructure());

        result.getValue().asStructure().asObjectMap().forEach((k, v) -> {
            Assert.assertTrue(jsonData.containsKey(k));
            Assert.assertEquals(jsonData.get(k), v);
        });

        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testGetObjectEvaluationWithDevCycleEvalReason() {
        Map<String, Object> jsonData = new LinkedHashMap<>();
        jsonData.put("strVal", "some string");
        jsonData.put("boolVal", true);
        jsonData.put("numVal", 123);

        Map<String, Object> defaultJsonData = new LinkedHashMap<>();

        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(jsonData).defaultValue(defaultJsonData).type(Variable.TypeEnum.JSON).eval(new EvalReason("SPLIT", "User ID", "json_target_id")).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        Value defaultValue = new Value(Structure.mapToStructure(defaultJsonData));

        ProviderEvaluation<Value> result = provider.getObjectEvaluation("some-flag", defaultValue, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getValue());
        Assert.assertTrue(result.getValue().isStructure());

        result.getValue().asStructure().asObjectMap().forEach((k, v) -> {
            Assert.assertTrue(jsonData.containsKey(k));
            Assert.assertEquals(jsonData.get(k), v);
        });

        Assert.assertEquals(result.getReason(), "SPLIT");
        Assert.assertNull(result.getErrorCode());
        Assert.assertNotNull(result.getFlagMetadata());
        Assert.assertEquals(result.getFlagMetadata().getString("evalReasonDetails"), "User ID");
        Assert.assertEquals(result.getFlagMetadata().getString("evalReasonTargetId"), "json_target_id");
    }
}
