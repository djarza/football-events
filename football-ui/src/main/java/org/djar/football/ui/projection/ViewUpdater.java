package org.djar.football.ui.projection;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.StreamsBuilder;
import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerStatistic;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.stream.StreamsUtils;
import org.djar.football.util.Topics;

public class ViewUpdater {

    public static final String MATCH_SCORES_STORE = "match_scores_store";
    public static final String TEAM_RANKING_STORE = "team_ranking_store";
    public static final String PLAYER_STATISTIC_STORE = "player_statistic_store";

    public void build(StreamsBuilder builder) {
        createStateStore(builder, MatchScore.class, MATCH_SCORES_STORE);
        createStateStore(builder, TeamRanking.class, TEAM_RANKING_STORE);
        createStateStore(builder, PlayerStatistic.class, PLAYER_STATISTIC_STORE);
    }

    private void createStateStore(StreamsBuilder builder, Class viewType, String store) {
        JsonPojoSerde serde = new JsonPojoSerde<>(viewType);
        builder.table(Topics.viewTopicName(viewType), Consumed.with(Serdes.String(), serde),
                StreamsUtils.materialized(store, serde));
    }
}
