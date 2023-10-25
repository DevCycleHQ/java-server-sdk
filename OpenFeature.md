# DevCycle Java SDK OpenFeature Provider

This SDK provides a Java implementation of the [OpenFeature](https://openfeature.dev/) Provider interface.

## Example App

See the [example app](src/examples/java/com/devcycle/examples/OpenFeatureExample.java) for a working example of the DevCycle Java SDK OpenFeature Provider.

## Usage

Start by creating the appropriate DevCycle SDK client (`DevCycleLocalClient` or `DevCycleCloudClient`).

See our [Java Cloud Bucketing SDK](https://docs.devcycle.com/sdk/server-side-sdks/java-cloud) and [Java Local Bucketing SDK](https://docs.devcycle.com/sdk/server-side-sdks/java-local) documentation for more information on how to configure the SDK.

Once the DevCycle client is configured, pass it to a new `DevCycleProvider` instance and set it as the provider for OpenFeature.

Once the DevCycle client is configured, call the `getOpenFeatureProvider()` function to obtain the OpenFeature provider.


```java
// Initialize DevCycle Client
DevCycleLocalOptions options = DevCycleLocalOptions.builder().build();
DevCycleLocalClient devCycleClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY", options);

// Set the initialzed DevCycle client as the provider for OpenFeature
OpenFeatureAPI api = OpenFeatureAPI.getInstance();
api.setProvider(devCycleClient.getOpenFeatureProvider());
        
// Get the OpenFeature client
Client openFeatureClient = api.getClient();

// Create the evaluation context to use for fetching variable values
EvaluationContext context = new ImmutableContext("test-1234");

// Retrieve a boolean flag from the OpenFeature client
Boolean variableValue = openFeatureClient.getBooleanValue(VARIABLE_KEY, false, context);
```

### Required Targeting Key

For DevCycle SDK to work we require either a `targeting key` or `user_id` attribute to be set on the OpenFeature context.
This value is used to identify the user as the `user_id` property for a `DevCycleUser` in DevCycle.

### Mapping Context Properties to DevCycleUser

The provider will automatically translate known `DevCycleUser` properties from the OpenFeature context to the `DevCycleUser` object.
[DevCycleUser Java Interface](https://github.com/DevCycleHQ/java-server-sdk/blob/main/src/main/java/com/devcycle/sdk/server/common/model/DevCycleUser.java)

For example all these properties will be set on the `DevCycleUser`:
```java
Map<String, Value> attributes = new LinkedHashMap<>();
attributes.put("email", new Value("email@devcycle.com"));
attributes.put("name", new Value("name"));
attributes.put("country", new Value("CA"));
attributes.put("language", new Value("en"));
attributes.put("appVersion", new Value("1.0.11"));
attributes.put("appBuild", new Value(1000));

Map<String,Object> customData = new LinkedHashMap<>();
customData.put("custom", "value");
attributes.put("customData", new Value(Structure.mapToStructure(customData)));

Map<String,Object> privateCustomData = new LinkedHashMap<>();
privateCustomData.put("private", "data");
attributes.put("privateCustomData", new Value(Structure.mapToStructure(privateCustomData)));

EvaluationContext context = new ImmutableContext("test-1234", attributes);
```

Context properties that are not known `DevCycleUser` properties will be automatically
added to the `customData` property of the `DevCycleUser`.

### Context Limitations

DevCycle only supports flat JSON Object properties used in the Context. Non-flat properties will be ignored.

For example `obj` will be ignored:
```java
context = EvaluationContext(targeting_key="test-1234", attributes={
    "obj": { "key": "value" }
})
```

### JSON Flag Limitations

The OpenFeature spec for JSON flags allows for any type of valid JSON value to be set as the flag value.

For example the following are all valid default value types to use with OpenFeature:
```java
// Invalid JSON values for the DevCycle SDK, will return defaults
openFeatureClient.getObjectValue("json-flag", new Value(new ArrayList<String>(Arrays.asList("value1", "value2"))));
openFeatureClient.getObjectValue("json-flag", new Value(610));
openFeatureClient.getObjectValue("json-flag", new Value(false));
openFeatureClient.getObjectValue("json-flag", new Value("string"));
```

However, these are not valid types for the DevCycle SDK, the DevCycle SDK only supports JSON Objects:
```java

Map<String,Object> defaultJsonData = new LinkedHashMap<>();
defaultJsonData.put("default", "value");
openFeatureClient.getObjectValue("json-flag", new Value(Structure.mapToStructure(defaultJsonData)));
```