package com.devcycle.sdk.server.local.utils;

public class ByteConversionUtils {

    public static long getUnsignedInt(byte[] data) {
        long result = 0;

        for (int i = 0; i < data.length; i++) {
            result = (result << 8) + (data[i] & 0xFF);
        }

        return result;
    }
    public static byte[] intToBytesLittleEndian(int value) {
        byte[] encodedValue = new byte[4];
        encodedValue[3] = (byte) (value >> 8 * 3);
        encodedValue[2] = (byte) (value >> 8 * 2);
        encodedValue[1] = (byte) (value >> 8);
        encodedValue[0] = (byte) value;
        return encodedValue;
    }

    public static int bytesToIntLittleEndian(byte[] bytes) {
        int i = 4;
        int value = bytes[--i];
        while (--i >= 0) {
            value <<= 8;
            value |= bytes[i] & 0xFF;
        }
        return value;
    }
}
