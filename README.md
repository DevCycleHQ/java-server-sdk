# DevCycle Java Server SDK

Welcome to the DevCycle Java SDK, which interfaces with the [DevCycle Bucketing API](https://docs.devcycle.com/bucketing-api/#tag/devcycle). 

## Requirements

This version of the DevCycle SDK works with Java 8 and above.

Using the Java SDK library requires [Maven](https://maven.apache.org/) or [Gradle](https://gradle.org/) >= 5.6.4 to be installed.

An x86_64 JDK is required for Local Bucketing with the DevCycle Java SDK. Currently Supported Platforms are:


| OS | Arch |
| --- | --- |
| Linux (ELF) | x86_64 |
| Mac OS | x86_64 |
| Windows | x86_64 |

## Installation

### Maven

You can use the SDK in your Maven project by adding the following to your *pom.xml*:

```xml
<dependency>
    <groupId>com.devcycle</groupId>
    <artifactId>java-server-sdk</artifactId>
    <version>1.5.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle
Alternatively you can use the SDK in your Gradle project by adding the following to *build.gradle*:

```yaml
implementation("com.devcycle:java-server-sdk:1.5.0")
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

