# DVC API

Method | HTTP request | Description
------------- | ------------- | -------------
[**getFeatures**](DVC.md#getFeatures) | **POST** v1/features | Get all features for user data
[**getVariableByKey**](DVC.md#getVariableByKey) | **POST** v1/variables/{key} | Get variable by key for user data
[**getVariables**](DVC.md#getVariables) | **POST** v1/variables | Get all variables for user data
[**track**](DVC.md#track) | **POST** v1/track | Post events to DevCycle for user

<a name="getFeatures"></a>
# **getFeatures**
> Map&lt;String, Feature&gt; getFeatures(user)

Get all features for user data

### Example
```java
import com.devcycle.sdk.server.api.DVC;

public class MyClass {
    
    private DVC dvc;
    
    public MyClass() {
        dvc = new DVC("your_server_key");
    }
    
    public void getFeatures() {
        User user = User.builder()
                .userId("a_user_id")
                .country("US")
                .build();

        Map<String, Feature> features = dvc.getFeatures(user);
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | [**User**](User.md)|  |

### Return type

[**Map&lt;String, Feature&gt;**](Feature.md)

<a name="getVariableByKey"></a>
# **getVariableByKey**
> Variable getVariableByKey(user, key)

Get variable by key for user data

### Example
```java
import com.devcycle.sdk.server.api.DVC;

public class MyClass {

    private DVC dvc;

    public MyClass() {
        dvc = new DVC("your_server_key");
    }

    public void setFlag() {
        User user = User.builder()
                .userId("a_user_id")
                .country("US")
                .build();

        String key = "turn_on_super_cool_feature";
        Boolean defaultValue = true;
        Variable variable = dvc.getVariableByKey(user, key, defaultValue);

        if ((Boolean) variable.getValue()) {
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
 **user** | [**User**](User.md)|  |
 **key** | **String**| Variable key |

### Return type

[**Variable**](Variable.md)

<a name="getVariables"></a>
# **getVariables**
> Map&lt;String, Variable&gt; getVariables(body)

Get all variables for user data

### Example
```java
import com.devcycle.sdk.server.api.DVC;

public class MyClass {

    private DVC dvc;

    public MyClass() {
        dvc = new DVC("your_server_key");
    }

    public void getVariables() {
        User user = User.builder()
                .userId("a_user_id")
                .country("US")
                .build();
        
        Map<String, Variable> variables = dvc.getVariables(user);
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userAndEvents** | [**UserAndEvents**](UserAndEvents.md)|  |

### Return type

[**Map&lt;String, Variable&gt;**](Variable.md)

<a name="track"></a>
# **track**
> DvcResponse track(userAndEvents)

Post events to DevCycle for user

### Example
```java
import com.devcycle.sdk.server.api.DVC;

public class MyClass {

    private DVC dvc;

    public MyClass() {
        dvc = new DVC("your_server_key");
    }

    public void addAnEvent() {
        User user = User.builder()
                .userId("a_user_id")
                .country("US")
                .build();

        Event event = Event.builder()
                .date(Instant.now().toEpochMilli())
                .target("test target")
                .type("test event")
                .value(new BigDecimal(600))
                .build();

        UserAndEvents userAndEvents = new UserAndEvents();
        userAndEvents.setUser(user);
        userAndEvents.setEvents(Collections.singletonList(event));

        DVCResponse response = dvc.track(userAndEvents);
    }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**UserDataAndEventBody**](UserAndEvents.md)|  |

### Return type

[**DVCResponse200**](DVCResponse.md)