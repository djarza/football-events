package org.djar.football.tests;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.djar.football.Topics;
import org.djar.football.event.CardReceived;
import org.djar.football.event.Event;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchScheduled;
import org.djar.football.event.MatchStarted;
import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.model.MatchScore;
import org.djar.football.model.PlayerStatistic;
import org.djar.football.tests.utils.DockerCompose;
import org.djar.football.tests.utils.Errors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class BasicIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicIntegrationTest.class);

    private static final int EVENT_TIMEOUT = 30000;
    private static final int REST_RETRY_TIMEOUT = 5000;

    private static DockerCompose dockerCompose;
    private static RestTemplate rest;
    private static JdbcTemplate postgres;
    private static KafkaConsumer<String, String> eventConsumer;

    @Rule
    public Errors errors = new Errors();

    @BeforeClass
    public static void setup()  {
        dockerCompose = new DockerCompose()
            .addHealthCheck("http://football-match:18081/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://football-player:18082/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://football-query:18083/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://football-ui:18080/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://connect:8083/connectors", "\\[.*\\]"); // match any response

        rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 20000);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, BasicIntegrationTest.class.getName());
        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, BasicIntegrationTest.class.getName());
        eventConsumer = new KafkaConsumer<>(consumerProps);

        dockerCompose.up();
        dockerCompose.waitUntilServicesAreAvailable(180, SECONDS);

        // create database with some initial data
        postgres = new JdbcTemplate(new SimpleDriverDataSource(new Driver(),
            "jdbc:postgresql://postgres:5432/postgres", "postgres", "postgres"));
        postgres.execute("CREATE TABLE IF NOT EXISTS players (id bigint PRIMARY KEY, name varchar(50) NOT NULL)");
        postgres.execute("INSERT INTO players VALUES (101, 'Player One') ON CONFLICT DO NOTHING");
        postgres.execute("INSERT INTO players VALUES (102, 'Player Two') ON CONFLICT DO NOTHING");

        createConnector("http://connect:8083/connectors/", "football-connector.json");
    }

    @AfterClass
    public static void cleanup() {
        eventConsumer.close();

        if (Errors.count() == 0) {
            dockerCompose.down();
        }
    }

    @Test
    public void playAMatch() throws Exception {
        // no matches and no scores
        get("http://football-ui:18080/ui/matchScores", MatchScore[].class, 0);

        // create another player
        postgres.execute("INSERT INTO players VALUES (103, 'Player Three') ON CONFLICT DO NOTHING");
        assertEvents(PlayerStartedCareer.class, "101", "102", "103");

        // schedule a new match
        assertResponse("http://football-match:18081/command/matches",
            POST, "{\"id\":\"m1\", \"seasonId\":\"s1\", \"date\":\"2018-05-26T15:00:00\"," +
                "\"homeClubId\":\"Man Utd\", \"awayClubId\":\"Liverpool\"}", CREATED);

        // the request is processed asynchronously, so wait for the right event before the next step
        assertEvents(MatchScheduled.class, "m1");

        // change match status from SCHEDULED to STARTED
        assertResponse("http://football-match:18081/command/matches/m1",
            PATCH, "\"STARTED\"", NO_CONTENT, NOT_FOUND);

        assertEvents(MatchStarted.class, "m1");

        // some goals and cards during the match
        assertResponse("http://football-match:18081/command/matches/m1/homeGoals",
            POST, "{\"id\":\"g1\", \"minute\":20, \"scorerId\":\"101\"}", CREATED, UNPROCESSABLE_ENTITY);
        assertResponse("http://football-match:18081/command/matches/m1/awayGoals",
            POST, "{\"id\":\"g2\", \"minute\":30, \"scorerId\":\"102\"}", CREATED);
        assertResponse("http://football-match:18081/command/matches/m1/cards",
            POST, "{\"id\":\"c1\", \"minute\":40, \"receiverId\":\"102\", \"type\":\"YELLOW\"}", CREATED);
        assertResponse("http://football-match:18081/command/matches/m1/cards",
            POST, "{\"id\":\"c1\", \"minute\":40, \"receiverId\":\"103\", \"type\":\"RED\"}", CREATED);
        assertResponse("http://football-match:18081/command/matches/m1/homeGoals",
            POST, "{\"id\":\"g3\", \"minute\":50, \"scorerId\":\"101\"}", CREATED);

        assertEvents(GoalScored.class, "m1", "m1", "m1");
        assertEvents(CardReceived.class, "m1", "m1");

        // finish match
        assertResponse("http://football-match:18081/command/matches/m1",
            PATCH, "\"FINISHED\"", NO_CONTENT);

        assertEvents(MatchFinished.class, "m1");

        // check the score
        MatchScore[] scores = get("http://football-ui:18080/ui/matchScores", MatchScore[].class, 1);
        assertThat(scores[0].getHomeClubId()).isEqualTo("Man Utd");
        assertThat(scores[0].getAwayClubId()).isEqualTo("Liverpool");
        assertThat(scores[0].getHomeGoals()).isEqualTo(2);
        assertThat(scores[0].getAwayGoals()).isEqualTo(1);

//        // check players
//        PlayerStatistic[] players = get("http://football-ui:18080/ui/players", PlayerStatistic[].class, 3);
//        assertThat(players[0].getPlayerName()).isEqualTo("Player One");
//        assertThat(players[0].getGoals()).isEqualTo("2");
//        assertThat(players[0].getYellowCards()).isEqualTo(2);
//        assertThat(players[0].getRedCards()).isEqualTo(1);
    }

    @Test
    public void startNonExistentMatch() {
        assertResponse("http://football-match:18081/command/matches/FAKE_MATCH",
            PATCH, "\"STARTED\"", NOT_FOUND);
    }

    @Test
    public void scoreGoalInNonExistentMatch() {
        assertResponse("http://football-match:18081/command/matches/FAKE_MATCH",
            PATCH, "\"STARTED\"", NOT_FOUND);
    }

    @Test
    public void scoreGoalInNotStartedMatch() {
        assertResponse("http://football-match:18081/command/matches",
            POST, "{\"id\":\"NOT_STARTED_MATCH\", \"seasonId\":\"s1\", \"date\":\"2018-05-26T15:00:00\"," +
                "\"homeClubId\":\"Man City\", \"awayClubId\":\"Chelsea\"}", CREATED);

        assertEvents(MatchScheduled.class, "NOT_STARTED_MATCH");

        assertResponse("http://football-match:18081/command/matches/NOT_STARTED_MATCH/homeGoals",
            POST, "{\"id\":\"g1000\", \"minute\":10, \"scorerId\":\"101\"}", UNPROCESSABLE_ENTITY);
    }

    private static void assertResponse(String url, HttpMethod method, String json, HttpStatus expectedStatus) {
        assertResponse(url, method, json, expectedStatus, null);
    }

    private static void assertResponse(String url, HttpMethod method, String json, HttpStatus expectedStatus,
            HttpStatus retryStatus) {
        HttpStatus currentStatus;
        long timeout = System.currentTimeMillis() + REST_RETRY_TIMEOUT;

        try {
            do {
                currentStatus = send(url, method, json);

                if (currentStatus.equals(expectedStatus)) {
                    return;
                }
                if (currentStatus.equals(retryStatus)) {
                    logger.trace("Retry status received, trying again...");
                    Thread.sleep(500);
                    return;
                }
                Assert.fail("Expected response: " + expectedStatus + ", actual: " + currentStatus);
            } while (System.currentTimeMillis() > timeout);

            Assert.fail("Expected response: " + expectedStatus + ", actual: " + currentStatus);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
        }
    }

    private static HttpStatus send(String url, HttpMethod method, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        logger.trace(json);

        try {
            return rest.exchange(url, method, new HttpEntity<>(json, headers), String.class).getStatusCode();
        } catch (HttpClientErrorException e) {
            return e.getStatusCode();
        }
    }

    private static <T> T get(String url, Class<T> responseType, int expectedResultCount) throws IOException {
        long timeout = System.currentTimeMillis() + REST_RETRY_TIMEOUT;
        int resultSize = -1;

        try {
            do {
                ResponseEntity<String> response = rest.getForEntity(url, String.class);
                logger.trace(response.getBody());
                T result = new ObjectMapper().readerFor(responseType).readValue(response.getBody());
                int responseSize = ((Object[]) result).length;

                if (responseSize == expectedResultCount) {
                    return result;
                }
                logger.trace(responseSize + " items received, trying again...");
                Thread.sleep(500);
            } while (System.currentTimeMillis() > timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
        }
        throw new AssertionError("Expected items: " + expectedResultCount + ", actual: " + resultSize);
    }

    private static <T extends Event> void assertEvents(Class<T> type, String... eventIds) {
        long timeout = System.currentTimeMillis() + EVENT_TIMEOUT;

        Collection<String> expected = new ArrayList<>(Arrays.asList(eventIds));
        Collection<String> found = new ArrayList<>();
        Collection<String> redundant = new ArrayList<>();

        String topic = Topics.eventTopicName(type);
        eventConsumer.subscribe(Collections.singletonList(topic));
        long poolTimeout = EVENT_TIMEOUT;

        do {
            ConsumerRecords<String, String> records = eventConsumer.poll(poolTimeout);

            for (ConsumerRecord<String, String> record : records) {
                if (expected.remove(record.key())) {
                    found.add(record.key());
                } else {
                    redundant.add(record.key());
                }
            }
            if (expected.isEmpty()) {
                logger.debug("The expected events have been received: " + Arrays.toString(eventIds));
                break;
            }
            poolTimeout = timeout - System.currentTimeMillis();
        }
        while (poolTimeout > 0);

        if (!redundant.isEmpty()) {
            logger.warn("Some redundant events have been found in topic " + topic + ": " + expected);
        }
        if (!expected.isEmpty()) {
            Assert.fail("The expected events in topic " + topic + " should be: " + expected + ", but found: " + found);
        }
    }

    private static void createConnector(String connectorRestApiUrl, String request) {
        String json;

        try {
            json = StreamUtils.copyToString(
                BasicIntegrationTest.class.getResourceAsStream(request), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HttpStatus status = send(connectorRestApiUrl, POST, json);

        if (status != CREATED) {
            if (status != CONFLICT) {
                throw new RuntimeException("Unable to create Kafka connector, HTTP status: " + status);
            }
            logger.warn("Connector already exists");
        }
    }
}
