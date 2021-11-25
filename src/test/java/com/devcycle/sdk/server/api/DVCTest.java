package com.devcycle.sdk.server.api;

import com.devcycle.sdk.server.exception.DVCException;
import com.devcycle.sdk.server.helpers.WhiteBox;
import com.devcycle.sdk.server.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;

/**
 * API tests for DevcycleApi
 */
@RunWith(MockitoJUnitRunner.class)
public class DVCTest {

    @Mock
    private DVCApi apiInterface;

    private DVC api;

    private DVCApiMock dvcApiMock;

    @Before
    public void setup() {
        final String apiKey = String.format("server-%s", UUID.randomUUID());

        api = new DVC(apiKey);

        WhiteBox.setInternalState(api, "api", apiInterface);

        dvcApiMock = new DVCApiMock();
    }

    @Test
    public void getFeaturesTest() throws IOException, DVCException {
        User user = User.builder()
                .userId("j_test")
                .country("US")
                .build();

        when(apiInterface.getFeatures(user)).thenReturn(dvcApiMock.getFeatures(user));

        Map<String, Feature> features = api.allFeatures(user);

        assertUserDefaultsCorrect(user);

        Assert.assertNotNull(features);
        Assert.assertEquals(5, features.size());
        Assert.assertNotNull(features.get("show-feature-history"));
    }

    @Test
    public void getVariableByKeyTest() {
        User user = User.builder()
                .userId("j_test")
                .build();

        String key = "show-quickstart";

        when(apiInterface.getVariableByKey(user, key)).thenReturn(dvcApiMock.getVariableByKey(user, key));

        Variable variable = api.variable(user, key, true);

        assertUserDefaultsCorrect(user);

        Assert.assertFalse((Boolean) variable.getValue());
    }

    @Test
    public void getVariablesTest() throws DVCException, IOException {
        User user = User.builder()
                .userId("j_test")
                .build();

        when(apiInterface.getVariables(user)).thenReturn(dvcApiMock.getVariables(user));

        Map<String, Variable> variables = api.allVariables(user);

        assertUserDefaultsCorrect(user);

        Assert.assertNotNull(variables);
    }

    @Test
    public void postEventsTest() throws DVCException, IOException {
        User user = User.builder()
                .userId("j_test")
                .build();

        Event event = Event.builder()
                .date(Instant.now().toEpochMilli())
                .target("test target")
                .type("test event")
                .value(new BigDecimal(600))
                .metaData(Meta.builder()
                        .meta("data")
                        .build())
                .build();

        UserAndEvents userAndEvents = UserAndEvents.builder()
                .user(user)
                .events(Collections.singletonList(event))
                .build();

        when(apiInterface.track(userAndEvents)).thenReturn(dvcApiMock.track(userAndEvents));

        DVCResponse response = api.track(user, event);

        assertUserDefaultsCorrect(user);

        Assert.assertEquals("Successfully received 1 events", response.getMessage());
    }

    private void assertUserDefaultsCorrect(User user) {
        Assert.assertEquals("Java", user.getPlatform());
        Assert.assertEquals(User.SdkTypeEnum.SERVER, user.getSdkType());
        Assert.assertEquals("1.0.0", user.getSdkVersion());
    }
}