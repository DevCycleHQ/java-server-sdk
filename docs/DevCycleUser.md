# DevCycleUser

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**userId** | **String** | Unique id to identify the user | 
**email** | **String** | User&#x27;s email used to identify the user on the dashboard / target audiences |  [optional]
**name** | **String** | User&#x27;s name used to identify the user on the dashboard / target audiences |  [optional]
**language** | **String** | User&#x27;s language in ISO 639-1 format |  [optional]
**country** | **String** | User&#x27;s country in ISO 3166 alpha-2 format |  [optional]
**appVersion** | **String** | App Version of the running application |  [optional]
**appBuild** | **String** | App Build number of the running application |  [optional]
**customData** | **Object** | User&#x27;s custom data to target the user with, data will be logged to DevCycle for use in dashboard. |  [optional]
**privateCustomData** | **Object** | User&#x27;s custom data to target the user with, data will not be logged to DevCycle only used for feature bucketing. |  [optional]
**createdDate** | **Long** | Date the user was created, Unix epoch timestamp format |  [optional]
**lastSeenDate** | **Long** | Date the user was created, Unix epoch timestamp format |  [optional]
**platform** | **String** | Platform the Client SDK is running on |  [optional]
**platformVersion** | **String** | Version of the platform the Client SDK is running on |  [optional]
**deviceType** | **String** | User&#x27;s device type |  [optional]
**sdkType** | [**SdkTypeEnum**](#SdkTypeEnum) | DevCycle SDK type |  [optional]
**sdkVersion** | **String** | DevCycle SDK Version |  [optional]

<a name="SdkTypeEnum"></a>
## Enum: SdkTypeEnum
Name | Value
---- | -----
API | &quot;api&quot;
SERVER | &quot;server&quot;
