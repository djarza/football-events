package org.djar.football.tests.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

public class WebSocket {

    private final String url;
    private final WebSocketStompClient client;
    private final Map<Class, Subscription> subscriptions = new HashMap<>();

    public WebSocket(String url) {
        this.url = url;
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketClient webSocketClient = new SockJsClient(transports);
        client = new WebSocketStompClient(webSocketClient);
        client.setMessageConverter(new MappingJackson2MessageConverter());
    }

    public void subscribe(String topic, Class payloadType) {
        subscriptions.put(payloadType, new Subscription(topic, payloadType));
    }

    public void connect() {
        client.connect(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                for (Subscription subscription : subscriptions.values()) {
                    subscribeSession(session, subscription);
                }
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                    byte[] payload, Throwable exception) {
                throw new RuntimeException(exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private StompSession.Subscription subscribeSession(StompSession session, Subscription subscription) {
        return session.subscribe(subscription.topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return subscription.payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                subscription.add(payload);
            }
        });
    }

    public void disconnect() {
        client.stop();
    }

    public <T> T readLast(Class<T> payloadType, long timeout, TimeUnit unit)  {
        Subscription subscription = Objects.requireNonNull(subscriptions.get(payloadType));
        return (T) subscription.getLast(timeout, unit);
    }

    public <T> List<T> readAll(Class<T> payloadType, int count, long timeout, TimeUnit unit) {
        Subscription subscription = Objects.requireNonNull(subscriptions.get(payloadType));
        return subscription.getAll(count, timeout, unit);
    }

    private class Subscription<T> {

        private final String topic;
        private final Class<T> payloadType;
        private final BlockingQueue<Callable<T>> queue = new LinkedBlockingQueue<>();

        public Subscription(String topic, Class<T> payloadType) {
            this.topic = topic;
            this.payloadType = payloadType;
        }

        public void add(Object payload) {
            if (!payloadType.isInstance(payload)) {
                throw new IllegalArgumentException("The expected payload type is " + payloadType + ", but found "
                    + String.valueOf(payload));
            }
            queue.add(() -> (T)payload);
        }

        public T getLast(long timeout, TimeUnit unit) {
            try {
                Callable<T> recent = null;

                while (true) {
                    Callable<T> item = queue.poll();

                    if (item == null) {
                        break;
                    }
                    recent = item;
                }
                if (recent == null) {
                    recent = queue.poll(timeout, unit);
                }
                return recent.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public List<T> getAll(int count, long timeout, TimeUnit unit) {
            List<T> result = new ArrayList<>(count);
            long currentTimeout = unit.toMillis(timeout);
            long endTime = System.currentTimeMillis() + currentTimeout;

            try {
                do {
                    Callable<T> callable = queue.poll(timeout, TimeUnit.MILLISECONDS);

                    if (callable == null) {
                        break;
                    }
                    result.add(callable.call());
                    currentTimeout = endTime - System.currentTimeMillis();
                } while (result.size() < count && currentTimeout > 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (result == null) {
                throw new RuntimeException("Timeout");
            }
            return result;
        }
    }
}
