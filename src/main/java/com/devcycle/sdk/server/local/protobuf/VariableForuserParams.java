// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: variableForUserParams.proto

package com.devcycle.sdk.server.local.protobuf;

public final class VariableForUserParams {
  private VariableForUserParams() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_NullableString_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_NullableString_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_NullableDouble_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_NullableDouble_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_CustomDataValue_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_CustomDataValue_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_NullableCustomData_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_NullableCustomData_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_NullableCustomData_ValueEntry_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_NullableCustomData_ValueEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_VariableForUserParams_PB_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_VariableForUserParams_PB_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_DVCUser_PB_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_DVCUser_PB_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_SDKVariable_PB_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_SDKVariable_PB_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\033variableForUserParams.proto\"/\n\016Nullabl" +
      "eString\022\r\n\005value\030\001 \001(\t\022\016\n\006isNull\030\002 \001(\010\"/" +
      "\n\016NullableDouble\022\r\n\005value\030\001 \001(\001\022\016\n\006isNul" +
      "l\030\002 \001(\010\"m\n\017CustomDataValue\022\035\n\004type\030\001 \001(\016" +
      "2\017.CustomDataType\022\021\n\tboolValue\030\002 \001(\010\022\023\n\013" +
      "doubleValue\030\003 \001(\001\022\023\n\013stringValue\030\004 \001(\t\"\223" +
      "\001\n\022NullableCustomData\022-\n\005value\030\001 \003(\0132\036.N" +
      "ullableCustomData.ValueEntry\022\016\n\006isNull\030\002" +
      " \001(\010\032>\n\nValueEntry\022\013\n\003key\030\001 \001(\t\022\037\n\005value" +
      "\030\002 \001(\0132\020.CustomDataValue:\0028\001\"\234\001\n\030Variabl" +
      "eForUserParams_PB\022\016\n\006sdkKey\030\001 \001(\t\022\023\n\013var" +
      "iableKey\030\002 \001(\t\022&\n\014variableType\030\003 \001(\0162\020.V" +
      "ariableType_PB\022\031\n\004user\030\004 \001(\0132\013.DVCUser_P" +
      "B\022\030\n\020shouldTrackEvent\030\005 \001(\010\"\350\002\n\nDVCUser_" +
      "PB\022\017\n\007user_id\030\001 \001(\t\022\036\n\005email\030\002 \001(\0132\017.Nul" +
      "lableString\022\035\n\004name\030\003 \001(\0132\017.NullableStri" +
      "ng\022!\n\010language\030\004 \001(\0132\017.NullableString\022 \n" +
      "\007country\030\005 \001(\0132\017.NullableString\022!\n\010appBu" +
      "ild\030\006 \001(\0132\017.NullableDouble\022#\n\nappVersion" +
      "\030\007 \001(\0132\017.NullableString\022$\n\013deviceModel\030\010" +
      " \001(\0132\017.NullableString\022\'\n\ncustomData\030\t \001(" +
      "\0132\023.NullableCustomData\022.\n\021privateCustomD" +
      "ata\030\n \001(\0132\023.NullableCustomData\"\254\001\n\016SDKVa" +
      "riable_PB\022\013\n\003_id\030\001 \001(\t\022\036\n\004type\030\002 \001(\0162\020.V" +
      "ariableType_PB\022\013\n\003key\030\003 \001(\t\022\021\n\tboolValue" +
      "\030\004 \001(\010\022\023\n\013doubleValue\030\005 \001(\001\022\023\n\013stringVal" +
      "ue\030\006 \001(\t\022#\n\nevalReason\030\007 \001(\0132\017.NullableS" +
      "tring*@\n\017VariableType_PB\022\013\n\007Boolean\020\000\022\n\n" +
      "\006Number\020\001\022\n\n\006String\020\002\022\010\n\004JSON\020\003*6\n\016Custo" +
      "mDataType\022\010\n\004Bool\020\000\022\007\n\003Num\020\001\022\007\n\003Str\020\002\022\010\n" +
      "\004Null\020\003BX\n&com.devcycle.sdk.server.local" +
      ".protobufP\001Z\007./proto\252\002\"DevCycle.SDK.Serv" +
      "er.Local.Protobufb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_NullableString_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_NullableString_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_NullableString_descriptor,
        new java.lang.String[] { "Value", "IsNull", });
    internal_static_NullableDouble_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_NullableDouble_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_NullableDouble_descriptor,
        new java.lang.String[] { "Value", "IsNull", });
    internal_static_CustomDataValue_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_CustomDataValue_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_CustomDataValue_descriptor,
        new java.lang.String[] { "Type", "BoolValue", "DoubleValue", "StringValue", });
    internal_static_NullableCustomData_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_NullableCustomData_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_NullableCustomData_descriptor,
        new java.lang.String[] { "Value", "IsNull", });
    internal_static_NullableCustomData_ValueEntry_descriptor =
      internal_static_NullableCustomData_descriptor.getNestedTypes().get(0);
    internal_static_NullableCustomData_ValueEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_NullableCustomData_ValueEntry_descriptor,
        new java.lang.String[] { "Key", "Value", });
    internal_static_VariableForUserParams_PB_descriptor =
      getDescriptor().getMessageTypes().get(4);
    internal_static_VariableForUserParams_PB_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_VariableForUserParams_PB_descriptor,
        new java.lang.String[] { "SdkKey", "VariableKey", "VariableType", "User", "ShouldTrackEvent", });
    internal_static_DVCUser_PB_descriptor =
      getDescriptor().getMessageTypes().get(5);
    internal_static_DVCUser_PB_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_DVCUser_PB_descriptor,
        new java.lang.String[] { "UserId", "Email", "Name", "Language", "Country", "AppBuild", "AppVersion", "DeviceModel", "CustomData", "PrivateCustomData", });
    internal_static_SDKVariable_PB_descriptor =
      getDescriptor().getMessageTypes().get(6);
    internal_static_SDKVariable_PB_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_SDKVariable_PB_descriptor,
        new java.lang.String[] { "Id", "Type", "Key", "BoolValue", "DoubleValue", "StringValue", "EvalReason", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
