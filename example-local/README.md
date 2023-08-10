# example-local-java-server-sdk-app

## Guide

This is a simple Spring Boot web app that can be run over several steps

Run the example without creating a feature / variable will result in a default value 
being used

Rerun the example after creating a feature and a variable and that will be shown 

replace `application.properties` `devcycle.sdkKey` with your server SDK key

## Run Example

To start the application run the following command:

```bash
./gradlew buildDependents
./gradlew bootRun
```

Then access the website at: http://localhost:8080

## Run against local Java SDK

See instructions in `build.gradle` for how to run against a local version of the Java SDK.