package org.djar.football.view;

import static org.apache.kafka.common.serialization.Serdes.String;
import static org.apache.kafka.streams.Consumed.with;
import static org.apache.kafka.streams.KeyValue.pair;
import static org.apache.kafka.streams.kstream.Joined.with;
import static org.djar.football.stream.StreamsUtils.materialized;

import java.util.Collection;
import java.util.LinkedList;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.MatchFinished;
import org.djar.football.model.event.MatchStarted;
import org.djar.football.model.event.PlayerStartedCareer;
import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerCards;
import org.djar.football.model.view.PlayerGoals;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.model.view.TopPlayers;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.util.Topics;

/**
 * Builder that creates Kafka Streams topology for creating simple statistics: match scores, teams ranking
 * and player statistics with number of goals and yellow/red cards.
 */
public class StatisticsBuilder {

    private static final String MATCH_STARTED_TOPIC = Topics.eventTopicName(MatchStarted.class);
    private static final String GOAL_SCORED_TOPIC = Topics.eventTopicName(GoalScored.class);
    private static final String MATCH_FINISHED_TOPIC = Topics.eventTopicName(MatchFinished.class);
    private static final String PLAYER_STARTED_TOPIC = Topics.eventTopicName(PlayerStartedCareer.class);
    private static final String CARD_RECEIVED_TOPIC = Topics.eventTopicName(CardReceived.class);

    public static final String MATCH_SCORES_STORE = "match_scores_store";
    public static final String TEAM_RANKING_STORE = "team_ranking_store";
    public static final String PLAYER_GOALS_STORE = "player_goals_store";
    public static final String PLAYER_CARDS_STORE = "player_cards_store";
    public static final String TOP_SCORERS_STORE = "top_scorers_store";

    public static final String TEAM_RANKING_TOPIC = Topics.viewTopicName(TeamRanking.class);
    public static final String MATCH_SCORES_TOPIC = Topics.viewTopicName(MatchScore.class);
    public static final String PLAYER_GOALS_TOPIC = Topics.viewTopicName(PlayerGoals.class);
    public static final String PLAYER_CARDS_TOPIC = Topics.viewTopicName(PlayerCards.class);
    public static final String TOP_SCORERS_TOPIC = Topics.viewTopicName(TopPlayers.class);

    private final JsonPojoSerde<MatchStarted> matchStartedSerde = new JsonPojoSerde<>(MatchStarted.class);
    private final JsonPojoSerde<MatchFinished> matchFinishedSerde = new JsonPojoSerde<>(MatchFinished.class);
    private final JsonPojoSerde<GoalScored> goalScoredSerde = new JsonPojoSerde<>(GoalScored.class);
    private final JsonPojoSerde<CardReceived> cardReceivedSerde = new JsonPojoSerde<>(CardReceived.class);
    private final JsonPojoSerde<PlayerStartedCareer> playerSerde = new JsonPojoSerde<>(PlayerStartedCareer.class);
    private final JsonPojoSerde<MatchScore> matchScoreSerde = new JsonPojoSerde<>(MatchScore.class);
    private final JsonPojoSerde<TeamRanking> rankingSerde = new JsonPojoSerde<>(TeamRanking.class);
    private final JsonPojoSerde<PlayerGoals> playerGoalsSerde = new JsonPojoSerde<>(PlayerGoals.class);
    private final JsonPojoSerde<PlayerCards> playerCardsSerde = new JsonPojoSerde<>(PlayerCards.class);
    private final JsonPojoSerde<TopPlayers> top10Serde = new JsonPojoSerde<>(TopPlayers.class);

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
        // a common stream for match and player statistics (can't create 2 streams from a single topic)
        KStream<String, GoalScored> goalStream = builder
                .stream(GOAL_SCORED_TOPIC, with(String(), goalScoredSerde));

