package org.djar.football.query.projection;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.djar.football.Topics;
import org.djar.football.event.CardReceived;
import org.djar.football.event.GoalScored;
import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.model.PlayerStatistic;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.stream.StreamsUtils;

public class PlayerStatisticsBuilder {

    private static final String PLAYER_STARTED_TOPIC = Topics.eventTopicName(PlayerStartedCareer.class);
    private static final String GOAL_SCORED_TOPIC = Topics.eventTopicName(GoalScored.class);
    private static final String CARD_RECEIVED_TOPIC = Topics.eventTopicName(CardReceived.class);

    public static final String PLAYER_STATISTIC_STORE = "player_statistic_store";

    public static final String PLAYER_STATISTIC_TOPIC = Topics.viewTopicName(PlayerStatistic.class);

    private final JsonPojoSerde<PlayerStartedCareer> playerSerde = new JsonPojoSerde<>(PlayerStartedCareer.class);
    private final JsonPojoSerde<PlayerStatistic> statsSerde = new JsonPojoSerde<>(PlayerStatistic.class);
    private final JsonPojoSerde<GoalScored> goalSerde = new JsonPojoSerde<>(GoalScored.class);
    private final JsonPojoSerde<CardReceived> cardSerde = new JsonPojoSerde<>(CardReceived.class);

    public void build(StreamsBuilder builder) {
        KTable<String, PlayerStartedCareer> playerTable = builder
                .table(PLAYER_STARTED_TOPIC, Consumed.with(Serdes.String(), playerSerde));

        KStream<String, GoalScored> goalStream = builder
                .stream(GOAL_SCORED_TOPIC, Consumed.with(Serdes.String(), goalSerde))
                .selectKey((matchId, goal) -> goal.getScorerId());

        KStream<String, CardReceived> cardStream = builder
                .stream(CARD_RECEIVED_TOPIC, Consumed.with(Serdes.String(), cardSerde))
                .selectKey((matchId, card) -> card.getReceiverId());

        KTable<String, PlayerStatistic> goalPlayerTable = goalStream
                .leftJoin(playerTable, (goal, player) -> new PlayerStatistic(player).goal(goal),
                    Joined.with(Serdes.String(), goalSerde, playerSerde))
                .groupByKey(Serialized.with(Serdes.String(), statsSerde))
                .reduce(PlayerStatistic::aggregate);

        KTable<String, PlayerStatistic> cardPlayerTable = cardStream
                .leftJoin(playerTable, (card, player) -> new PlayerStatistic(player).card(card),
                    Joined.with(Serdes.String(), cardSerde, playerSerde))
                .groupByKey(Serialized.with(Serdes.String(), statsSerde))
                .reduce(PlayerStatistic::aggregate);

        KTable<String, PlayerStatistic> statTable = goalPlayerTable
                .outerJoin(cardPlayerTable, (stat1, stat2) -> PlayerStatistic.join(stat1, stat2),
                    StreamsUtils.materialized(PLAYER_STATISTIC_STORE, statsSerde));
        statTable.toStream().to(PLAYER_STATISTIC_TOPIC, Produced.with(Serdes.String(), statsSerde));
    }
}
