package org.djar.football.stream;

import static org.djar.football.Events.TOPIC_NAME_PREFIX;

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

    private static final int KAFKA_CONNECT_RETRY_DELAY = 2000;
    private static final int KAFKA_CONNECTION_TIMEOUT = 20000;
    private static final int STREAMS_STARTUP_TIMEOUT = 20000;

    // the number of football topics - docker-compose.yml/kafka/KAFKA_CREATE_TOPICS
    private static final int FB_TOPIC_COUNT = 7;

    private KafkaStreamsStarter() {
    }

    public static KafkaStreams start(String kafkaBootstrapAddress, Topology topology, String applicationId) {
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
        waitForKafkaAndTopics(kafkaBootstrapAddress);
        startStreams(kafkaStreams);

        logger.debug("Started Kafka Streams, Kafka bootstrap: {}", kafkaBootstrapAddress);
        return kafkaStreams;
    }

    private static void waitForKafkaAndTopics(String kafkaBootstrapAddress) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, KAFKA_CONNECTION_TIMEOUT);
        Set<String> topics;

        try (AdminClient client = KafkaAdminClient.create(properties)) {
            while (true) {
                try {
                    topics = client.listTopics().names().get();

                    // wait until all football topics are created
                    // these topics are created at Kafka startup
                    if (topics.stream().filter(name -> name.startsWith(TOPIC_NAME_PREFIX)).count() == FB_TOPIC_COUNT) {
                        break;
                    }
                } catch (ExecutionException e) {
                    // ignore retriable errors, especially timeouts
                    if (!(e.getCause() instanceof RetriableException)) {
                        throw e;
                    }
                    logger.trace("Trying to connect to Kafka {}", e);
                }
                Thread.sleep(KAFKA_CONNECT_RETRY_DELAY);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Kafka connection error " + kafkaBootstrapAddress, e);
        }
        logger.trace("Required topics exist: {}", topics);
    }

    private static void startStreams(KafkaStreams kafkaStreams) {
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
        long timeout = System.currentTimeMillis() + STREAMS_STARTUP_TIMEOUT;

        try {
            streamsStartedLatch.await(timeout - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
        }

        KafkaStreams.State state = kafkaStreams.state();

        if (state != KafkaStreams.State.RUNNING) {
            logger.error("Unable to start Kafka Streams in {} s, the current state is {}",
                    STREAMS_STARTUP_TIMEOUT, state);
            System.exit(1);
        }
    }
}
