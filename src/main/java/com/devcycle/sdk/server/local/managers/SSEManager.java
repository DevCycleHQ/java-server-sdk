package com.devcycle.sdk.server.local.managers;

import com.launchdarkly.eventsource.*;

import java.net.URI;
import java.util.function.Function;

public class SSEManager {

    private EventSource eventSource;
    private Thread messageHandlerThread;
    private URI uri;

    public SSEManager(URI uri) {
        eventSource = new EventSource.Builder(uri).build();
        this.uri = uri;
    }

    public void close() {
        eventSource.close();
    }

    public void restart(URI uri, Function<MessageEvent, Void> messageHandler, Function<FaultEvent, Void> errorHandler) {
        if (this.uri.equals(uri)) {
            return;
        }
        this.uri = uri;
        if (eventSource != null) {
            eventSource.close();
        }
        if (messageHandlerThread != null) {
            messageHandlerThread.interrupt();
        }
        eventSource = new EventSource.Builder(uri).build();
        start(messageHandler, errorHandler);
    }

    private boolean start(Function<MessageEvent, Void> messageHandler, Function<FaultEvent, Void> errorHandler) {
        switch (eventSource.getState()) {
            case CONNECTING:
            case OPEN:
                break;
            case CLOSED:
                eventSource = new EventSource.Builder(uri).build();
                try {
                    eventSource.start();
                } catch (StreamException e) {
                    return false;
                }
                break;
        }
        messageHandlerThread = new Thread(new SSEMessageHandler(eventSource, messageHandler, errorHandler));
        messageHandlerThread.start();
        return true;
    }

    private static class SSEMessageHandler implements Runnable {

        private final Function<MessageEvent, Void> messageHandler;
        private final Function<FaultEvent, Void> errorHandler;
        private final EventSource sse;

        public SSEMessageHandler(EventSource sse, Function<MessageEvent, Void> messageHandler, Function<FaultEvent, Void> errorHandler) {
            this.messageHandler = messageHandler;
            this.errorHandler = errorHandler;
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
                    } else {
                        // ignore other event types
                    }
                } catch (StreamException e) {
                    break;
                }
            }
        }
    }
}
