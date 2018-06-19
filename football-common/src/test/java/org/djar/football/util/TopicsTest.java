package org.djar.football.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.djar.football.model.event.MatchScheduled;
import org.djar.football.model.event.PlayerStartedCareer;
import org.djar.football.model.view.TopPlayers;
import org.junit.jupiter.api.Test;

public class TopicsTest {

    @Test
    public void eventTopicName() throws Exception {
        assertThat(Topics.eventTopicName(PlayerStartedCareer.class)).isEqualTo("fb-event.player-started-career");
    }

    @Test
    public void viewTopicName() throws Exception {
        assertThat(Topics.viewTopicName(TopPlayers.class)).isEqualTo("fb-view.top-players");
    }
}