        buildMatchStatistics(goalStream);
        buildPlayerStatistics(goalStream);
    }

    private void buildMatchStatistics(KStream<String, GoalScored> goalStream) {
        KStream<String, MatchStarted> matchStartedStream = builder
                .stream(MATCH_STARTED_TOPIC, with(String(), matchStartedSerde));

        KStream<String, MatchFinished> matchFinishedStream = builder
                .stream(MATCH_FINISHED_TOPIC, with(String(), matchFinishedSerde));

        KStream<String, MatchScore> scoreStream = matchStartedStream
                .leftJoin(goalStream, (match, goal) -> new MatchScore(match).goal(goal),
                    JoinWindows.of(maxMatchDuration), with(String(), matchStartedSerde, goalScoredSerde)
        );

        KTable<String, MatchScore> scoreTable = scoreStream
                .groupByKey()
                .reduce(MatchScore::aggregate, materialized(MATCH_SCORES_STORE, matchScoreSerde));
        scoreTable.toStream().to(MATCH_SCORES_TOPIC, Produced.with(String(), matchScoreSerde));

        KStream<String, MatchScore> finalScoreStream = matchFinishedStream
                .leftJoin(scoreTable, (matchFinished, matchScore) -> matchScore,
                    with(String(), matchFinishedSerde, matchScoreSerde)
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
                .groupByKey(Serialized.with(String(), rankingSerde))
                .reduce(TeamRanking::aggregate, materialized(TEAM_RANKING_STORE, rankingSerde));

        // publish changes to a view topic
        rankingTable.toStream().to(TEAM_RANKING_TOPIC, Produced.with(String(), rankingSerde));
    }

    private void buildPlayerStatistics(KStream<String, GoalScored> goalStream) {
        KTable<String, PlayerStartedCareer> playerTable = builder
                .table(PLAYER_STARTED_TOPIC, with(String(), playerSerde));

        KTable<String, PlayerGoals> playerGoalsTable = goalStream
                .selectKey((matchId, goal) -> goal.getScorerId())
                .leftJoin(playerTable, (goal, player) -> new PlayerGoals(player).goal(goal),
                    with(String(), goalScoredSerde, playerSerde))
                .groupByKey(Serialized.with(String(), playerGoalsSerde))
                .reduce(PlayerGoals::aggregate, materialized(PLAYER_GOALS_STORE, playerGoalsSerde));

        KTable<String, PlayerCards> playerCardsTable = builder
                .stream(CARD_RECEIVED_TOPIC, with(String(), cardReceivedSerde))
                .selectKey((matchId, card) -> card.getReceiverId())
                .leftJoin(playerTable, (card, player) -> new PlayerCards(player).card(card),
                    with(String(), cardReceivedSerde, playerSerde))
                .groupByKey(Serialized.with(String(), playerCardsSerde))
                .reduce(PlayerCards::aggregate, materialized(PLAYER_CARDS_STORE, playerCardsSerde));

        // publish changes to a view topic
        playerCardsTable.toStream().to(PLAYER_CARDS_TOPIC, Produced.with(String(), playerCardsSerde));

        KStream<String, PlayerGoals> playerGoalsStream = playerGoalsTable.toStream();
        playerGoalsStream.to(PLAYER_GOALS_TOPIC, Produced.with(String(), playerGoalsSerde));

        buildTop10Scorers(playerGoalsStream);
    }

    private void buildTop10Scorers(KStream<String, PlayerGoals> playerGoalsStream) {
        KTable<String, TopPlayers> top10Table = playerGoalsStream
                // create a single record that includes the top scorers
                .groupBy((playerId, playerGoals) -> "top10Scorers", Serialized.with(Serdes.String(), playerGoalsSerde))
                .aggregate(() -> new TopPlayers(10), (playerId, playerStat, top10) -> top10.aggregate(playerStat),
                    materialized(TOP_SCORERS_STORE, top10Serde));

        top10Table.toStream().to(TOP_SCORERS_TOPIC, Produced.with(String(), top10Serde));
    }
}
