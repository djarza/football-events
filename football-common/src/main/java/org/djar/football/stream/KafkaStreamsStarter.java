package org.djar.football.stream;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaStreamsStarter {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamsStarter.class);

    private static final int CONNECT_RETRY_DELAY = 2000;

    private KafkaStreamsStarter() {
    }

    public static KafkaStreams start(String kafkaBootstrapAddress, Topology topology, String applicationId) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 0);

        final KafkaStreams kafkaStreams = new KafkaStreams(topology, props);

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
        kafkaStreams.setUncaughtExceptionHandler((thread, exception) -> logger.error(thread.toString(), exception));

        // wait for Kafka to avoid endless REBALANCING problem
        waitForKafka(kafkaBootstrapAddress);
        startStreams(kafkaStreams);

        logger.debug("Started Kafka Streams, Kafka bootstrap: {}", kafkaBootstrapAddress);
        return kafkaStreams;
    }

    private static void waitForKafka(String kafkaBootstrapAddress) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);
        properties.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 5000);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

        try (AdminClient client = KafkaAdminClient.create(properties)) {
            while (true) {
                ListTopicsResult topics = client.listTopics();
                Set<String> names = topics.names().get();

                // wait until football topics are created
                if (names.stream().anyMatch(name -> name.startsWith("fb."))) {
                    break;
                }
                Thread.sleep(CONNECT_RETRY_DELAY);
            }
        } catch (Exception e) {
            throw new RuntimeException("Kafka connection error " + kafkaBootstrapAddress, e);
        }
        logger.trace("Connected to Kafka {}", kafkaBootstrapAddress);
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

        try {
            streamsStartedLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        KafkaStreams.State state = kafkaStreams.state();

        if (state != KafkaStreams.State.RUNNING) {
            logger.error("Unable to start Kafka Streams, the current state is {}", state);
            System.exit(1);
        }
    }
}
