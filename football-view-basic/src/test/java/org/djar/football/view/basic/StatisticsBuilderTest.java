package org.djar.football.view.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.djar.football.view.basic.StatisticsBuilder.MATCH_SCORES_STORE;
import static org.djar.football.view.basic.StatisticsBuilder.PLAYER_CARDS_STORE;
import static org.djar.football.view.basic.StatisticsBuilder.PLAYER_GOALS_STORE;
import static org.djar.football.view.basic.StatisticsBuilder.TEAM_RANKING_STORE;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.MatchFinished;
import org.djar.football.model.event.MatchStarted;
import org.djar.football.model.event.PlayerStartedCareer;
import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerCards;
import org.djar.football.model.view.PlayerGoals;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.test.StreamsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatisticsBuilderTest {

    private StreamsTester tester;

    @Before
    public void setUp() {
        tester = new StreamsTester(getClass().getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        new StatisticsBuilder(streamsBuilder).build();

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
        ReadOnlyKeyValueStore<String, TeamRanking> rankingStore = tester.getStore(TEAM_RANKING_STORE);

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

        ReadOnlyKeyValueStore<String, PlayerGoals> goalsStore = tester.getStore(PLAYER_GOALS_STORE);
        ReadOnlyKeyValueStore<String, PlayerCards> cardsStore = tester.getStore(PLAYER_CARDS_STORE);

        PlayerGoals bBradleyJohnsonGoals = goalsStore.get("B. Bradley Johnson");
        assertThat(bBradleyJohnsonGoals.getGoals()).isEqualTo(1);
        assertThat(cardsStore.get("B. Bradley Johnson")).isNull();

        PlayerGoals aAndreasBouchalakisGoals = goalsStore.get("A. Andreas Bouchalakis");
        PlayerCards aAndreasBouchalakisCards = cardsStore.get("A. Andreas Bouchalakis");
        assertThat(aAndreasBouchalakisGoals.getGoals()).isEqualTo(2);
        assertThat(aAndreasBouchalakisCards.getYellowCards()).isEqualTo(3);
        assertThat(aAndreasBouchalakisCards.getRedCards()).isEqualTo(0);

        PlayerCards jJamesVaughanCards = cardsStore.get("J. James Vaughan");
        assertThat(jJamesVaughanCards.getYellowCards()).isEqualTo(1);
        assertThat(jJamesVaughanCards.getRedCards()).isEqualTo(0);
        assertThat(goalsStore.get("J. James Vaughan")).isNull();

        PlayerCards dDarylMurphyCards = cardsStore.get("D. Daryl Murphy");
        PlayerGoals dDarylMurphyGoals = goalsStore.get("D. Daryl Murphy");
        assertThat(dDarylMurphyGoals.getGoals()).isEqualTo(1);
        assertThat(dDarylMurphyCards.getYellowCards()).isEqualTo(0);
        assertThat(dDarylMurphyCards.getRedCards()).isEqualTo(1);
    }

    @After
    public void tearDown() throws Exception {
        tester.close();
    }
}
