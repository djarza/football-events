package org.djar.football.view.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.djar.football.view.projection.StatisticsBuilder.MATCH_SCORES_STORE;
import static org.djar.football.view.projection.StatisticsBuilder.PLAYER_STATISTIC_STORE;
import static org.djar.football.view.projection.StatisticsBuilder.RANKING_STORE;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.event.CardReceived;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchStarted;
import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.model.MatchScore;
import org.djar.football.model.PlayerStatistic;
import org.djar.football.model.TeamRanking;
import org.djar.football.test.StreamsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatisticsBuilderTest {

    private StreamsTester tester;
    private StatisticsBuilder stats;

    @Before
    public void setUp() throws Exception {
        tester = new StreamsTester(getClass().getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        stats = new StatisticsBuilder(streamsBuilder);
        stats.build();

        Topology topology = streamsBuilder.build();
        tester.setUp(topology);
    }

    @Test
    public void testMatchStatistics() {
        tester.sendEvents(getClass().getResource("player-started-career.json"), PlayerStartedCareer.class);
        tester.sendEvents(getClass().getResource("match-started.json"), MatchStarted.class);
        tester.sendEvents(getClass().getResource("goal-scored.json"), GoalScored.class);
        tester.sendEvents(getClass().getResource("match-finished.json"), MatchFinished.class);

        ReadOnlyKeyValueStore<String, MatchScore> matchStore = tester.getStore(MATCH_SCORES_STORE);
        ReadOnlyKeyValueStore<String, TeamRanking> rankingStore = tester.getStore(RANKING_STORE);

        assertThat(tester.count(rankingStore)).isEqualTo(24);
        assertThat(tester.count(matchStore)).isEqualTo(22);

        MatchScore brentfordVsNottinghamForest = matchStore.get("15");
        assertThat(brentfordVsNottinghamForest.getHomeGoals()).isEqualTo(3);
        assertThat(brentfordVsNottinghamForest.getAwayGoals()).isEqualTo(4);

        MatchScore leedsUnitedVsPrestonNorthEnd = matchStore.get("19");
        assertThat(leedsUnitedVsPrestonNorthEnd.getHomeGoals()).isEqualTo(0);
        assertThat(leedsUnitedVsPrestonNorthEnd.getAwayGoals()).isEqualTo(0);

        TeamRanking nottinghamForest = rankingStore.get("Nottingham Forest");
        assertThat(nottinghamForest.getMatchesPlayed()).isEqualTo(2);
        assertThat(nottinghamForest.getGoalsFor()).isEqualTo(5);
        assertThat(nottinghamForest.getGoalsAgainst()).isEqualTo(3);
        assertThat(nottinghamForest.getPoints()).isEqualTo(6);

        TeamRanking burtonAlbion = rankingStore.get("Burton Albion");
        assertThat(burtonAlbion.getMatchesPlayed()).isEqualTo(2);
        assertThat(burtonAlbion.getGoalsFor()).isEqualTo(1);
        assertThat(burtonAlbion.getGoalsAgainst()).isEqualTo(5);
        assertThat(burtonAlbion.getPoints()).isEqualTo(0);
    }

    @Test
    public void testPlayerStatistics() {
        tester.sendEvents(getClass().getResource("player-started-career.json"), PlayerStartedCareer.class);
        tester.sendEvents(getClass().getResource("goal-scored.json"), GoalScored.class);
        tester.sendEvents(getClass().getResource("card-received.json"), CardReceived.class);

        ReadOnlyKeyValueStore<String, PlayerStatistic> statsStore = tester.getStore(PLAYER_STATISTIC_STORE);

//        assertThat(tester.goal(statsStore)).isEqualTo(44);

        PlayerStatistic bBradleyJohnson = statsStore.get("B. Bradley Johnson");
        assertThat(bBradleyJohnson.getGoals()).isEqualTo(1);
        assertThat(bBradleyJohnson.getYellowCards()).isEqualTo(0);
        assertThat(bBradleyJohnson.getRedCards()).isEqualTo(0);

        PlayerStatistic aAndreasBouchalakis = statsStore.get("A. Andreas Bouchalakis");
        assertThat(aAndreasBouchalakis.getGoals()).isEqualTo(2);
        assertThat(aAndreasBouchalakis.getYellowCards()).isEqualTo(3);
        assertThat(aAndreasBouchalakis.getRedCards()).isEqualTo(0);

        PlayerStatistic jJamesVaughan = statsStore.get("J. James Vaughan");
        assertThat(jJamesVaughan.getGoals()).isEqualTo(0);
        assertThat(jJamesVaughan.getYellowCards()).isEqualTo(1);
        assertThat(jJamesVaughan.getRedCards()).isEqualTo(0);

        PlayerStatistic dDarylMurphy = statsStore.get("D. Daryl Murphy");
        assertThat(dDarylMurphy.getGoals()).isEqualTo(1);
        assertThat(dDarylMurphy.getYellowCards()).isEqualTo(0);
        assertThat(dDarylMurphy.getRedCards()).isEqualTo(1);
    }

    @After
    public void tearDown() throws Exception {
        tester.close();
    }
}
