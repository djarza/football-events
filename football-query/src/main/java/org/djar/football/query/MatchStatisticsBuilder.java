package org.djar.football.query;

import java.util.LinkedList;
import java.util.List;
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
import org.djar.football.event.Event;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchStarted;
import org.djar.football.query.model.MatchScore;
import org.djar.football.query.model.Ranking;
import org.djar.football.stream.JsonPojoSerde;

public class MatchStatisticsBuilder {

    private String matchScoresStore = "match-scores-store";
    private String rankingStore = "ranking-store";

    private String matchStartedTopic = Event.eventName(MatchStarted.class);
    private String goalScoredTopic = Event.eventName(GoalScored.class);
    private String matchFinishedTopic = Event.eventName(MatchFinished.class);

    private long matchGoalTimeDifference = (
            /* standard time */
            (45 + 15 + 45 + 10)
            /* play off */
            + (15 + 5 + 15)
            /* penalty shoot-out */
            + 30
            ) * 60 * 1000; // ms

    public String getMatchScoresStore() {
        return matchScoresStore;
    }

    public void setMatchScoresStore(String matchScoresStore) {
        this.matchScoresStore = matchScoresStore;
    }

    public String getRankingStore() {
        return rankingStore;
    }

    public void setRankingStore(String rankingStore) {
        this.rankingStore = rankingStore;
    }

    public String getMatchStartedTopic() {
        return matchStartedTopic;
    }

    public void setMatchStartedTopic(String matchStartedTopic) {
        this.matchStartedTopic = matchStartedTopic;
    }

    public String getGoalScoredTopic() {
        return goalScoredTopic;
    }

    public void setGoalScoredTopic(String goalScoredTopic) {
        this.goalScoredTopic = goalScoredTopic;
    }

    public String getMatchFinishedTopic() {
        return matchFinishedTopic;
    }

    public void setMatchFinishedTopic(String matchFinishedTopic) {
        this.matchFinishedTopic = matchFinishedTopic;
    }

    public long getMatchGoalTimeDifference() {
        return matchGoalTimeDifference;
    }

    public void setMatchGoalTimeDifference(long matchGoalTimeDifference) {
        this.matchGoalTimeDifference = matchGoalTimeDifference;
    }

    public void build(StreamsBuilder builder) {
        JsonPojoSerde<MatchStarted> matchStartedSerde = new JsonPojoSerde<>(MatchStarted.class);
        JsonPojoSerde<MatchFinished> matchFinishedSerde = new JsonPojoSerde<>(MatchFinished.class);
        JsonPojoSerde<GoalScored> goalScoredSerde = new JsonPojoSerde<>(GoalScored.class);
        JsonPojoSerde<MatchScore> matchScoreSerde = new JsonPojoSerde<>(MatchScore.class);
        JsonPojoSerde<Ranking> rankingSerde = new JsonPojoSerde<>(Ranking.class);

        // new key: matchId
        KStream<String, MatchStarted> matchStartedStream = builder.stream(
                matchStartedTopic, Consumed.with(Serdes.String(), matchStartedSerde))
                .map((eventId, matchStarted) -> KeyValue.pair(matchStarted.getMatchId(), matchStarted));

        // new key: matchId
        KStream<String, GoalScored> goalStream = builder.stream(
                goalScoredTopic, Consumed.with(Serdes.String(), goalScoredSerde))
                .map((eventId, goal) -> KeyValue.pair(goal.getMatchId(), goal));

        // new key: matchId
        KStream<String, MatchFinished> matchFinishedStream = builder.stream(
                matchFinishedTopic, Consumed.with(Serdes.String(), matchFinishedSerde))
                .map((eventId, matchFinished) -> KeyValue.pair(matchFinished.getMatchId(), matchFinished));

        KStream<String, MatchScore> scoreStream = matchStartedStream.leftJoin(goalStream, (matchStarted, goal) -> {
                int homeGoals = matchStarted.scoredForHomeClub(goal) ? 1 : 0;
                int awayGoals = matchStarted.scoredForAwayClub(goal) ? 1 : 0;
                return new MatchScore(matchStarted.getHomeClubId(), matchStarted.getAwayClubId(), homeGoals,
                    awayGoals);
            },
            JoinWindows.of(matchGoalTimeDifference), Joined.with(Serdes.String(), matchStartedSerde, goalScoredSerde)
        );

        KTable<String, MatchScore> scoreTable = scoreStream.groupByKey()
                .reduce(MatchScore::aggregate,
                Materialized.<String, MatchScore, KeyValueStore<Bytes, byte[]>>as(matchScoresStore).withKeySerde(Serdes.String()).withValueSerde(matchScoreSerde));

        KStream<String, MatchScore> finalScoreStream = matchFinishedStream.leftJoin(
                scoreTable, (matchFinished, matchScore) -> matchScore,
                Joined.with(Serdes.String(), matchFinishedSerde, matchScoreSerde)
        );

        // new key: clubId
        KStream<String, Ranking> rankingStream = finalScoreStream.flatMap((clubId, score) -> {
            List<KeyValue<String, Ranking>> result = new LinkedList<>();
            result.add(KeyValue.pair(score.getHomeClubId(), score.homeRanking()));
            result.add(KeyValue.pair(score.getAwayClubId(), score.awayRanking()));
            return result;
        });

        KTable<String, Ranking> rankingTable = rankingStream.groupByKey(Serialized.with(Serdes.String(), rankingSerde))
                .reduce(Ranking::aggregate,
                Materialized.<String, Ranking, KeyValueStore<Bytes, byte[]>>as(rankingStore).withKeySerde(Serdes.String()).withValueSerde(rankingSerde));
    }
}
