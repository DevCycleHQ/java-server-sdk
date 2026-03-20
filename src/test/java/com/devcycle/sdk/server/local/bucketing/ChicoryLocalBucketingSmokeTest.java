package com.devcycle.sdk.server.local.bucketing;

import org.junit.Assert;
import org.junit.Test;

/** Ensures the Chicory WASM path loads; default {@link LocalBucketing} tests cover Wasmtime. */
public class ChicoryLocalBucketingSmokeTest {

    @Test
    public void constructsAndLoadsWasm() {
        ChicoryLocalBucketing bucketing = new ChicoryLocalBucketing();
        Assert.assertNotNull(bucketing);
    }
}
