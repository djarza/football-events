package org.djar.football.ui;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.StreamsBuilder;
import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerCards;
import org.djar.football.model.view.PlayerGoals;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.model.view.TopPlayers;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.stream.StreamsUtils;
import org.djar.football.util.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class StatisticsKeeper {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsKeeper.class);

    public static final String MATCH_SCORES_STORE = "match_scores_store";
    public static final String TEAM_RANKING_STORE = "team_ranking_store";
    public static final String PLAYER_GOALS_STORE = "player_goals_store";
    public static final String PLAYER_CARDS_STORE = "player_cards_store";
    public static final String TOP_PLAYERS_STORE = "top_players_store";

    private final StreamsBuilder streamsBuilder;
    private final SimpMessagingTemplate stomp;
    private final Executor executor = Executors.newCachedThreadPool();

    public StatisticsKeeper(StreamsBuilder streamsBuilder, SimpMessagingTemplate stomp) {
        this.streamsBuilder = streamsBuilder;
        this.stomp = stomp;
    }

    public void build() {
        updateStoreAndDashboard(MatchScore.class, MATCH_SCORES_STORE);
        updateStoreAndDashboard(TeamRanking.class, TEAM_RANKING_STORE);
        updateStoreAndDashboard(PlayerGoals.class, PLAYER_GOALS_STORE);
        updateStoreAndDashboard(PlayerCards.class, PLAYER_CARDS_STORE);
        updateStoreAndDashboard(TopPlayers.class, TOP_PLAYERS_STORE);
    }

    private <T> void updateStoreAndDashboard(Class<T> viewType, String store) {
        JsonPojoSerde serde = new JsonPojoSerde<>(viewType);
        streamsBuilder.stream(Topics.viewTopicName(viewType), Consumed.with(Serdes.String(), serde))
                .peek(this::updateDashboard)
                .groupByKey()
                .reduce((aggValue, newValue) -> newValue, StreamsUtils.materialized(store, serde));
    }

    private void updateDashboard(Object key, Object value) {
        // emit WebSocket notification
        executor.execute(() -> {
            logger.debug("Update dashboard {}: {}->{}", value.getClass().getSimpleName(), key, value);
            stomp.convertAndSend("/topic/" + value.getClass().getSimpleName(), value);
        });
    }
}
