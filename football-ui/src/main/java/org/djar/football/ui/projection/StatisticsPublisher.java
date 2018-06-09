package org.djar.football.ui.projection;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerStatistic;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.stream.StreamsUtils;
import org.djar.football.util.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class StatisticsPublisher {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsPublisher.class);

    public static final String MATCH_SCORES_STORE = "match_scores_store";
    public static final String TEAM_RANKING_STORE = "team_ranking_store";
    public static final String PLAYER_STATISTIC_STORE = "player_statistic_store";

    private final StreamsBuilder streamsBuilder;
    private final SimpMessagingTemplate stomp;
    private final Executor executor = Executors.newCachedThreadPool();

    public StatisticsPublisher(StreamsBuilder streamsBuilder, SimpMessagingTemplate stomp) {
        this.streamsBuilder = streamsBuilder;
        this.stomp = stomp;
    }

    public void build() {
        storeAndUpdateDashboard(streamsBuilder, MatchScore.class, MATCH_SCORES_STORE, this::updateDashboard);
        storeAndUpdateDashboard(streamsBuilder, TeamRanking.class, TEAM_RANKING_STORE, this::updateDashboard);
        storeAndUpdateDashboard(streamsBuilder, PlayerStatistic.class, PLAYER_STATISTIC_STORE, this::updateDashboard);
    }

    private <T> void storeAndUpdateDashboard(StreamsBuilder builder, Class<T> viewType, String store,
            ForeachAction<String, T> action) {
        JsonPojoSerde serde = new JsonPojoSerde<>(viewType);
        builder.stream(Topics.viewTopicName(viewType), Consumed.with(Serdes.String(), serde))
                .peek(action)
                .groupByKey()
                .reduce((aggValue, newValue) -> newValue, StreamsUtils.materialized(store, serde));
    }

    private <T> void updateDashboard(String key, T value) {
        executor.execute(() -> {
            logger.debug("Update dashboard: {}->{}", key, value);
            stomp.convertAndSend("/topic/" + value.getClass().getSimpleName(), value);
        });
    }
}
