package org.djar.football.query.projection;

import static org.apache.kafka.streams.KeyValue.pair;

import java.util.Collection;
import java.util.LinkedList;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.djar.football.Events;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchStarted;
import org.djar.football.query.model.MatchScore;
import org.djar.football.query.model.Ranking;
import org.djar.football.stream.JsonPojoSerde;

public class MatchStatisticsBuilder {

    public static final String MATCH_SCORES_STORE = "match_scores_store";
    public static final String RANKING_STORE = "ranking_store";

    private static final String MATCH_STARTED_TOPIC = Events.topicName(MatchStarted.class);
    private static final String GOAL_SCORED_TOPIC = Events.topicName(GoalScored.class);
    private static final String MATCH_FINISHED_TOPIC = Events.topicName(MatchFinished.class);

    private long matchGoalTimeDifference = (
            /* standard time */
            (45 + 15 + 45 + 10)
            /* play off */
            + (15 + 5 + 15)
            /* penalty shoot-out */
            + 30
            ) * 60 * 1000; // ms

    public long getMatchGoalTimeDifference() {
        return matchGoalTimeDifference;
    }

    public void setMatchGoalTimeDifference(long matchGoalTimeDifference) {
        this.matchGoalTimeDifference = matchGoalTimeDifference;
    }

    public void build(StreamsBuilder builder) {
        final JsonPojoSerde<MatchStarted> matchStartedSerde = new JsonPojoSerde<>(MatchStarted.class);
        final JsonPojoSerde<MatchFinished> matchFinishedSerde = new JsonPojoSerde<>(MatchFinished.class);
        final JsonPojoSerde<GoalScored> goalScoredSerde = new JsonPojoSerde<>(GoalScored.class);
        final JsonPojoSerde<MatchScore> matchScoreSerde = new JsonPojoSerde<>(MatchScore.class);
        final JsonPojoSerde<Ranking> rankingSerde = new JsonPojoSerde<>(Ranking.class);

        KStream<String, MatchStarted> matchStartedStream = builder.stream(
                MATCH_STARTED_TOPIC, Consumed.with(Serdes.String(), matchStartedSerde));

        KStream<String, GoalScored> goalStream = builder.stream(
                GOAL_SCORED_TOPIC, Consumed.with(Serdes.String(), goalScoredSerde));

        KStream<String, MatchFinished> matchFinishedStream = builder.stream(
                MATCH_FINISHED_TOPIC, Consumed.with(Serdes.String(), matchFinishedSerde));

        KStream<String, MatchScore> scoreStream = matchStartedStream.leftJoin(goalStream,
                (matchStarted, goalScored) -> {
                    MatchScore score = new MatchScore(matchStarted.getHomeClubId(), matchStarted.getAwayClubId());
                    score.count(goalScored);
                    return score;
                },
                JoinWindows.of(matchGoalTimeDifference), Joined.with(Serdes.String(), matchStartedSerde,
                    goalScoredSerde)
        );

        KTable<String, MatchScore> scoreTable = scoreStream.groupByKey().reduce(
                MatchScore::aggregate, materialized(MATCH_SCORES_STORE, matchScoreSerde));

        KStream<String, MatchScore> finalScoreStream = matchFinishedStream.leftJoin(
                scoreTable, (matchFinished, matchScore) -> matchScore,
                Joined.with(Serdes.String(), matchFinishedSerde, matchScoreSerde)
        );

        // new key: clubId
        KStream<String, Ranking> rankingStream = finalScoreStream.flatMap(
                (clubId, matchScore) -> {
                    Collection<KeyValue<String, Ranking>> result = new LinkedList<>();
                    result.add(pair(matchScore.getHomeClubId(), matchScore.homeRanking()));
                    result.add(pair(matchScore.getAwayClubId(), matchScore.awayRanking()));
                    return result;
                });

        rankingStream.groupByKey(Serialized.with(Serdes.String(), rankingSerde)).reduce(
                Ranking::aggregate, materialized(RANKING_STORE, rankingSerde));
    }

    private <V> Materialized<String, V, KeyValueStore<Bytes, byte[]>> materialized(String storeName, Serde<V> serde) {
        return Materialized.<String, V, KeyValueStore<Bytes, byte[]>>as(storeName)
            .withKeySerde(Serdes.String()).withValueSerde(serde);
    }
}
