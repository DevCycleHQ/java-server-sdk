# Feature

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | unique database id | 
**key** | **String** | Unique key by Project, can be used in the SDK / API to reference by &#x27;key&#x27; rather than _id. | 
**type** | [**TypeEnum**](#TypeEnum) | Feature type | 
**variation** | **String** | Bucketed feature variation | 
**evalReason** | **String** | Evaluation reasoning |  [optional]

<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
RELEASE | &quot;release&quot;
EXPERIMENT | &quot;experiment&quot;
PERMISSION | &quot;permission&quot;
OPS | &quot;ops&quot;
