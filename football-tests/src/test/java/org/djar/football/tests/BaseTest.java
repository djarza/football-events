package org.djar.football.tests;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.djar.football.model.event.Event;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.tests.utils.DockerCompose;
import org.djar.football.tests.utils.WebSocket;
import org.djar.football.util.Topics;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

    private static final long EVENT_TIMEOUT = 10000;
    private static final long REST_RETRY_TIMEOUT = 10000;

    protected static DockerCompose dockerCompose;
    protected static RestTemplate rest;
    protected static JdbcTemplate postgres;
    protected static WebSocket webSocket;

    protected static final Properties consumerProps = new Properties();

    protected static HttpStatus command(String url, HttpMethod method, String json, HttpStatus retryStatus) {
        HttpStatus currentStatus;
        long endTime = System.currentTimeMillis() + REST_RETRY_TIMEOUT;

        try {
            do {
                currentStatus = command(url, method, json);

                if (!currentStatus.equals(retryStatus)) {
                    return currentStatus;
                }
                logger.trace("Retry status received ({}), trying again...", retryStatus);
                Thread.sleep(500);
            } while (System.currentTimeMillis() < endTime);

            throw new AssertionError("Response timeout, last status: " + currentStatus);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
            return null;
        }
    }

    protected static HttpStatus command(String url, HttpMethod method, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        logger.trace(json);

        try {
            return rest.exchange(url, method, new HttpEntity<>(json, headers), String.class).getStatusCode();
        } catch (HttpClientErrorException e) {
            return e.getStatusCode();
        }
    }

    protected static <T> T query(String url, Class<T> responseType, int expectedResultCount) throws IOException {
        long timeout = System.currentTimeMillis() + REST_RETRY_TIMEOUT;
        int resultSize = -1;

        try {
            do {
                ResponseEntity<String> response = rest.getForEntity(url, String.class);
                logger.trace(response.getBody());
                T result = new ObjectMapper().readerFor(responseType).readValue(response.getBody());
                resultSize = ((Object[]) result).length;

                if (resultSize == expectedResultCount) {
                    return result;
                }
                logger.trace(resultSize + " items received, trying again...");
                Thread.sleep(500);
            } while (System.currentTimeMillis() > timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
        }
        throw new AssertionError("Expected items: " + expectedResultCount + ", actual: " + resultSize);
    }

    protected static <T extends Event> T waitForEvent(Class<T> type) {
        return waitForEvents(type, 1).get(0);
    }

    protected static <T extends Event> List<T> waitForEvents(Class<T> type, int expectedEventCount) {
        KafkaConsumer<String, T> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(),
                new JsonPojoSerde<T>(type));

        try {
            String topic = Topics.eventTopicName(type);
            consumer.subscribe(Collections.singletonList(topic));
            List<T> found = new ArrayList<>(expectedEventCount);
            long timeout = EVENT_TIMEOUT;
            long endTime = System.currentTimeMillis() + timeout;

            do {
                for (ConsumerRecord<String, T> record : consumer.poll(timeout)) {
                    found.add(record.value());
                }
                timeout = endTime - System.currentTimeMillis();
            } while (found.size() < expectedEventCount && timeout > 0);

            if (found.size() < expectedEventCount) {
                Assert.fail("The expected number of waitForEvents in topic " + topic + " should be: "
                        + expectedEventCount + ", but found: " + found);
            }
            if (found.size() > expectedEventCount) {
                logger.warn("Some redundant waitForEvents have been found in topic {}: {}", topic, found);
            }
            return found;
        } finally {
            consumer.close();
        }
    }

    protected static <T> T waitForWebSocket(Class<T> type) {
        Object event = webSocket.readLast(type, EVENT_TIMEOUT, MILLISECONDS);

        if (event == null) {
            throw new AssertionError("The expected WebSocket waitForEvent " + type.getSimpleName() + " was not found");
        }
        if (!type.isInstance(event)) {
            Assert.fail("The expected WebSocket waitForEvent is " + type.getSimpleName()
                + ", but found: " + event.getClass());
        }
        return (T)event;
    }

    protected static <T> List<T> waitForWebSocket(Class<T> type, int expectedCount) {
        List<T> events = webSocket.readAll(type, expectedCount, EVENT_TIMEOUT, TimeUnit.MILLISECONDS);

        if (events.size() < expectedCount) {
            Assert.fail("The expected number of WebSocket waitForEvents " + type + " should be: " + expectedCount
                + ", but found: " + events);
        }
        return events;
    }

    protected static void createConnector(String connectorRestApiUrl, String request) {
        String json;

        try {
            json = StreamUtils.copyToString(
                BasicIntegrationTest.class.getResourceAsStream(request), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpStatus status = rest.exchange(connectorRestApiUrl, POST, new HttpEntity<>(json, headers), String.class)
                .getStatusCode();

        if (status != CREATED) {
            if (status != CONFLICT) {
                throw new RuntimeException("Unable to create Kafka connector, HTTP status: " + status);
            }
            logger.warn("Connector already exists");
        }
    }
}
