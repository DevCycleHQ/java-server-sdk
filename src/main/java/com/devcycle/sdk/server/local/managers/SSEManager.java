package com.devcycle.sdk.server.local.managers;

import com.devcycle.sdk.server.common.logging.DevCycleLogger;
import com.launchdarkly.eventsource.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SSEManager {

    private static EventSource eventSource;
    private static Thread messageHandlerThread;
    private URI uri;

    public SSEManager(URI uri) {
        eventSource = new EventSource.Builder(uri)
                .errorStrategy(ErrorStrategy.alwaysContinue())
                .retryDelay(10000, TimeUnit.MILLISECONDS)
                .build();
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
        if (eventSource != null&& eventSource.getState() == ReadyState.OPEN) {
            eventSource.close();
        }
        if (messageHandlerThread != null) {
            messageHandlerThread.interrupt();
        }
        eventSource = new EventSource.Builder(uri).build();
        start(messageHandler, errorHandler, stateHandler);
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
        private final EventSource sse;

        public SSEMessageHandler(EventSource sse, Function<MessageEvent, Void> messageHandler, Function<FaultEvent, Void> errorHandler, Function<StartedEvent, Void> stateHandler) {
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
                        DevCycleLogger.info("Message event received: " + ((MessageEvent) event).getData());
                        messageHandler.apply((MessageEvent) event);
                    } else if (event instanceof FaultEvent) {
                        DevCycleLogger.error("Fault event received: " + ((FaultEvent) event).getCause().getMessage());
                        errorHandler.apply((FaultEvent) event);
                    } else if (event instanceof StartedEvent) {
                        DevCycleLogger.info("Started event received");
                    } else if (event instanceof CommentEvent){
                        DevCycleLogger.info("Comment event received: " + ((CommentEvent) event).getText());
                    } else {
                        DevCycleLogger.error("Unknown event type: " + event.getClass().getName());
                    }
                } catch (StreamException e) {
                    DevCycleLogger.error("Error reading event", e);
                    DevCycleLogger.warning(e.getMessage());
                }
            }
        }
    }
}
