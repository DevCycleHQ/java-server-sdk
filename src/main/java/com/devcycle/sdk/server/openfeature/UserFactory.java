package com.devcycle.sdk.server.openfeature;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.Structure;
import dev.openfeature.sdk.Value;
import dev.openfeature.sdk.exceptions.TargetingKeyMissingError;

import java.util.LinkedHashMap;
import java.util.Map;

public class UserFactory {
    static void setCustomValue(Map<String, Object> customData, String key, Value value) {
        // Only support boolean, number, and string types for custom data values
        // ignore all other data
        if(customData != null && key != null && value != null)
        {
            if(value.isBoolean()){
                customData.put(key, value.asBoolean());
            }
            else if(value.isNumber())
            {
                customData.put(key, value.asDouble());
            }
            else if(value.isString())
            {
                customData.put(key, value.asString());
            }
        }
    }

    public static DevCycleUser createUser(EvaluationContext ctx) {
        String userId = "";

        if (ctx != null && ctx.getTargetingKey() != null) {
            userId = ctx.getTargetingKey();
        }
        else if(ctx != null && ctx.getValue("user_id") != null) {
            userId = ctx.getValue("user_id").asString();
        }

        if(userId == null || userId.isEmpty()) {
            throw new TargetingKeyMissingError();
        }

        DevCycleUser user = DevCycleUser.builder().userId(userId).build();

        Map<String,Object> customData = new LinkedHashMap();
        Map<String,Object> privateCustomData = new LinkedHashMap();

        for(String key : ctx.keySet()) {
            if(key.equals("user_id")) {
                continue;
            }

            Value value = ctx.getValue(key);

            if(key == "email" && value.isString())
            {
                user.setEmail(value.asString());
            }
            else if(key == "name" && value.isString())
            {
                user.setName(value.asString());
            }
            else if(key == "language" && value.isString())
            {
                user.setLanguage(value.asString());
            }
            else if(key == "country" && value.isString())
            {
                user.setCountry(value.asString());
            }
            else if(key == "appVersion" && value.isString())
            {
                user.setAppVersion(value.asString());
            }
            else if(key == "appBuild" && value.isString())
            {
                user.setAppBuild(value.asString());
            }
            else if(key == "customData" && value.isStructure())
            {
                Structure customDataStructure = value.asStructure();
                for(String dataKey : customDataStructure.keySet()) {
                    setCustomValue(customData, dataKey, customDataStructure.getValue(dataKey));
                }
            }
            else if(key == "privateCustomData" && value.isStructure())
            {
                Structure privateDataStructure = value.asStructure();
                for(String dataKey : privateDataStructure.keySet()) {
                    setCustomValue(privateCustomData, dataKey, privateDataStructure.getValue(dataKey));
                }
            }
            else {
                setCustomValue(customData, key, value);
            }
        }

        if(customData.size() > 0){
            user.setCustomData(customData);
        }

        if(privateCustomData.size() > 0){
            user.setPrivateCustomData(privateCustomData);
        }

        return user;
    }
}
