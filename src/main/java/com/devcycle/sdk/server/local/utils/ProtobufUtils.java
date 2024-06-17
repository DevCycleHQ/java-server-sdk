package com.devcycle.sdk.server.local.utils;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.protobuf.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProtobufUtils {
    public static DVCUser_PB createDVCUserPB(DevCycleUser user) {
        double appBuild = Double.NaN;
        try {
            appBuild = Double.parseDouble(user.getAppBuild());
        } catch (Exception e) { /* doesn't matter */ }

        return DVCUser_PB.newBuilder()
                .setUserId(user.getUserId())
                .setEmail(createNullableString(user.getEmail()))
                .setName(createNullableString(user.getName()))
                .setLanguage(createNullableString(user.getLanguage()))
                .setCountry(createNullableString(user.getCountry()))
                .setAppBuild(createNullableDouble(appBuild))
                .setAppVersion(createNullableString(user.getAppVersion()))
                .setCustomData(createNullableCustomData(user.getCustomData()))
                .setPrivateCustomData(createNullableCustomData(user.getPrivateCustomData()))
                .build();
    }


    /**
     * Create the appropriate Variable instance from the protobuf SDKVariable_PB object provided
     *
     * @param sdkVariable
     * @param defaultValue
     * @return A new Variable instance
     */
    public static <T> Variable<T> createVariable(SDKVariable_PB sdkVariable, T defaultValue) throws JsonProcessingException {
        Variable<T> variable;

        switch (sdkVariable.getType()) {
            case Boolean:
                variable = (Variable<T>) Variable.builder()
                        .key(sdkVariable.getKey())
                        .type(Variable.TypeEnum.BOOLEAN)
                        .value(sdkVariable.getBoolValue())
                        .defaultValue(defaultValue)
                        .isDefaulted(false)
                        .build();
                break;
            case String:
                variable = (Variable<T>) Variable.builder()
                        .key(sdkVariable.getKey())
                        .type(Variable.TypeEnum.STRING)
                        .value(sdkVariable.getStringValue())
                        .defaultValue(defaultValue)
                        .isDefaulted(false)
                        .build();
                break;
            case Number:
                variable = (Variable<T>) Variable.builder()
                        .key(sdkVariable.getKey())
                        .type(Variable.TypeEnum.NUMBER)
                        .value(sdkVariable.getDoubleValue())
                        .defaultValue(defaultValue)
                        .isDefaulted(false)
                        .build();
                break;
            case JSON:
                ObjectMapper mapper = new ObjectMapper();
                LinkedHashMap<String, Object> jsonObject = mapper.readValue(sdkVariable.getStringValue(), new TypeReference<LinkedHashMap<String, Object>>() {
                });
                variable = (Variable<T>) Variable.builder()
                        .key(sdkVariable.getKey())
                        .type(Variable.TypeEnum.JSON)
                        .value(jsonObject)
                        .defaultValue(defaultValue)
                        .isDefaulted(false)
                        .build();
                break;
            default:
                throw new IllegalArgumentException("Unknown variable type: " + sdkVariable.getType());
        }
        return variable;
    }

    public static NullableString createNullableString(String value) {
        return value == null
                ? NullableString.newBuilder().setIsNull(true).build()
                : NullableString.newBuilder().setIsNull(false).setValue(value).build();
    }

    public static NullableDouble createNullableDouble(double value) {
        return !Double.isNaN(value)
                ? NullableDouble.newBuilder().setIsNull(false).setValue(value).build()
                : NullableDouble.newBuilder().setIsNull(true).build();
    }

    public static NullableCustomData createNullableCustomData(Object temp) {
        if (temp == null) {
            return NullableCustomData.newBuilder().setIsNull(true).build();
        } else {
            Map<String, Object> customData = (Map<String, Object>) temp;

            Map<String, CustomDataValue> values = new HashMap();

            for (Map.Entry<String, Object> entry : customData.entrySet()) {
                if (entry.getValue() == null) {
                    values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Null).build());

                } else if (entry.getValue() instanceof String) {
                    String strValue = (String) entry.getValue();
                    values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Str).setStringValue(strValue).build());

                } else if (entry.getValue() instanceof Number) {
                    double numValue = ((Number) entry.getValue()).doubleValue();
                    values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Num).setDoubleValue(numValue).build());
                } else if (entry.getValue() instanceof Boolean) {
                    boolean boolValue = (Boolean) entry.getValue();
                    values.put(entry.getKey(), CustomDataValue.newBuilder().setType(CustomDataType.Bool).setBoolValue(boolValue).build());
                }
            }
            return NullableCustomData.newBuilder().putAllValue(values).setIsNull(false).build();
        }
    }

    public static VariableType_PB convertTypeEnumToVariableType(Variable.TypeEnum type) {
        switch (type) {
            case BOOLEAN:
                return VariableType_PB.Boolean;
            case STRING:
                return VariableType_PB.String;
            case NUMBER:
                return VariableType_PB.Number;
            case JSON:
                return VariableType_PB.JSON;
            default:
                throw new IllegalArgumentException("Unknown variable type: " + type);
        }
    }


}
