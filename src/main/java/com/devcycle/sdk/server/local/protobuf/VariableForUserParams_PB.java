// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: variableForUserParams.proto

package com.devcycle.sdk.server.local.protobuf;

/**
 * Protobuf type {@code VariableForUserParams_PB}
 */
public final class VariableForUserParams_PB extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:VariableForUserParams_PB)
    VariableForUserParams_PBOrBuilder {
private static final long serialVersionUID = 0L;
  // Use VariableForUserParams_PB.newBuilder() to construct.
  private VariableForUserParams_PB(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private VariableForUserParams_PB() {
    sdkKey_ = "";
    variableKey_ = "";
    variableType_ = 0;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new VariableForUserParams_PB();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.devcycle.sdk.server.local.protobuf.VariableForUserParams.internal_static_VariableForUserParams_PB_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.devcycle.sdk.server.local.protobuf.VariableForUserParams.internal_static_VariableForUserParams_PB_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB.class, com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB.Builder.class);
  }

  public static final int SDKKEY_FIELD_NUMBER = 1;
  @SuppressWarnings("serial")
  private volatile java.lang.Object sdkKey_ = "";
  /**
   * <code>string sdkKey = 1;</code>
   * @return The sdkKey.
   */
  @java.lang.Override
  public java.lang.String getSdkKey() {
    java.lang.Object ref = sdkKey_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      sdkKey_ = s;
      return s;
    }
  }
  /**
   * <code>string sdkKey = 1;</code>
   * @return The bytes for sdkKey.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getSdkKeyBytes() {
    java.lang.Object ref = sdkKey_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      sdkKey_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int VARIABLEKEY_FIELD_NUMBER = 2;
  @SuppressWarnings("serial")
  private volatile java.lang.Object variableKey_ = "";
  /**
   * <code>string variableKey = 2;</code>
   * @return The variableKey.
   */
  @java.lang.Override
  public java.lang.String getVariableKey() {
    java.lang.Object ref = variableKey_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      variableKey_ = s;
      return s;
    }
  }
  /**
   * <code>string variableKey = 2;</code>
   * @return The bytes for variableKey.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getVariableKeyBytes() {
    java.lang.Object ref = variableKey_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      variableKey_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int VARIABLETYPE_FIELD_NUMBER = 3;
  private int variableType_ = 0;
  /**
   * <code>.VariableType_PB variableType = 3;</code>
   * @return The enum numeric value on the wire for variableType.
   */
  @java.lang.Override public int getVariableTypeValue() {
    return variableType_;
  }
  /**
   * <code>.VariableType_PB variableType = 3;</code>
   * @return The variableType.
   */
  @java.lang.Override public com.devcycle.sdk.server.local.protobuf.VariableType_PB getVariableType() {
    com.devcycle.sdk.server.local.protobuf.VariableType_PB result = com.devcycle.sdk.server.local.protobuf.VariableType_PB.forNumber(variableType_);
    return result == null ? com.devcycle.sdk.server.local.protobuf.VariableType_PB.UNRECOGNIZED : result;
  }

  public static final int USER_FIELD_NUMBER = 4;
  private com.devcycle.sdk.server.local.protobuf.DVCUser_PB user_;
  /**
   * <code>.DVCUser_PB user = 4;</code>
   * @return Whether the user field is set.
   */
  @java.lang.Override
  public boolean hasUser() {
    return user_ != null;
  }
  /**
   * <code>.DVCUser_PB user = 4;</code>
   * @return The user.
   */
  @java.lang.Override
  public com.devcycle.sdk.server.local.protobuf.DVCUser_PB getUser() {
    return user_ == null ? com.devcycle.sdk.server.local.protobuf.DVCUser_PB.getDefaultInstance() : user_;
  }
  /**
   * <code>.DVCUser_PB user = 4;</code>
   */
  @java.lang.Override
  public com.devcycle.sdk.server.local.protobuf.DVCUser_PBOrBuilder getUserOrBuilder() {
    return user_ == null ? com.devcycle.sdk.server.local.protobuf.DVCUser_PB.getDefaultInstance() : user_;
  }

  public static final int SHOULDTRACKEVENT_FIELD_NUMBER = 5;
  private boolean shouldTrackEvent_ = false;
  /**
   * <code>bool shouldTrackEvent = 5;</code>
   * @return The shouldTrackEvent.
   */
  @java.lang.Override
  public boolean getShouldTrackEvent() {
    return shouldTrackEvent_;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(sdkKey_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, sdkKey_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(variableKey_)) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, variableKey_);
    }
    if (variableType_ != com.devcycle.sdk.server.local.protobuf.VariableType_PB.Boolean.getNumber()) {
      output.writeEnum(3, variableType_);
    }
    if (user_ != null) {
      output.writeMessage(4, getUser());
    }
    if (shouldTrackEvent_ != false) {
      output.writeBool(5, shouldTrackEvent_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(sdkKey_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, sdkKey_);
    }
    if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(variableKey_)) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, variableKey_);
    }
    if (variableType_ != com.devcycle.sdk.server.local.protobuf.VariableType_PB.Boolean.getNumber()) {
      size += com.google.protobuf.CodedOutputStream
        .computeEnumSize(3, variableType_);
    }
    if (user_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(4, getUser());
    }
    if (shouldTrackEvent_ != false) {
      size += com.google.protobuf.CodedOutputStream
        .computeBoolSize(5, shouldTrackEvent_);
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB)) {
      return super.equals(obj);
    }
    com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB other = (com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB) obj;

    if (!getSdkKey()
        .equals(other.getSdkKey())) return false;
    if (!getVariableKey()
        .equals(other.getVariableKey())) return false;
    if (variableType_ != other.variableType_) return false;
    if (hasUser() != other.hasUser()) return false;
    if (hasUser()) {
      if (!getUser()
          .equals(other.getUser())) return false;
    }
    if (getShouldTrackEvent()
        != other.getShouldTrackEvent()) return false;
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + SDKKEY_FIELD_NUMBER;
    hash = (53 * hash) + getSdkKey().hashCode();
    hash = (37 * hash) + VARIABLEKEY_FIELD_NUMBER;
    hash = (53 * hash) + getVariableKey().hashCode();
    hash = (37 * hash) + VARIABLETYPE_FIELD_NUMBER;
    hash = (53 * hash) + variableType_;
    if (hasUser()) {
      hash = (37 * hash) + USER_FIELD_NUMBER;
      hash = (53 * hash) + getUser().hashCode();
    }
    hash = (37 * hash) + SHOULDTRACKEVENT_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
        getShouldTrackEvent());
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code VariableForUserParams_PB}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:VariableForUserParams_PB)
      com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PBOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.devcycle.sdk.server.local.protobuf.VariableForUserParams.internal_static_VariableForUserParams_PB_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.devcycle.sdk.server.local.protobuf.VariableForUserParams.internal_static_VariableForUserParams_PB_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB.class, com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB.Builder.class);
    }

    // Construct using com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      sdkKey_ = "";
      variableKey_ = "";
      variableType_ = 0;
      user_ = null;
      if (userBuilder_ != null) {
        userBuilder_.dispose();
        userBuilder_ = null;
      }
      shouldTrackEvent_ = false;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.devcycle.sdk.server.local.protobuf.VariableForUserParams.internal_static_VariableForUserParams_PB_descriptor;
    }

    @java.lang.Override
    public com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB getDefaultInstanceForType() {
      return com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB.getDefaultInstance();
    }

    @java.lang.Override
    public com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB build() {
      com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB buildPartial() {
      com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB result = new com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.sdkKey_ = sdkKey_;
      }
      if (((from_bitField0_ & 0x00000002) != 0)) {
        result.variableKey_ = variableKey_;
      }
      if (((from_bitField0_ & 0x00000004) != 0)) {
        result.variableType_ = variableType_;
      }
      if (((from_bitField0_ & 0x00000008) != 0)) {
        result.user_ = userBuilder_ == null
            ? user_
            : userBuilder_.build();
      }
      if (((from_bitField0_ & 0x00000010) != 0)) {
        result.shouldTrackEvent_ = shouldTrackEvent_;
      }
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB) {
        return mergeFrom((com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB other) {
      if (other == com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB.getDefaultInstance()) return this;
      if (!other.getSdkKey().isEmpty()) {
        sdkKey_ = other.sdkKey_;
        bitField0_ |= 0x00000001;
        onChanged();
      }
      if (!other.getVariableKey().isEmpty()) {
        variableKey_ = other.variableKey_;
        bitField0_ |= 0x00000002;
        onChanged();
      }
      if (other.variableType_ != 0) {
        setVariableTypeValue(other.getVariableTypeValue());
      }
      if (other.hasUser()) {
        mergeUser(other.getUser());
      }
      if (other.getShouldTrackEvent() != false) {
        setShouldTrackEvent(other.getShouldTrackEvent());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              sdkKey_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000001;
              break;
            } // case 10
            case 18: {
              variableKey_ = input.readStringRequireUtf8();
              bitField0_ |= 0x00000002;
              break;
            } // case 18
            case 24: {
              variableType_ = input.readEnum();
              bitField0_ |= 0x00000004;
              break;
            } // case 24
            case 34: {
              input.readMessage(
                  getUserFieldBuilder().getBuilder(),
                  extensionRegistry);
              bitField0_ |= 0x00000008;
              break;
            } // case 34
            case 40: {
              shouldTrackEvent_ = input.readBool();
              bitField0_ |= 0x00000010;
              break;
            } // case 40
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private java.lang.Object sdkKey_ = "";
    /**
     * <code>string sdkKey = 1;</code>
     * @return The sdkKey.
     */
    public java.lang.String getSdkKey() {
      java.lang.Object ref = sdkKey_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        sdkKey_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string sdkKey = 1;</code>
     * @return The bytes for sdkKey.
     */
    public com.google.protobuf.ByteString
        getSdkKeyBytes() {
      java.lang.Object ref = sdkKey_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        sdkKey_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string sdkKey = 1;</code>
     * @param value The sdkKey to set.
     * @return This builder for chaining.
     */
    public Builder setSdkKey(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      sdkKey_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>string sdkKey = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearSdkKey() {
      sdkKey_ = getDefaultInstance().getSdkKey();
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <code>string sdkKey = 1;</code>
     * @param value The bytes for sdkKey to set.
     * @return This builder for chaining.
     */
    public Builder setSdkKeyBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      sdkKey_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }

    private java.lang.Object variableKey_ = "";
    /**
     * <code>string variableKey = 2;</code>
     * @return The variableKey.
     */
    public java.lang.String getVariableKey() {
      java.lang.Object ref = variableKey_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        variableKey_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string variableKey = 2;</code>
     * @return The bytes for variableKey.
     */
    public com.google.protobuf.ByteString
        getVariableKeyBytes() {
      java.lang.Object ref = variableKey_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        variableKey_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string variableKey = 2;</code>
     * @param value The variableKey to set.
     * @return This builder for chaining.
     */
    public Builder setVariableKey(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      variableKey_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }
    /**
     * <code>string variableKey = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearVariableKey() {
      variableKey_ = getDefaultInstance().getVariableKey();
      bitField0_ = (bitField0_ & ~0x00000002);
      onChanged();
      return this;
    }
    /**
     * <code>string variableKey = 2;</code>
     * @param value The bytes for variableKey to set.
     * @return This builder for chaining.
     */
    public Builder setVariableKeyBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      variableKey_ = value;
      bitField0_ |= 0x00000002;
      onChanged();
      return this;
    }

    private int variableType_ = 0;
    /**
     * <code>.VariableType_PB variableType = 3;</code>
     * @return The enum numeric value on the wire for variableType.
     */
    @java.lang.Override public int getVariableTypeValue() {
      return variableType_;
    }
    /**
     * <code>.VariableType_PB variableType = 3;</code>
     * @param value The enum numeric value on the wire for variableType to set.
     * @return This builder for chaining.
     */
    public Builder setVariableTypeValue(int value) {
      variableType_ = value;
      bitField0_ |= 0x00000004;
      onChanged();
      return this;
    }
    /**
     * <code>.VariableType_PB variableType = 3;</code>
     * @return The variableType.
     */
    @java.lang.Override
    public com.devcycle.sdk.server.local.protobuf.VariableType_PB getVariableType() {
      com.devcycle.sdk.server.local.protobuf.VariableType_PB result = com.devcycle.sdk.server.local.protobuf.VariableType_PB.forNumber(variableType_);
      return result == null ? com.devcycle.sdk.server.local.protobuf.VariableType_PB.UNRECOGNIZED : result;
    }
    /**
     * <code>.VariableType_PB variableType = 3;</code>
     * @param value The variableType to set.
     * @return This builder for chaining.
     */
    public Builder setVariableType(com.devcycle.sdk.server.local.protobuf.VariableType_PB value) {
      if (value == null) {
        throw new NullPointerException();
      }
      bitField0_ |= 0x00000004;
      variableType_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     * <code>.VariableType_PB variableType = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearVariableType() {
      bitField0_ = (bitField0_ & ~0x00000004);
      variableType_ = 0;
      onChanged();
      return this;
    }

    private com.devcycle.sdk.server.local.protobuf.DVCUser_PB user_;
    private com.google.protobuf.SingleFieldBuilderV3<
        com.devcycle.sdk.server.local.protobuf.DVCUser_PB, com.devcycle.sdk.server.local.protobuf.DVCUser_PB.Builder, com.devcycle.sdk.server.local.protobuf.DVCUser_PBOrBuilder> userBuilder_;
    /**
     * <code>.DVCUser_PB user = 4;</code>
     * @return Whether the user field is set.
     */
    public boolean hasUser() {
      return ((bitField0_ & 0x00000008) != 0);
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     * @return The user.
     */
    public com.devcycle.sdk.server.local.protobuf.DVCUser_PB getUser() {
      if (userBuilder_ == null) {
        return user_ == null ? com.devcycle.sdk.server.local.protobuf.DVCUser_PB.getDefaultInstance() : user_;
      } else {
        return userBuilder_.getMessage();
      }
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     */
    public Builder setUser(com.devcycle.sdk.server.local.protobuf.DVCUser_PB value) {
      if (userBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        user_ = value;
      } else {
        userBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000008;
      onChanged();
      return this;
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     */
    public Builder setUser(
        com.devcycle.sdk.server.local.protobuf.DVCUser_PB.Builder builderForValue) {
      if (userBuilder_ == null) {
        user_ = builderForValue.build();
      } else {
        userBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000008;
      onChanged();
      return this;
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     */
    public Builder mergeUser(com.devcycle.sdk.server.local.protobuf.DVCUser_PB value) {
      if (userBuilder_ == null) {
        if (((bitField0_ & 0x00000008) != 0) &&
          user_ != null &&
          user_ != com.devcycle.sdk.server.local.protobuf.DVCUser_PB.getDefaultInstance()) {
          getUserBuilder().mergeFrom(value);
        } else {
          user_ = value;
        }
      } else {
        userBuilder_.mergeFrom(value);
      }
      bitField0_ |= 0x00000008;
      onChanged();
      return this;
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     */
    public Builder clearUser() {
      bitField0_ = (bitField0_ & ~0x00000008);
      user_ = null;
      if (userBuilder_ != null) {
        userBuilder_.dispose();
        userBuilder_ = null;
      }
      onChanged();
      return this;
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     */
    public com.devcycle.sdk.server.local.protobuf.DVCUser_PB.Builder getUserBuilder() {
      bitField0_ |= 0x00000008;
      onChanged();
      return getUserFieldBuilder().getBuilder();
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     */
    public com.devcycle.sdk.server.local.protobuf.DVCUser_PBOrBuilder getUserOrBuilder() {
      if (userBuilder_ != null) {
        return userBuilder_.getMessageOrBuilder();
      } else {
        return user_ == null ?
            com.devcycle.sdk.server.local.protobuf.DVCUser_PB.getDefaultInstance() : user_;
      }
    }
    /**
     * <code>.DVCUser_PB user = 4;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.devcycle.sdk.server.local.protobuf.DVCUser_PB, com.devcycle.sdk.server.local.protobuf.DVCUser_PB.Builder, com.devcycle.sdk.server.local.protobuf.DVCUser_PBOrBuilder> 
        getUserFieldBuilder() {
      if (userBuilder_ == null) {
        userBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.devcycle.sdk.server.local.protobuf.DVCUser_PB, com.devcycle.sdk.server.local.protobuf.DVCUser_PB.Builder, com.devcycle.sdk.server.local.protobuf.DVCUser_PBOrBuilder>(
                getUser(),
                getParentForChildren(),
                isClean());
        user_ = null;
      }
      return userBuilder_;
    }

    private boolean shouldTrackEvent_ ;
    /**
     * <code>bool shouldTrackEvent = 5;</code>
     * @return The shouldTrackEvent.
     */
    @java.lang.Override
    public boolean getShouldTrackEvent() {
      return shouldTrackEvent_;
    }
    /**
     * <code>bool shouldTrackEvent = 5;</code>
     * @param value The shouldTrackEvent to set.
     * @return This builder for chaining.
     */
    public Builder setShouldTrackEvent(boolean value) {
      
      shouldTrackEvent_ = value;
      bitField0_ |= 0x00000010;
      onChanged();
      return this;
    }
    /**
     * <code>bool shouldTrackEvent = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearShouldTrackEvent() {
      bitField0_ = (bitField0_ & ~0x00000010);
      shouldTrackEvent_ = false;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:VariableForUserParams_PB)
  }

  // @@protoc_insertion_point(class_scope:VariableForUserParams_PB)
  private static final com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB();
  }

  public static com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<VariableForUserParams_PB>
      PARSER = new com.google.protobuf.AbstractParser<VariableForUserParams_PB>() {
    @java.lang.Override
    public VariableForUserParams_PB parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<VariableForUserParams_PB> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<VariableForUserParams_PB> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.devcycle.sdk.server.local.protobuf.VariableForUserParams_PB getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

