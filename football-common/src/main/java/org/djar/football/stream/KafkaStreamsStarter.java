package org.djar.football.stream;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

public class KafkaStreamsStarter {

    private KafkaStreamsStarter() {
    }

    public static KafkaStreams start(String kafkaBootstrapAddress, Topology topology, String applicationId) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);

        final KafkaStreams kafkaStreams = new KafkaStreams(topology, props);

        Runtime.getRuntime().addShutdownHook(new Thread(applicationId + "-shutdown-hook") {
            @Override
            public void run() {
                System.out.println("shutting down...");
                kafkaStreams.close();
            }
        });
        CountDownLatch streamsStartedLatch = new CountDownLatch(1);

        // to avoid InvalidStateStoreException from KafkaStreams.store()
        kafkaStreams.setStateListener((newState, oldState) -> {
            System.out.println(oldState + " -> " + newState);
            if (oldState == KafkaStreams.State.REBALANCING && newState == KafkaStreams.State.RUNNING) {
                streamsStartedLatch.countDown();
            }
        });
        kafkaStreams.start();

        try {
            streamsStartedLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("kafka streams started!!!");
        return kafkaStreams;
    }
}
