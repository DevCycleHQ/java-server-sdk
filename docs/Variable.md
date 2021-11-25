# Variable

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**_id** | **String** | unique database id | 
**key** | **String** | Unique key by Project, can be used in the SDK / API to reference by &#x27;key&#x27; rather than _id. | 
**type** | [**TypeEnum**](#TypeEnum) | Variable type | 
**value** | **Object** | Variable value can be a string, number, boolean, or JSON | 

<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
STRING | &quot;String&quot;
BOOLEAN | &quot;Boolean&quot;
NUMBER | &quot;Number&quot;
JSON | &quot;JSON&quot;
