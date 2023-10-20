# DevCycleLocalClient

Method | HTTP request | Description
------------- | ------------- | -------------
[**allFeatures**](DevCycleLocalClient.md#allFeatures) | **POST** v1/features | Get all features for user data
[**variable**](DevCycleLocalClient.md#variable) | **POST** v1/variables/{key} | Get variable by key for user data
[**allVariables**](DevCycleLocalClient.md#allVariables) | **POST** v1/variables | Get all variables for user data
[**track**](DevCycleLocalClient.md#track) | **POST** v1/track | Post events to DevCycle for user

<a name="allFeatures"></a>
# **allFeatures**
> Map&lt;String, Feature&gt; allFeatures(user)

Get all features for user data

### Example
```java
import com.devcycle.sdk.server.api.DevCycleLocalClient;

public class MyClass {
    
    private DevCycleLocalClient dvcClient;
    
    public MyClass() {
        dvcClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY");
    }
    
    public void allFeatures() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("a_user_id")
                .country("US")
                .build();

        Map<String, Feature> features = dvcClient.allFeatures(user);
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | [**DevCycleUser**](DevCycleUser.md)|  |

### Return type

[**Map&lt;String, Feature&gt;**](Feature.md)

<a name="variable"></a>
# **variable**
> Variable variable(user, key, defaultValue)

Get variable by key for user data

### Example
```java
import com.devcycle.sdk.server.api.DevCycleLocalClient;

public class MyClass {

    private DevCycleLocalClient dvcClient;

    public MyClass() {
        dvcClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY");
    }

    public void setFlag() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("a_user_id")
                .country("US")
                .build();

        String key = "turn_on_super_cool_feature";
        Boolean defaultValue = true;
        Variable<Boolean> variable = dvcClient.variable(user, key, defaultValue);

        if (variable.getValue()) {
            // New Feature code here
        } else {
            // Old code here
        }
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | [**DevCycleUser**](DevCycleUser.md)|  |
 **key** | **String**| Variable key |

### Return type

[**Variable**](Variable.md)

<a name="allVariables"></a>
# **allVariables**
> Map&lt;String, Variable&gt; allVariables(user)

Get all variables for user data

### Example
```java
import com.devcycle.sdk.server.api.DevCycleLocalClient;

public class MyClass {

    private DevCycleLocalClient dvcClient;

    public MyClass() {
        dvcClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY");
    }

    public void allVariables() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("a_user_id")
                .country("US")
                .build();
        
        Map<String, Variable> variables = dvcClient.allVariables(user);
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
**user** | [**DevCycleUser**](DevCycleUser.md)|  |

### Return type

[**Map&lt;String, Variable&gt;**](Variable.md)

<a name="track"></a>
# **track**
> DvcResponse track(user, event)

Post events to DevCycle for user

### Example
```java
import com.devcycle.sdk.server.api.DevCycleLocalClient;

public class MyClass {

    private DevCycleLocalClient dvcClient;

    public MyClass() {
        dvcClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY");
    }

    public void addAnEvent() {
        DevCycleUser user = DevCycleUser.builder()
                .userId("a_user_id")
                .country("US")
                .build();

        DevCycleEvent event = DevCycleEvent.builder()
                .date(Instant.now().toEpochMilli())
                .target("test target")
                .type("test event")
                .value(new BigDecimal(600))
                .build();

        DevCycleResponse response = dvcClient.track(user, event);
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
**user** | [**DevCycleUser**](DevCycleUser.md)|  |
**event** | [**DevCycleEvent**](DevCycleEvent.md)|

### Return type

[**DVCResponse200**](DevCycleResponse.md)