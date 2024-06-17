package com.devcycle.sdk.server.utils;

import com.devcycle.sdk.server.local.utils.ByteConversionUtils;
import org.junit.Assert;
import org.junit.Test;

public class ByteConversionUtilsTest {
    @Test
    public void getUnsignedIntTest() {
        byte[] data = {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};
        long expected = 0x12345678L;
        long result = ByteConversionUtils.getUnsignedInt(data);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testWriteInt32LittleEndian() {
        int value = 0;
        byte[] expected = new byte[]{0, 0, 0, 0};
        byte[] result = ByteConversionUtils.intToBytesLittleEndian(value);
        Assert.assertArrayEquals(expected, result);

        value = 123456789;
        expected = new byte[]{(byte) 0x15, (byte) 0xCD, (byte) 0x5B, (byte) 0x07};
        result = ByteConversionUtils.intToBytesLittleEndian(value);
        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testReadInt32LittleEndian() {
        byte[] encodedValue = {(byte) 0x15, (byte) 0xCD, (byte) 0x5B, (byte) 0x07};
        int expected = 123456789;

        int result = ByteConversionUtils.bytesToIntLittleEndian(encodedValue);
        Assert.assertEquals(expected, result);
    }
}
