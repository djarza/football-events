package org.djar.football.player;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.djar.football.event.Event;
import org.djar.football.player.snapshot.SnapshotBuilder;
import org.djar.football.stream.EventPublisher;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.stream.KafkaStreamsStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PlayerApplication {

    private static final Logger logger = LoggerFactory.getLogger(PlayerApplication.class);
    private static final String PROCESS_ID = processId();

    @Value("${kafka.bootstrapAddress}")
    private String kafkaBootstrapAddress;

    @Value("${apiVersion}")
    private int apiVersion;

    @Bean
    public KafkaStreams kafkaStreams() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        SnapshotBuilder snapshotBuilder = new SnapshotBuilder();
        Topology topology = streamsBuilder.build();
        snapshotBuilder.init(topology);
        return KafkaStreamsStarter.start(kafkaBootstrapAddress, topology, getClass().getSimpleName());
    }

    @Bean
    public EventPublisher eventPublisher() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonPojoSerde.class.getName());
        KafkaProducer<String, Event> kafkaProducer = new KafkaProducer<>(producerProps);
        return new EventPublisher(kafkaProducer, getClass().getSimpleName(), apiVersion);
    }

    private static String processId() {
        // a makeshift; should be a microservice's instance identifier like IP or Kubernetes POD name
        return PlayerApplication.class.getSimpleName() + new ApplicationPid().toString();
    }

    public static void main(String[] args) {
        logger.info("Process ID: {}", PROCESS_ID);
        SpringApplication.run(PlayerApplication.class, args);
    }
}
