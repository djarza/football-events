package org.djar.football.ui;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.djar.football.model.MatchScore;
import org.djar.football.model.PlayerStatistic;
import org.djar.football.model.TeamRanking;
import org.djar.football.repo.StateStoreRepository;
import org.djar.football.stream.KafkaStreamsStarter;
import org.djar.football.ui.projection.ViewUpdater;
import org.djar.football.util.MicroserviceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UiApplication {

    private static final Logger logger = LoggerFactory.getLogger(UiApplication.class);

    private static final String APP_ID = MicroserviceUtils.applicationId(UiApplication.class);

    @Value("${kafka.bootstrapAddress}")
    private String kafkaBootstrapAddress;

    @Value("${kafkaTimeout:60000}")
    private long kafkaTimeout;

    @Value("${streamsStartupTimeout:20000}")
    private long streamsStartupTimeout;

    @Bean
    public KafkaStreams kafkaStreams() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        ViewUpdater statisticsBuilder = new ViewUpdater();
        statisticsBuilder.build(streamsBuilder);
        Topology topology = streamsBuilder.build();
        KafkaStreamsStarter starter = new KafkaStreamsStarter(kafkaBootstrapAddress, topology, APP_ID);
        starter.setKafkaTimeout(kafkaTimeout);
        starter.setStreamsStartupTimeout(streamsStartupTimeout);
        return starter.start();
    }

    @Bean
    public StateStoreRepository<MatchScore> matchScoresRepo() {
        return new StateStoreRepository<>(kafkaStreams(), ViewUpdater.MATCH_SCORES_STORE);
    }

    @Bean
    public StateStoreRepository<TeamRanking> teamRankingRepo() {
        return new StateStoreRepository<>(kafkaStreams(), ViewUpdater.TEAM_RANKING_STORE);
    }

    @Bean
    public StateStoreRepository<PlayerStatistic> playerStatisticRepo() {
        return new StateStoreRepository<>(kafkaStreams(), ViewUpdater.PLAYER_STATISTIC_STORE);
    }

    public static void main(String[] args) {
        logger.info("Application ID: {}", APP_ID);
        SpringApplication.run(UiApplication.class, args);
    }
}
