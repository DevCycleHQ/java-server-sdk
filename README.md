# DevCycle Java Server SDK

Welcome to the DevCycle Java Server SDK, providing feature flag functionality via [Local Bucketing](https://docs.devcycle.com/sdk/#difference-between-local-and-cloud-bucketing) or Cloud Bucket through the [DevCycle Bucketing API](https://docs.devcycle.com/bucketing-api/#tag/devcycle). 

## Requirements

This version of the DevCycle SDK works with Java 11 and above.

Using the Java SDK library requires [Maven](https://maven.apache.org/) or [Gradle](https://gradle.org/) >= 7.6+ to be installed.

An x86_64 or aarch64 JDK is required for Local Bucketing with the DevCycle Java SDK.

Currently Supported Platforms are:

| OS             | Arch      |
|----------------|-----------|
| Linux (ELF)    | x86_64    |
| Linux (ELF)    | aarch64   |
| Mac OS         | x86_64    |
| Mac OS         | aarch64   |
| Windows        | x86_64    |

In addition, the environment must support GLIBC v2.16 or higher.  You can use the following command to check your GLIBC version:

```bash
ldd --version
``` 

## Installation

### Gradle
You can use the SDK in your Gradle project by adding the following to *build.gradle*:

```yaml
implementation("com.devcycle:java-server-sdk:2.6.3")
```

### Maven

You can use the SDK in your Maven project by adding the following to your *pom.xml*:

```xml
<dependency>
    <groupId>com.devcycle</groupId>
    <artifactId>java-server-sdk</artifactId>
    <version>2.6.3</version>
    <scope>compile</scope>
</dependency>
```

## DNS Caching
The JVM, by default, caches DNS for infinity. DevCycle servers are load balanced and dynamic. To address this concern,
setting the DNS cache TTL to a short duration is recommended. The TTL is controlled by this security setting `networkaddress.cache.ttl`.
Recommended settings and how to configure them can be found [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-jvm-ttl.html).

## Getting Started

To use the DevCycle Java SDK, initialize a client object. 

Cloud:
```java
import com.devcycle.sdk.server.cloud.api.DevCycleCloudClient;

public class MyClass {

    private DevCycleCloudClient dvcCloudClient;

    public MyClass() {
        dvcCloudClient = new DevCycleCloudClient("DEVCYCLE_SERVER_SDK_KEY");
    }
}
```

Local:
```java
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;

public class MyClass {
    
    private DevCycleLocalClient dvcLocalClient;
    
    public MyClass() {
        dvcLocalClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY");
    }
}
```

## OpenFeature Support

This SDK provides an implementation of the [OpenFeature](https://openfeature.dev/) Provider interface. Use the `getOpenFeatureProvider()` method on the DevCycle SDK client to obtain a provider for OpenFeature.

```java
DevCycleLocalClient devCycleClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY", options);
OpenFeatureAPI api = OpenFeatureAPI.getInstance();
api.setProvider(devCycleClient.getOpenFeatureProvider());
```

You can find additional instructions on how to use it here: [DevCycle Java SDK OpenFeature Provider](OpenFeature.md)

## Usage

To find usage documentation, visit our docs for [Local Bucketing](https://docs.devcycle.com/sdk/server-side-sdks/java-local/java-local-usage) and [Cloud Bucketing](https://docs.devcycle.com/sdk/server-side-sdks/java-cloud/java-cloud-usage)

## Logging

The DevCycle SDK logs to **stdout** by default and does not require any specific logging package. To integrate with your 
own logging system, such as Java Logging or SLF4J, you can create a wrapper that implements the `IDevCycleLogger` interface. 
Then you can set the logger into the Java Server SDK setting the Custom Logger property in the options object used to 
initialize the client.

```java
// Create your logging wrapper
IDevCycleLogger loggingWrapper = new IDevCycleLogger() {
    @Override
    public void debug(String message) {
        // Your logging implementation here
    }

    @Override
    public void info(String message) {
        // Your logging implementation here
    }

    @Override
    public void warning(String message) {
        // Your logging implementation here
    }

    @Override
    public void error(String message) {
        // Your logging implementation here
    }

    @Override
    public void error(String message, Throwable throwable) {
        // Your logging implementation here
    }
};

// Set the logger in the options before creating the DevCycleLocalClient
DevCycleLocalOptions options = DevCycleLocalOptions.builder().customLogger(loggingWrapper).build();
DevCycleLocalClient dvcClient = new DevCycleLocalClient("DEVCYCLE_SERVER_SDK_KEY", options);

// Or for DevCycleCloudClient
DevCycleCloudOptions options = DevCycleCloudOptions.builder().customLogger(loggingWrapper).build();
DevCycleCloudClient dvcClient = new DevCycleCloudClient("DEVCYCLE_SERVER_SDK_KEY", options);
```

You can also disable all logging by setting the custom logger to `new SimpleDevCycleLogger(SimpleDevCycleLogger.Level.OFF)`.
