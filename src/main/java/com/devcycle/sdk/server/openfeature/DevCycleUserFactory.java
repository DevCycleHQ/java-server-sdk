package com.devcycle.sdk.server.openfeature;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for creating a DevCycleUser from an EvaluationContext
 */
class DevCycleUserFactory {
    private static final String USER_ID = "user_id";
    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String LANGUAGE = "language";
    private static final String COUNTRY = "country";
    private static final String APP_VERSION = "appVersion";
    private static final String APP_BUILD = "appBuild";
    private static final String CUSTOM_DATA = "customData";
    private static final String PRIVATE_CUSTOM_DATA = "privateCustomData";

    static void setCustomValue(Map<String, Object> customData, String key, Value value) {
        // Only support boolean, number, and string types for custom data values
        // ignore all other data
        if (customData != null && key != null && value != null) {
            if (value.isBoolean()) {
                customData.put(key, value.asBoolean());
            } else if (value.isNumber()) {
                customData.put(key, value.asDouble());
            } else if (value.isString()) {
                customData.put(key, value.asString());
            }
        }
    }

    /**
     * Create a DevCycleUser from an EvaluationContext
     *
     * @param ctx A context to load a targeting key and user data from
     * @return An initialized DevCycleUser with data from the context
     * @throws TargetingKeyMissingError if the targeting key or user_id attribute is not set
     */
    static DevCycleUser createUser(EvaluationContext ctx) {
        String userId = "";

        if (ctx != null && ctx.getTargetingKey() != null) {
            userId = ctx.getTargetingKey();
        } else if (ctx != null && ctx.getValue(USER_ID) != null) {
            userId = ctx.getValue(USER_ID).asString();
        }

        if (userId == null || userId.isEmpty()) {
            throw new TargetingKeyMissingError();
        }

        DevCycleUser user = DevCycleUser.builder().userId(userId).build();

        Map<String, Object> customData = new LinkedHashMap<>();
        Map<String, Object> privateCustomData = new LinkedHashMap<>();

        for (String key : ctx.keySet()) {
            if (key.equals(USER_ID)) {
                continue;
            }

            Value value = ctx.getValue(key);

            if (key.equals(EMAIL) && value.isString()) {
                user.setEmail(value.asString());
            } else if (key.equals(NAME) && value.isString()) {
                user.setName(value.asString());
            } else if (key.equals(LANGUAGE) && value.isString()) {
                user.setLanguage(value.asString());
            } else if (key.equals(COUNTRY) && value.isString()) {
                user.setCountry(value.asString());
            } else if (key.equals(APP_VERSION) && value.isString()) {
                user.setAppVersion(value.asString());
            } else if (key.equals(APP_BUILD) && value.isString()) {
                user.setAppBuild(value.asString());
            } else if (key.equals(CUSTOM_DATA) && value.isStructure()) {
                Structure customDataStructure = value.asStructure();
                for (String dataKey : customDataStructure.keySet()) {
                    setCustomValue(customData, dataKey, customDataStructure.getValue(dataKey));
                }
            } else if (key.equals(PRIVATE_CUSTOM_DATA) && value.isStructure()) {
                Structure privateDataStructure = value.asStructure();
                for (String dataKey : privateDataStructure.keySet()) {
                    setCustomValue(privateCustomData, dataKey, privateDataStructure.getValue(dataKey));
                }
            } else {
                setCustomValue(customData, key, value);
            }
        }

        if (!customData.isEmpty()) {
            user.setCustomData(customData);
        }

        if (!privateCustomData.isEmpty()) {
            user.setPrivateCustomData(privateCustomData);
        }

        return user;
    }
}
