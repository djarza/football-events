package org.djar.football.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.djar.football.query.projection.PlayerStatisticsBuilder.PLAYER_STATISTICS_STORE;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.event.CardReceived;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchScheduled;
import org.djar.football.event.MatchStarted;
import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.query.model.MatchScore;
import org.djar.football.query.model.PlayerStatistic;
import org.djar.football.query.model.Ranking;
import org.djar.football.query.projection.PlayerStatisticsBuilder;
import org.djar.football.test.StreamsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PlayerStatisticsBuilderTest {

    private StreamsTester tester;
    private PlayerStatisticsBuilder stats;

    @Before
    public void setUp() throws Exception {
        tester = new StreamsTester(getClass().getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        stats = new PlayerStatisticsBuilder();
        stats.build(streamsBuilder);

        Topology topology = streamsBuilder.build();
        tester.setUp(topology);
    }

    @Test
    public void test() throws Exception {
        tester.sendEvents(getClass().getResource("player-started-career.json"), PlayerStartedCareer.class);
        tester.sendEvents(getClass().getResource("goal-scored.json"), GoalScored.class);
        tester.sendEvents(getClass().getResource("card-received.json"), CardReceived.class);

        ReadOnlyKeyValueStore<String, PlayerStatistic> statsStore = tester.getStore(PLAYER_STATISTICS_STORE);

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
