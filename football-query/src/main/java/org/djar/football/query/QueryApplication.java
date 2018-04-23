package org.djar.football.query;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.djar.football.query.projection.MatchStatisticsBuilder;
import org.djar.football.stream.KafkaStreamsStarter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QueryApplication {

    @Value("${kafka.bootstrapAddress}")
    private String kafkaBootstrapAddress;

    @Bean
    public KafkaStreams kafkaStreams() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        MatchStatisticsBuilder statisticsBuilder = new MatchStatisticsBuilder();
        statisticsBuilder.build(streamsBuilder);
        Topology topology = streamsBuilder.build();
        return KafkaStreamsStarter.start(kafkaBootstrapAddress, topology, QueryApplication.class.getSimpleName());
    }

    public static void main(String[] args) {
        SpringApplication.run(QueryApplication.class, args);
    }
}
