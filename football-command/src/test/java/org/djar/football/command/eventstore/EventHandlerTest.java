package org.djar.football.command.eventstore;

import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.command.domain.Goal;
import org.djar.football.command.domain.Match;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchScheduled;
import org.djar.football.event.MatchStarted;
import org.djar.football.test.StreamsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventHandlerTest {

    private StreamsTester tester;

    @Before
    public void setUp() throws Exception {
        tester = new StreamsTester(getClass().getName());

        Topology topology = new Topology();
        new EventHandler().init(topology);

        tester.setUp(topology);
    }

    @Test
    public void test() throws Exception {
        tester.sendEvents("match-scheduled.json", MatchScheduled.class);
        tester.sendEvents("match-started.json", MatchStarted.class);
        tester.sendEvents("goal-scored.json", GoalScored.class);

        ReadOnlyKeyValueStore<String, Match> matchStore = tester.getStore(EventHandler.MATCH_STORE);
        ReadOnlyKeyValueStore<String, Goal> goalStore = tester.getStore(EventHandler.GOAL_STORE);

        assertThat(tester.count(matchStore)).isEqualTo(24);

        Match brentfordVsNottinghamForest = matchStore.get("15");
        assertThat(brentfordVsNottinghamForest.getState()).isEqualTo(Match.State.STARTED);

        assertThat(matchStore.get("23").getState()).isEqualTo(Match.State.SCHEDULED);

        assertThat(tester.count(goalStore)).isEqualTo(51);
    }

    @After
    public void tearDown() throws Exception {
        tester.close();
    }
}
