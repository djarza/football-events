package org.djar.football.query;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchStarted;
import org.djar.football.query.model.MatchScore;
import org.djar.football.query.model.Ranking;
import org.djar.football.query.projection.MatchStatisticsBuilder;
import org.djar.football.test.StreamsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.djar.football.query.projection.MatchStatisticsBuilder.*;

public class MatchStatisticsBuilderTest {

    private StreamsTester tester;
    private MatchStatisticsBuilder stats;

    @Before
    public void setUp() throws Exception {
        tester = new StreamsTester(getClass().getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        stats = new MatchStatisticsBuilder();
        stats.build(streamsBuilder);

        Topology topology = streamsBuilder.build();
        tester.setUp(topology);
    }

    @Test
    public void test() throws Exception {
        tester.sendEvents("match-started.json", MatchStarted.class);
        tester.sendEvents("goal-scored.json", GoalScored.class);
        tester.sendEvents("match-finished.json", MatchFinished.class);

        ReadOnlyKeyValueStore<String, MatchScore> matchStore = tester.getStore(MATCH_SCORES_STORE);
        ReadOnlyKeyValueStore<String, Ranking> rankingStore = tester.getStore(RANKING_STORE);

        assertThat(tester.count(rankingStore)).isEqualTo(24);
        assertThat(tester.count(matchStore)).isEqualTo(22);

        MatchScore brentfordVsNottinghamForest = matchStore.get("15");
        assertThat(brentfordVsNottinghamForest.getHomeGoals()).isEqualTo(3);
        assertThat(brentfordVsNottinghamForest.getAwayGoals()).isEqualTo(4);

        MatchScore leedsUnitedVsPrestonNorthEnd = matchStore.get("19");
        assertThat(leedsUnitedVsPrestonNorthEnd.getHomeGoals()).isEqualTo(0);
        assertThat(leedsUnitedVsPrestonNorthEnd.getAwayGoals()).isEqualTo(0);

        Ranking nottinghamForest = rankingStore.get("Nottingham Forest");
        assertThat(nottinghamForest.getMatchesPlayed()).isEqualTo(2);
        assertThat(nottinghamForest.getGoalsFor()).isEqualTo(5);
        assertThat(nottinghamForest.getGoalsAgainst()).isEqualTo(3);
        assertThat(nottinghamForest.getPoints()).isEqualTo(6);

        Ranking burtonAlbion = rankingStore.get("Burton Albion");
        assertThat(burtonAlbion.getMatchesPlayed()).isEqualTo(2);
        assertThat(burtonAlbion.getGoalsFor()).isEqualTo(1);
        assertThat(burtonAlbion.getGoalsAgainst()).isEqualTo(5);
        assertThat(burtonAlbion.getPoints()).isEqualTo(0);
    }

    @After
    public void tearDown() throws Exception {
        tester.close();
    }
}
