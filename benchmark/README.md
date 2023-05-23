# Java Server SDK Benchmark Tool

This tool is used to benchmark the DevCycle Java Server SDK and is built using the [Java Microbenchmark Harness](https://github.com/openjdk/jmh) (JMH).

## Requirements

This tool works with Java 8 and above but will require a x86_64 JDK. 

Currently Supported Platforms are:

| OS | Arch |
| --- | --- |
| Linux (ELF) | x86_64 |
| Mac OS | x86_64 |
| Windows | x86_64 |

In addition the benchmark tool requires [Maven](https://maven.apache.org/) to be installed.

## How Does it Work?

The benchmark itself is defined in `SDKBenchmark.java`. It is configured to test a single boolean variable evaluation repeatedly based on the config defined in `resources/fixture_large_config.json`

A local web server (see `MockServer.java`) is created to replicate DevCycle config and event services and support the SDK client.

You can run the benchmark tool by executing the following commands:

```bash
mvn clean install

java -jar target/java-server-sdk-benchmarks.jar
````

JMH builds an executable jar designed to run the benchmark as optimally as possible. 

## Targeting a Specific SDK Version

The Java Server SDK is defined in the `pom.xml` file as a dependency. To target a specific version of the SDK, update the `pom.xml` file to reference the desired version.

```xml
<dependency>
    <groupId>com.devcycle</groupId>
    <artifactId>java-server-sdk</artifactId>
    <version>1.4.0</version>
    <scope>compile</scope>
</dependency>
```

If you want to target an ad hoc version, generate a JAR file for the **java-server-sdk** in the parent project by running the following command:

```bash
 gradle jar
```

Then update the `benchmark/pom.xml` to point to that JAR file but adding a `systemPath` element to the dependency and changing the scope to `system`:

```xml
<dependency>
    <groupId>com.devcycle</groupId>
    <artifactId>java-server-sdk</artifactId>
    <version>1.4.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../build/libs/java-server-sdk-1.4.0.jar</systemPath>
</dependency>
```

## Profiling the SDK

`VariableTest.java` is a simple test that can be used to profile the SDK. It utilizes the same structure and data as the benchmark, but without the JMH complexity. 

Just execute the 

## Contributing

To work on the benchmark tool, it is recommended that you open up the `benchmark` directory as its own project folder in your IDE. This will allow you to run the tool directly from your IDE. 