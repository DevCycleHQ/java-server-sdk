package com.devcycle.sdk.server.cloud;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.devcycle.sdk.server.cloud.api.DVCCloudClient;
import com.devcycle.sdk.server.cloud.model.DVCCloudOptions;
import com.devcycle.sdk.server.common.api.DVCApiMock;
import com.devcycle.sdk.server.common.api.IDVCApi;
import com.devcycle.sdk.server.common.exception.DVCException;
import com.devcycle.sdk.server.common.model.*;
import com.devcycle.sdk.server.helpers.WhiteBox;

/**
 * API tests for DevcycleApi
 */
@RunWith(MockitoJUnitRunner.class)
public class DVCCloudClientTest {

    @Mock
    private IDVCApi apiInterface;

    private DVCCloudClient api;

    private DVCApiMock dvcApiMock;

    private DVCCloudOptions dvcOptions;

    @Before
    public void setup() {
        final String apiKey = String.format("server-%s", UUID.randomUUID());

        api = new DVCCloudClient(apiKey);

        WhiteBox.setInternalState(api, "api", apiInterface);

        dvcApiMock = new DVCApiMock();

        dvcOptions = DVCCloudOptions.builder().build();
    }

    @Test
    public void getFeaturesTest() throws DVCException {
        User user = User.builder()
                .userId("j_test")
                .country("US")
                .build();

        when(apiInterface.getFeatures(user, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getFeatures(user, dvcOptions.getEnableEdgeDB()));

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

        when(apiInterface.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB()));

        Variable<Boolean> variable;
        try {
            variable = api.variable(user, key, true);

            assertUserDefaultsCorrect(user);

            Assert.assertFalse(variable.getValue());
        } catch (DVCException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVariableValueByKeyTest() {
        User user = User.builder()
                .userId("j_test")
                .build();

        String key = "show-quickstart";

        when(apiInterface.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getVariableByKey(user, key, dvcOptions.getEnableEdgeDB()));

        try {
            boolean value = api.variableValue(user, key, true);

            assertUserDefaultsCorrect(user);

            Assert.assertFalse(value);
        } catch (DVCException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getVariablesTest() throws DVCException {
        User user = User.builder()
                .userId("j_test")
                .build();

        when(apiInterface.getVariables(user, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.getVariables(user, dvcOptions.getEnableEdgeDB()));

        Map<String, BaseVariable> variables = api.allVariables(user);

        assertUserDefaultsCorrect(user);

        Assert.assertNotNull(variables);
    }

    @Test
    public void variable_nullUser_throwsException() {
        Assert.assertThrows("User cannot be null",
                IllegalArgumentException.class,
                () -> api.variable(null, "wibble", true));
    }

    @Test
    public void variable_nullUserId_throwsException() {
        Assert.assertThrows("userId is marked non-null but is null",
                NullPointerException.class, () -> User.builder().build());
    }

    @Test
    public void variable_emptyUserId_throwsException() {
        User user = User.builder().userId("").build();

        Assert.assertThrows("userId cannot be empty",
                IllegalArgumentException.class, () -> api.variable(user, "wibble", true));
    }

    @Test
    public void variable_emptyKey_throwsException() {
        User user = User.builder()
                .userId("j_test")
                .build();

        Assert.assertThrows("Missing parameter: key",
                DVCException.class, () -> api.variable(user, null, true));
    }

    @Test
    public void variable_emptyDefaultValue_throwsException() {
        User user = User.builder()
                .userId("j_test")
                .build();

        Assert.assertThrows("Missing parameter: defaultValue",
        DVCException.class, () -> api.variable(user, "wibble", null));
    }

    @Test
    public void postEventsTest() throws DVCException {
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

        when(apiInterface.track(userAndEvents, dvcOptions.getEnableEdgeDB())).thenReturn(dvcApiMock.track(userAndEvents, dvcOptions.getEnableEdgeDB()));

        api.track(user, event);

        assertUserDefaultsCorrect(user);
    }

    private void assertUserDefaultsCorrect(User user) {
        Assert.assertEquals("Java", user.getPlatform());
        Assert.assertEquals(PlatformData.SdkTypeEnum.SERVER, user.getSdkType());
        Assert.assertNotNull(user.getPlatformVersion());
        Assert.assertEquals("1.5.1", user.getSdkVersion());
    }
}