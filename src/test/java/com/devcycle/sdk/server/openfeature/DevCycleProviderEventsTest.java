package com.devcycle.sdk.server.openfeature;

import com.devcycle.sdk.server.helpers.LocalConfigServer;
import com.devcycle.sdk.server.helpers.TestDataFixtures;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import dev.openfeature.sdk.EventDetails;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.exceptions.FatalError;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for the provider events emitted off the back of the Local SDK config lifecycle
 */
public class DevCycleProviderEventsTest {
    private static final int PORT = 9010;
    private static final int POLL_INTERVAL_MS = 1000;
    private static final int EVENT_TIMEOUT_SECONDS = 20;

    private final String apiKey = String.format("server-%s", UUID.randomUUID());
    private final OpenFeatureAPI api = OpenFeatureAPI.getInstance();

    private LocalConfigServer localConfigServer;
    private DevCycleLocalClient client;

    @Before
    public void setup() throws Exception {
        localConfigServer = new LocalConfigServer(TestDataFixtures.SmallConfig(), PORT);
        localConfigServer.start();
    }

    @After
    public void cleanup() {
        if (client != null) {
            client.close();
        }
        localConfigServer.stop();
    }

    private DevCycleLocalClient createClient() {
        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .configCdnBaseUrl(localConfigServer.getHostRootURL())
                .configPollingIntervalMS(POLL_INTERVAL_MS)
                .disableRealtimeUpdates(true)
                .build();
        return new DevCycleLocalClient(apiKey, options);
    }

    private DevCycleLocalClient createInitializedClient() throws InterruptedException {
        DevCycleLocalClient client = createClient();
        long deadline = System.currentTimeMillis() + 10000;
        while (!client.isInitialized()) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("Client failed to initialize in 10 seconds");
            }
            Thread.sleep(50);
        }
        return client;
    }

    @Test
    public void testEmitsConfigurationChangedWhenConfigIsUpdated() throws Exception {
        client = createInitializedClient();
        FeatureProvider provider = client.getOpenFeatureProvider();

        String domain = "config-changed-" + UUID.randomUUID();
        api.setProviderAndWait(domain, provider);

        CountDownLatch configurationChanged = new CountDownLatch(1);
        AtomicReference<EventDetails> received = new AtomicReference<>();
        api.getClient(domain).onProviderConfigurationChanged(details -> {
            received.set(details);
            configurationChanged.countDown();
        });

        localConfigServer.setETag("\"test-etag-updated\"");

        Assert.assertTrue(
                "expected PROVIDER_CONFIGURATION_CHANGED after the config ETag changed",
                configurationChanged.await(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertEquals(
                "\"test-etag-updated\"",
                received.get().getEventMetadata().getString("configETag"));
        // DevCycle resolves variables per-user, so no flag key list is reported
        Assert.assertNull(received.get().getFlagsChanged());
    }

    @Test
    public void testDoesNotEmitConfigurationChangedWhenConfigIsUnchanged() throws Exception {
        client = createInitializedClient();
        FeatureProvider provider = client.getOpenFeatureProvider();

        String domain = "config-unchanged-" + UUID.randomUUID();
        api.setProviderAndWait(domain, provider);

        CountDownLatch configurationChanged = new CountDownLatch(1);
        api.getClient(domain).onProviderConfigurationChanged(details -> configurationChanged.countDown());

        // the config server keeps serving the same ETag, so several polls happen with no change
        Assert.assertFalse(
                "expected no PROVIDER_CONFIGURATION_CHANGED while the config ETag is unchanged",
                configurationChanged.await(POLL_INTERVAL_MS * 3L, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testEmitsStaleWhenConfigRefreshFailsThenReadyOnRecovery() throws Exception {
        client = createInitializedClient();
        FeatureProvider provider = client.getOpenFeatureProvider();

        String domain = "stale-" + UUID.randomUUID();
        api.setProviderAndWait(domain, provider);

        CountDownLatch stale = new CountDownLatch(1);
        api.getClient(domain).onProviderStale(details -> stale.countDown());

        localConfigServer.setResponseCode(500);

        Assert.assertTrue(
                "expected PROVIDER_STALE when the config could not be refreshed",
                stale.await(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // the cached config is still served while stale
        Assert.assertTrue(client.isInitialized());

        // registered only now: a handler added while the provider is already READY fires immediately
        CountDownLatch ready = new CountDownLatch(1);
        api.getClient(domain).onProviderReady(details -> ready.countDown());

        localConfigServer.setResponseCode(200);
        localConfigServer.setETag("\"test-etag-recovered\"");

        Assert.assertTrue(
                "expected PROVIDER_READY once config fetching recovered",
                ready.await(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testEmitsReadyWhenConfigArrivesAfterInitializationFailed() throws Exception {
        localConfigServer.setResponseCode(500);

        client = createClient();
        FeatureProvider provider = client.getOpenFeatureProvider();

        String domain = "late-config-" + UUID.randomUUID();
        CountDownLatch errored = new CountDownLatch(1);
        api.setProvider(domain, provider);
        api.getClient(domain).onProviderError(details -> errored.countDown());

        Assert.assertTrue(
                "expected PROVIDER_ERROR when no config could be fetched",
                errored.await(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        // the SDK does not call initialize() again, so the provider has to report readiness itself
        CountDownLatch ready = new CountDownLatch(1);
        api.getClient(domain).onProviderReady(details -> ready.countDown());

        localConfigServer.setResponseCode(200);

        Assert.assertTrue(
                "expected PROVIDER_READY once a config was finally fetched",
                ready.await(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertTrue(client.isInitialized());
    }

    @Test
    public void testInitializeThrowsFatalErrorWhenSDKKeyIsUnauthorized() throws Exception {
        localConfigServer.setResponseCode(401);

        client = createClient();
        FeatureProvider provider = client.getOpenFeatureProvider();

        Assert.assertThrows(
                FatalError.class,
                () -> provider.initialize(new ImmutableContext("test-1234")));
    }
}
