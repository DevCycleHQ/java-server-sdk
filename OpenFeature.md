# DevCycle Java SDK OpenFeature Provider

This SDK provides a Java implementation of the [OpenFeature](https://openfeature.dev/) Provider interface.

## Example App

See the [example app](src/examples/java/com/devcycle/examples/OpenFeatureExample.java) for a working example of the DevCycle Java SDK OpenFeature Provider.

## Usage

Start by creating the appropriate DevCycle SDK client (`DevCycleLocalClient` or `DevCycleCloudClient`).

See our [Java Cloud Bucketing SDK](https://docs.devcycle.com/sdk/server-side-sdks/java-cloud) and [Java Local Bucketing SDK](https://docs.devcycle.com/sdk/server-side-sdks/java-local) documentation for more information on how to configure the SDK.

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
EvaluationContext context = new MutableContext("user-1234");

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
MutableContext context = new MutableContext("test-1234");
context.add("email", "email@devcycle.com");
context.add("name", "name");
context.add("country", "CA");
context.add("language", "en");
context.add("appVersion", "1.0.11");
context.add("appBuild", 1000);

Map<String,Object> customData = new LinkedHashMap<>();
customData.put("custom", "value");
context.add("customData", Structure.mapToStructure(customData));

Map<String,Object> privateCustomData = new LinkedHashMap<>();
privateCustomData.put("private", "data");
context.add("privateCustomData", Structure.mapToStructure(privateCustomData));
```

Context properties that are not known `DevCycleUser` properties will be automatically
added to the `customData` property of the `DevCycleUser`.

DevCycle allows the following data types for custom data values: **boolean**, **integer**, **double**, **float**, and **String**. Other data types will be ignored

### Provider Events

The provider emits [OpenFeature provider events](https://openfeature.dev/specification/sections/events), so
applications can react to configuration changes and to the provider losing its connection to DevCycle.

```java
Client openFeatureClient = api.getClient();

openFeatureClient.onProviderConfigurationChanged(details ->
        System.out.println("DevCycle config updated, ETag " + details.getEventMetadata().getString("configETag")));
openFeatureClient.onProviderStale(details ->
        System.out.println("DevCycle config could not be refreshed: " + details.getMessage()));
```

| Event | When it is emitted |
| --- | --- |
| `PROVIDER_READY` | The DevCycle client has loaded a configuration. Also emitted when config fetching recovers after a failure. |
| `PROVIDER_CONFIGURATION_CHANGED` | A newly fetched configuration differs from the one previously in use, either from polling or from a realtime update. |
| `PROVIDER_STALE` | A configuration fetch failed while a previously fetched configuration is still being served. Evaluations continue against that cached configuration. |
| `PROVIDER_ERROR` | A configuration fetch failed and no configuration has ever been loaded. Reported with error code `PROVIDER_FATAL` when the SDK key is unauthorized, which means the provider will not recover. |

`PROVIDER_CONFIGURATION_CHANGED` does not include a `flagsChanged` list. DevCycle resolves variables per-user at
evaluation time, so the set of variable keys whose value actually changed for a given user is not known when the
configuration is fetched.

Only the Local Bucketing client (`DevCycleLocalClient`) holds a configuration, so configuration change, stale, and
error events apply to it. The Cloud Bucketing client (`DevCycleCloudClient`) evaluates against the DevCycle API on
every request and becomes ready immediately.

### JSON Flag Limitations

The OpenFeature spec for JSON flags allows for any type of valid JSON value to be set as the flag value.

For example the following are all valid default value types to use with OpenFeature:
```java
// Invalid JSON values for the DevCycle SDK, will return defaults
openFeatureClient.getObjectValue("json-flag", new Value(new ArrayList<String>(Arrays.asList("value1", "value2"))));
openFeatureClient.getObjectValue("json-flag", new Value(610));
openFeatureClient.getObjectValue("json-flag", new Value(false));
openFeatureClient.getObjectValue("json-flag", new Value("string"));
openFeatureClient.getObjectValue("json-flag", new Value());
```

However, these are not valid types for the DevCycle SDK, the DevCycle SDK only supports JSON Objects:
```java

Map<String,Object> defaultJsonData = new LinkedHashMap<>();
defaultJsonData.put("default", "value");
openFeatureClient.getObjectValue("json-flag", new Value(Structure.mapToStructure(defaultJsonData)));
```