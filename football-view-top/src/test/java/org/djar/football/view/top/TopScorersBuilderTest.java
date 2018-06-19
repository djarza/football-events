package org.djar.football.view.top;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.model.view.PlayerGoals;
import org.djar.football.model.view.TopPlayers;
import org.djar.football.test.StreamsTester;
import org.djar.football.util.Topics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TopScorersBuilderTest {

    private StreamsTester tester;

    @Before
    public void setUp() {
        tester = new StreamsTester(getClass().getName());

        StreamsBuilder streamsBuilder = new StreamsBuilder();
        new TopScorersBuilder(streamsBuilder).build();

        Topology topology = streamsBuilder.build();
        tester.setUp(topology);
    }

    @Test
    public void testTopPlayers() {
        tester.send(getClass().getResource("player-goals.json"), PlayerGoals.class,
                Topics.viewTopicName(PlayerGoals.class), PlayerGoals::getPlayerId);

        ReadOnlyKeyValueStore<String, TopPlayers> store = tester.getStore(TopScorersBuilder.TOP_SCORERS_STORE);
        List<PlayerGoals> players = store.get("topPlayers").getPlayers();
        assertThat(players.get(0).getGoals()).isEqualTo(5);
        assertThat(players.get(1).getGoals()).isEqualTo(4);
        assertThat(players.get(2).getGoals()).isEqualTo(2);
        assertThat(players.get(3).getGoals()).isEqualTo(1);
    }

    @After
    public void tearDown() throws Exception {
        tester.close();
    }
}
