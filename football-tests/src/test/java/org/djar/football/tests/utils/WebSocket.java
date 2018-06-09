package org.djar.football.tests.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final BlockingQueue<Callable<Object>> queue = new LinkedBlockingQueue<>();

    private final String url;
    private final WebSocketStompClient client;
    private final Map<String, Class> subscriptions = new LinkedHashMap<>();

    public WebSocket(String url) {
        this.url = url;
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketClient webSocketClient = new SockJsClient(transports);
        client = new WebSocketStompClient(webSocketClient);
        client.setMessageConverter(new MappingJackson2MessageConverter());
    }

    public void subscribe(String topic, Class payloadType) {
        subscriptions.put(topic, payloadType);
    }

    public void connect() {
        client.connect(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                for (Map.Entry<String, Class> subscription : subscriptions.entrySet()) {
                    subscribe(session, subscription.getKey(), subscription.getValue());
                }
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
                                        Throwable exception) {
                queue.add(() -> {
                    throw new RuntimeException(exception);
                });
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                queue.add(() -> {
                    throw new RuntimeException(exception);
                });
            }
        });
    }

    private StompSession.Subscription subscribe(StompSession session, String topic, Class payloadType) {
        return session.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add(() -> payload);
            }
        });
    }

    public void disconnect() {
        client.stop();
    }

    public Object read(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            return queue.poll(timeout, unit).call();
        } catch (RuntimeException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
