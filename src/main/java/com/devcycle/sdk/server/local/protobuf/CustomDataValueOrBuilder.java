// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: variableForUserParams.proto

package com.devcycle.sdk.server.local.protobuf;

public interface CustomDataValueOrBuilder extends
    // @@protoc_insertion_point(interface_extends:CustomDataValue)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.CustomDataType type = 1;</code>
   * @return The enum numeric value on the wire for type.
   */
  int getTypeValue();
  /**
   * <code>.CustomDataType type = 1;</code>
   * @return The type.
   */
  com.devcycle.sdk.server.local.protobuf.CustomDataType getType();

  /**
   * <code>bool boolValue = 2;</code>
   * @return The boolValue.
   */
  boolean getBoolValue();

  /**
   * <code>double doubleValue = 3;</code>
   * @return The doubleValue.
   */
  double getDoubleValue();

  /**
   * <code>string stringValue = 4;</code>
   * @return The stringValue.
   */
  java.lang.String getStringValue();
  /**
   * <code>string stringValue = 4;</code>
   * @return The bytes for stringValue.
   */
  com.google.protobuf.ByteString
      getStringValueBytes();
}
