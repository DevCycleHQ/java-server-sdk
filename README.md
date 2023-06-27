# DevCycle Java Server SDK

Welcome to the DevCycle Java SDK, which interfaces with the [DevCycle Bucketing API](https://docs.devcycle.com/bucketing-api/#tag/devcycle). 

## Requirements

This version of the DevCycle SDK works with Java 8 and above.

Using the Java SDK library requires [Maven](https://maven.apache.org/) or [Gradle](https://gradle.org/) >= 7.6+ to be installed.

An x86_64 JDK is required for Local Bucketing with the DevCycle Java SDK. 

Currently Supported Platforms are:

| OS | Arch |
| --- | --- |
| Linux (ELF) | x86_64 |
| Mac OS | x86_64 |
| Windows | x86_64 |

In addition, the environment must support GLIBC v2.32 or higher.  You can use the following command to check your GLIBC version:

```bash
ldd --version
``` 

## Installation

### Maven

You can use the SDK in your Maven project by adding the following to your *pom.xml*:

```xml
<dependency>
    <groupId>com.devcycle</groupId>
    <artifactId>java-server-sdk</artifactId>
    <version>1.6.1</version>
    <scope>compile</scope>
</dependency>
```

### Gradle
Alternatively you can use the SDK in your Gradle project by adding the following to *build.gradle*:

```yaml
implementation("com.devcycle:java-server-sdk:1.6.1")
```

## DNS Caching
The JVM, by default, caches DNS for infinity. DevCycle servers are load balanced and dynamic. To address this concern,
setting the DNS cache TTL to a short duration is recommended. The TTL is controlled by this security setting `networkaddress.cache.ttl`.
Recommended settings and how to configure them can be found [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-jvm-ttl.html).

## Getting Started

To use the DevCycle Java SDK, initialize a client object. 

Cloud:
```java
import com.devcycle.sdk.server.cloud.api.DVCCloudClient;

public class MyClass {

    private DVCCloudClient dvcCloudClient;

    public MyClass() {
        dvcCloudClient = new DVCCloudClient("YOUR_DVC_SERVER_SDK_KEY");
    }
}
```

Local:
```java
import com.devcycle.sdk.server.local.api.DVCLocalClient;

public class MyClass {
    
    private DVCLocalClient dvcLocalClient;
    
    public MyClass() {
        dvcLocalClient = new DVCLocalClient("YOUR_DVC_SERVER_SDK_KEY");
    }
}
```

## Usage

To find usage documentation, visit our docs for [Local Bucketing](https://docs.devcycle.com/docs/sdk/server-side-sdks/java-local).

## Logging

The DevCycle SDK logs to **stdout** by default and does not require any specific logging package. To integrate with your 
own logging system, such as Java Logging or SLF4J, you can create a wrapper that implements the `IDVCLogger` interface. 
Then you can set the logger into the Java Server SDK setting the Custom Logger property in the options object used to 
initialize the client.

```java

```java
// Create your logging wrapper
IDVCLogger loggingWrapper = new IDVCLogger() {
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

// Set the logger in the options before creating the DVCLocalClient
DVCLocalOptions options = DVCLocalOptions.builder().customLogger(loggingWrapper).build();
DVCLocalClient dvcClient = new DVCLocalClient("YOUR_DVC_SERVER_SDK_KEY", options);

// Or for DVCCloudClient
DVCCloudOptions options = DVCCloudOptions.builder().customLogger(loggingWrapper).build();
DVCCloudClient dvcClient = new DVCCloudClient("YOUR_DVC_SERVER_SDK_KEY", options);
```

You can also disable all logging by setting the custom logger to `new SimpleDVCLogger(SimpleDVCLogger.Level.OFF)`.