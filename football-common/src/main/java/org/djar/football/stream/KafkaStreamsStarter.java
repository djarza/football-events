package org.djar.football.stream;

import static org.djar.football.util.Topics.TOPIC_NAME_PREFIX;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaStreamsStarter {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsStarter.class);

    // the number of football topics - docker-compose.yml/kafka/KAFKA_CREATE_TOPICS
    private static final int FB_TOPIC_COUNT = 10;

    private final String kafkaBootstrapAddress;
    private final Topology topology;
    private final String applicationId;

    private long kafkaTimeout = 120000;
    private long streamsStartupTimeout = 20000;

    public KafkaStreamsStarter(String kafkaBootstrapAddress, Topology topology, String applicationId) {
        this.kafkaBootstrapAddress = kafkaBootstrapAddress;
        this.topology = topology;
        this.applicationId = applicationId;
    }

    public void setKafkaTimeout(long kafkaTimeout) {
        this.kafkaTimeout = kafkaTimeout;
    }

    public void setStreamsStartupTimeout(long streamsStartupTimeout) {
        this.streamsStartupTimeout = streamsStartupTimeout;
    }

    public KafkaStreams start() {
        Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, applicationId);
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 0);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1); //commit asap

        final KafkaStreams kafkaStreams = new KafkaStreams(topology, props);

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
        kafkaStreams.setUncaughtExceptionHandler((thread, exception) -> logger.error(thread.toString(), exception));

        // wait for Kafka and football topics creation to avoid endless REBALANCING problem
        waitForKafkaAndTopics();
        startStreams(kafkaStreams);

        logger.debug("Started Kafka Streams, Kafka bootstrap: {}", kafkaBootstrapAddress);
        return kafkaStreams;
    }

    private void waitForKafkaAndTopics() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);

        long timeout = System.currentTimeMillis() + kafkaTimeout;

        // football topics are created at Kafka startup
        // wait until all of them are created
        try (AdminClient client = KafkaAdminClient.create(properties)) {
            while (true) {
                Set<String> topicNames = null;

                try {
                    topicNames = client.listTopics().names().get();

                    if (containsFootballTopics(topicNames)) {
                        logger.trace("Required topics exist: {}", topicNames);
                        break;
                    }
                } catch (ExecutionException e) {
                    // ignore retriable errors, especially timeouts
                    if (!(e.getCause() instanceof RetriableException)) {
                        throw new RuntimeException("Kafka connection error " + kafkaBootstrapAddress, e);
                    }
                    logger.trace("Trying to connect to Kafka {}", e.getMessage());
                }
                checkTimeout(kafkaBootstrapAddress, timeout, topicNames);
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
        }
    }

    private void checkTimeout(String kafkaBootstrapAddress, long timeout, Set<String> topicNames) {
        if (System.currentTimeMillis() > timeout) {
            if (topicNames == null || topicNames.isEmpty()) {
                throw new RuntimeException("Timeout waiting for Kafka. Kafka is not available "
                        + kafkaBootstrapAddress);
            }
            throw new RuntimeException("Timeout waiting for Kafka. "
                    + "Some '" + TOPIC_NAME_PREFIX + "*' topics are missing, found only: " + topicNames);
        }
    }

    private boolean containsFootballTopics(Set<String> topicNames) {
        return topicNames.stream().filter(name -> name.startsWith(TOPIC_NAME_PREFIX)).count() == FB_TOPIC_COUNT;
    }

    private void startStreams(KafkaStreams kafkaStreams) {
        CountDownLatch streamsStartedLatch = new CountDownLatch(1);

        // wait for consistent state
        kafkaStreams.setStateListener((newState, oldState) -> {
            logger.trace("Kafka Streams state has been changed from {} to {}", oldState, newState);

            if (oldState == KafkaStreams.State.REBALANCING && newState == KafkaStreams.State.RUNNING) {
                streamsStartedLatch.countDown();
            }
        });
        kafkaStreams.cleanUp();
        kafkaStreams.start();
        long timeout = System.currentTimeMillis() + streamsStartupTimeout;

        try {
            streamsStartedLatch.await(timeout - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
        }

        KafkaStreams.State state = kafkaStreams.state();

        if (state != KafkaStreams.State.RUNNING) {
            logger.error("Unable to start Kafka Streams in {} ms, the current state is {}",
                    streamsStartupTimeout, state);
            System.exit(1);
        }
    }
}
