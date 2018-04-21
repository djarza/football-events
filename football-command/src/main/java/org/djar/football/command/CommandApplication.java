package org.djar.football.command;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.djar.football.command.eventstore.EventHandler;
import org.djar.football.command.eventstore.EventPublisher;
import org.djar.football.event.Event;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.stream.KafkaStreamsStarter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CommandApplication {

    @Value("${kafka.bootstrapAddress}")
    private String kafkaBootstrapAddress;

    @Bean
    public KafkaStreams kafkaStreams() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        EventHandler eventHandler = new EventHandler();
        Topology topology = streamsBuilder.build();
        eventHandler.init(topology);
        return KafkaStreamsStarter.start(kafkaBootstrapAddress, topology, CommandApplication.class.getSimpleName());
    }

    @Bean
    public EventPublisher eventPublisher() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapAddress);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonPojoSerde.class.getName());
        KafkaProducer<String, Event> kafkaProducer = new KafkaProducer<>(producerProps);
        return new EventPublisher(kafkaProducer);
    }

    public static void main(String[] args) {
        SpringApplication.run(CommandApplication.class, args);
    }
}
