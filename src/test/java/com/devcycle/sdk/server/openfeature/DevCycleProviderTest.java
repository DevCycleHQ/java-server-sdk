package com.devcycle.sdk.server.openfeature;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.model.Variable;
import dev.openfeature.sdk.ErrorCode;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Reason;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DevCycleProviderTest {

    @Test
    public void testResolveClientNotInitialized() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(false);

        DevCycleProvider provider = new DevCycleProvider(dvcClient);
        try {
            provider.resolve("some-flag", false, new ImmutableContext("test-1234"));
            Assert.fail("Expected ProviderNotReadyError");
        } catch (ProviderNotReadyError e) {
            // expected
        }
    }

    @Test
    public void testResolveNoUserInContext() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        try {
            provider.resolve("some-flag", false, new ImmutableContext());
            Assert.fail("Expected TargetingKeyMissingError");
        } catch (TargetingKeyMissingError e) {
            // expected
        }
    }

    @Test
    public void testResolveNoKey() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
        when(dvcClient.variable(any(), any(), any())).thenThrow(IllegalArgumentException.class);

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolve(null, false, new ImmutableContext("user-1234"));
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

        ProviderEvaluation<Boolean> result = provider.resolve("some-flag", null, new ImmutableContext("user-1234"));
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

        ProviderEvaluation<Boolean> result = provider.resolve("some-flag", false, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), false);
        Assert.assertEquals(result.getReason(), Reason.DEFAULT.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testResolveVariableDefaulted() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value("unused value 1").defaultValue("default value").isDefaulted(true).type(Variable.TypeEnum.STRING).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<String> result = provider.resolve("some-flag", "default value", new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), "default value");
        Assert.assertEquals(result.getReason(), Reason.DEFAULT.toString());
        Assert.assertNull(result.getErrorCode());
    }

    @Test
    public void testResolveVariableFound() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);

        when(dvcClient.variable(any(), any(), any())).thenReturn(Variable.builder().key("some-flag").value(true).defaultValue(false).type(Variable.TypeEnum.BOOLEAN).build());

        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolve("some-flag", false, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), true);
        Assert.assertEquals(result.getReason(), Reason.TARGETING_MATCH.toString());
        Assert.assertNull(result.getErrorCode());
    }
}
