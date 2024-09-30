package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.launchdarkly.eventsource.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SSEManager {

    private EventSource eventSource;
    private static Thread messageHandlerThread;
    private URI uri;

    public SSEManager(URI uri) {
        this.eventSource = buildEventSource(uri);

        this.uri = uri;
    }

    public void close() {
        eventSource.close();
    }

    public void restart(URI uri, Function<MessageEvent, Void> messageHandler, Function<FaultEvent, Void> errorHandler, Function<StartedEvent, Void> stateHandler) {
        if (this.uri.equals(uri) && eventSource != null && (eventSource.getState() == ReadyState.OPEN || eventSource.getState() == ReadyState.CONNECTING || eventSource.getState() == ReadyState.CLOSED)) {
            return;
        }
        this.uri = uri;
        if (eventSource != null && eventSource.getState() == ReadyState.OPEN) {
            eventSource.close();
        }
        if (messageHandlerThread != null) {
            messageHandlerThread.interrupt();
        }
        eventSource = buildEventSource(uri);
        start(messageHandler, errorHandler, stateHandler);
    }

    private EventSource buildEventSource(URI uri) {
        return new EventSource.Builder(ConnectStrategy.http(uri).clientBuilderActions(clientBuilder ->
                clientBuilder
                        .connectTimeout(100 * 60, TimeUnit.SECONDS)
                        .readTimeout(100 * 60, TimeUnit.SECONDS)
                        .writeTimeout(100 * 60, TimeUnit.SECONDS)
        )).build();
    }

    private boolean start(Function<MessageEvent, Void> messageHandler, Function<FaultEvent, Void> errorHandler, Function<StartedEvent, Void> stateHandler) {
        switch (eventSource.getState()) {
            case CONNECTING:
            case OPEN:
                break;
            case CLOSED:
            case RAW:
                try {
                    eventSource.start();
                } catch (StreamException e) {
                    DevCycleLogger.error("Error starting event source", e);
                    return false;
                }
                break;
        }
        messageHandlerThread = new Thread(new SSEMessageHandler(eventSource, messageHandler, errorHandler, stateHandler));
        messageHandlerThread.start();
        return true;
    }

    private static class SSEMessageHandler implements Runnable {

        private final Function<MessageEvent, Void> messageHandler;
        private final Function<FaultEvent, Void> errorHandler;
        private final Function<StartedEvent, Void> stateHandler;
        private final EventSource sse;

        public SSEMessageHandler(EventSource sse, Function<MessageEvent, Void> messageHandler, Function<FaultEvent, Void> errorHandler, Function<StartedEvent, Void> stateHandler) {
            this.messageHandler = messageHandler;
            this.errorHandler = errorHandler;
            this.stateHandler = stateHandler;
            this.sse = sse;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    StreamEvent event = sse.readAnyEvent();
                    if (event instanceof MessageEvent) {
                        messageHandler.apply((MessageEvent) event);
                    } else if (event instanceof FaultEvent) {
                        errorHandler.apply((FaultEvent) event);
                    } else if (event instanceof StartedEvent) {
                        stateHandler.apply((StartedEvent) event);
                    } else if (event instanceof CommentEvent) {
                        messageHandler.apply(new MessageEvent(((CommentEvent) event).getText()));
                    } else {
                        DevCycleLogger.warning("Unknown event type: " + event.getClass().getName());
                    }
                } catch (StreamException e) {
                    DevCycleLogger.warning("Error reading event");
                    DevCycleLogger.warning(e.getMessage());
                }
            }
        }
    }
}
