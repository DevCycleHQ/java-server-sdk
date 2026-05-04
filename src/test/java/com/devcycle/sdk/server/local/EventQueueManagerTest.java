package com.devcycle.sdk.server.local;

import com.devcycle.sdk.server.common.api.IDevCycleApi;
import com.devcycle.sdk.server.common.model.DevCycleResponse;
import com.devcycle.sdk.server.helpers.WhiteBox;
import com.devcycle.sdk.server.local.bucketing.LocalBucketing;
import com.devcycle.sdk.server.local.managers.EventQueueManager;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import com.devcycle.sdk.server.local.model.FlushPayload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import retrofit2.Call;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class EventQueueManagerTest {

    /**
     * Verifies that isFlushingEvents is reset to false even when the publish loop throws.
     * Without the try/finally fix, an exception escaping publishEvents() would leave
     * isFlushingEvents stuck true, silently dropping all subsequent flushes.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void flushEvents_resetsIsFlushingEventsOnException() throws Exception {
        LocalBucketing mockBucketing = Mockito.mock(LocalBucketing.class);

        // Large interval so the background scheduler doesn't fire during the test
        DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                .eventFlushIntervalMS(Integer.MAX_VALUE)
                .build();

        // Return empty payloads by default so any early scheduler tick is a no-op
        Mockito.when(mockBucketing.flushEventQueue(anyString())).thenReturn(new FlushPayload[0]);

        EventQueueManager manager = new EventQueueManager("server-key", mockBucketing, "test-uuid", options);

        // Swap in a mock API whose Call.execute() throws to simulate a publish failure
        IDevCycleApi mockApi = Mockito.mock(IDevCycleApi.class);
        Call<DevCycleResponse> mockCall = Mockito.mock(Call.class);
        Mockito.when(mockApi.publishEvents(any())).thenReturn(mockCall);
        Mockito.when(mockCall.execute()).thenThrow(new RuntimeException("simulated publish failure"));
        WhiteBox.setInternalState(manager, "eventsApiClient", mockApi);

        // Return a non-empty payload so flushEvents() reaches the publish loop
        FlushPayload payload = new FlushPayload();
        payload.payloadId = "test-payload-1";
        payload.eventCount = 1;
        payload.records = new FlushPayload.Record[0];
        Mockito.when(mockBucketing.flushEventQueue(anyString())).thenReturn(new FlushPayload[]{payload});

        try {
            manager.flushEvents();
        } catch (RuntimeException e) {
            // expected — the exception should escape flushEvents()
        }

        Field field = EventQueueManager.class.getDeclaredField("isFlushingEvents");
        field.setAccessible(true);
        assertFalse("isFlushingEvents must be reset to false after exception", (boolean) field.get(manager));
    }
}
