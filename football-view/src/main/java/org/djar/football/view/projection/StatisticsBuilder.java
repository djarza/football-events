package org.djar.football.view.projection;

import static org.apache.kafka.streams.KeyValue.pair;

import java.util.Collection;
import java.util.LinkedList;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.djar.football.Topics;
import org.djar.football.event.CardReceived;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchStarted;
import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.model.MatchScore;
import org.djar.football.model.PlayerStatistic;
import org.djar.football.model.TeamRanking;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.stream.StreamsUtils;

public class StatisticsBuilder {

    private static final String MATCH_STARTED_TOPIC = Topics.eventTopicName(MatchStarted.class);
    private static final String GOAL_SCORED_TOPIC = Topics.eventTopicName(GoalScored.class);
    private static final String MATCH_FINISHED_TOPIC = Topics.eventTopicName(MatchFinished.class);
    private static final String PLAYER_STARTED_TOPIC = Topics.eventTopicName(PlayerStartedCareer.class);
    private static final String CARD_RECEIVED_TOPIC = Topics.eventTopicName(CardReceived.class);

    public static final String MATCH_SCORES_STORE = "match_scores_store";
    public static final String RANKING_STORE = "team_ranking_store";
    public static final String PLAYER_STATISTIC_STORE = "player_statistic_store";

    public static final String TEAM_RANKING_TOPIC = Topics.viewTopicName(TeamRanking.class);
    public static final String MATCH_SCORES_TOPIC = Topics.viewTopicName(MatchScore.class);
    public static final String PLAYER_STATISTIC_TOPIC = Topics.viewTopicName(PlayerStatistic.class);

    private final JsonPojoSerde<MatchStarted> matchStartedSerde = new JsonPojoSerde<>(MatchStarted.class);
    private final JsonPojoSerde<MatchFinished> matchFinishedSerde = new JsonPojoSerde<>(MatchFinished.class);
    private final JsonPojoSerde<GoalScored> goalScoredSerde = new JsonPojoSerde<>(GoalScored.class);
    private final JsonPojoSerde<CardReceived> cardReceivedSerde = new JsonPojoSerde<>(CardReceived.class);
    private final JsonPojoSerde<PlayerStartedCareer> playerSerde = new JsonPojoSerde<>(PlayerStartedCareer.class);
    private final JsonPojoSerde<MatchScore> matchScoreSerde = new JsonPojoSerde<>(MatchScore.class);
    private final JsonPojoSerde<TeamRanking> rankingSerde = new JsonPojoSerde<>(TeamRanking.class);
    private final JsonPojoSerde<PlayerStatistic> statsSerde = new JsonPojoSerde<>(PlayerStatistic.class);

    private final StreamsBuilder builder;

    private long maxMatchDuration = (
            /* standard time */
            (45 + 15 + 45)
            /* additional time */
            + 10
            /* extra time */
            + (15 + 5 + 15)
            /* penalty shoot-out */
            + 30
            ) * 60 * 1000; // ms

    public StatisticsBuilder(StreamsBuilder builder) {
        this.builder = builder;
    }

    public long getMaxMatchDuration() {
        return maxMatchDuration;
    }

    public void setMaxMatchDuration(long maxMatchDuration) {
        this.maxMatchDuration = maxMatchDuration;
    }

    public void build() {
        KStream<String, GoalScored> goalStream = builder
                .stream(GOAL_SCORED_TOPIC, Consumed.with(Serdes.String(), goalScoredSerde));

        buildMatchStatistics(goalStream);
        buildPlayerStatistics(goalStream);
    }

    private void buildMatchStatistics(KStream<String, GoalScored> goalStream) {
        KStream<String, MatchStarted> matchStartedStream = builder
                .stream(MATCH_STARTED_TOPIC, Consumed.with(Serdes.String(), matchStartedSerde));

        KStream<String, MatchFinished> matchFinishedStream = builder
                .stream(MATCH_FINISHED_TOPIC, Consumed.with(Serdes.String(), matchFinishedSerde));

        KStream<String, MatchScore> scoreStream = matchStartedStream
                .leftJoin(goalStream, (match, goal) -> new MatchScore(match).goal(goal),
                    JoinWindows.of(maxMatchDuration), Joined.with(Serdes.String(), matchStartedSerde,
                        goalScoredSerde)
        );

        KTable<String, MatchScore> scoreTable = scoreStream
                .groupByKey()
                .reduce(MatchScore::aggregate, StreamsUtils.materialized(MATCH_SCORES_STORE, matchScoreSerde));
        scoreTable.toStream().to(MATCH_SCORES_TOPIC, Produced.with(Serdes.String(), matchScoreSerde));

        KStream<String, MatchScore> finalScoreStream = matchFinishedStream
                .leftJoin(scoreTable, (matchFinished, matchScore) -> matchScore,
                    Joined.with(Serdes.String(), matchFinishedSerde, matchScoreSerde)
        );

        // new key: clubId
        KStream<String, TeamRanking> rankingStream = finalScoreStream
                .flatMap((clubId, matchScore) -> {
                    Collection<KeyValue<String, TeamRanking>> result = new LinkedList<>();
                    result.add(pair(matchScore.getHomeClubId(), matchScore.homeRanking()));
                    result.add(pair(matchScore.getAwayClubId(), matchScore.awayRanking()));
                    return result;
                });

        KTable<String, TeamRanking> rankingTable = rankingStream
                .groupByKey(Serialized.with(Serdes.String(), rankingSerde))
                .reduce(TeamRanking::aggregate, StreamsUtils.materialized(RANKING_STORE, rankingSerde));
        rankingTable.toStream().to(TEAM_RANKING_TOPIC, Produced.with(Serdes.String(), rankingSerde));
    }

    private void buildPlayerStatistics(KStream<String, GoalScored> goalStream) {
        KStream<String, CardReceived> cardStream = builder
                .stream(CARD_RECEIVED_TOPIC, Consumed.with(Serdes.String(), cardReceivedSerde))
                .selectKey((matchId, card) -> card.getReceiverId());

        KTable<String, PlayerStartedCareer> playerTable = builder
                .table(PLAYER_STARTED_TOPIC, Consumed.with(Serdes.String(), playerSerde));

        KStream<String, GoalScored> playerGoalStream = goalStream
                .selectKey((matchId, goal) -> goal.getScorerId());

        KTable<String, PlayerStatistic> goalPlayerTable = playerGoalStream
                .leftJoin(playerTable, (goal, player) -> new PlayerStatistic(player).goal(goal),
                    Joined.with(Serdes.String(), goalScoredSerde, playerSerde))
                .groupByKey(Serialized.with(Serdes.String(), statsSerde))
                .reduce(PlayerStatistic::aggregate);

        KTable<String, PlayerStatistic> cardPlayerTable = cardStream
                .leftJoin(playerTable, (card, player) -> new PlayerStatistic(player).card(card),
                    Joined.with(Serdes.String(), cardReceivedSerde, playerSerde))
                .groupByKey(Serialized.with(Serdes.String(), statsSerde))
                .reduce(PlayerStatistic::aggregate);

        KTable<String, PlayerStatistic> statTable = goalPlayerTable
                .outerJoin(cardPlayerTable, (stat1, stat2) -> PlayerStatistic.join(stat1, stat2),
                    StreamsUtils.materialized(PLAYER_STATISTIC_STORE, statsSerde));

        statTable.toStream().to(PLAYER_STATISTIC_TOPIC, Produced.with(Serdes.String(), statsSerde));
    }
}
