package com.devcycle.sdk.server.openfeature;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.devcycle.sdk.server.common.api.IDevCycleClient;
import com.devcycle.sdk.server.common.model.PlatformData;
import dev.openfeature.sdk.*;
import dev.openfeature.sdk.exceptions.ProviderNotReadyError;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;
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
        }
        catch(ProviderNotReadyError e) {
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
        }
        catch(TargetingKeyMissingError e) {
            // expected
        }
    }

    @Test
    public void testResolveNoKey() {
        IDevCycleClient dvcClient = mock(IDevCycleClient.class);
        when(dvcClient.isInitialized()).thenReturn(true);
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
        DevCycleProvider provider = new DevCycleProvider(dvcClient);

        ProviderEvaluation<Boolean> result = provider.resolve("some-flag", null, new ImmutableContext("user-1234"));
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getValue(), false);
        Assert.assertEquals(result.getReason(), Reason.ERROR.toString());
        Assert.assertEquals(result.getErrorCode(), ErrorCode.GENERAL);
    }

}
