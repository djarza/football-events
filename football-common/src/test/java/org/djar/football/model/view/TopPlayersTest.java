package org.djar.football.model.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TopPlayersTest {

    @Test
    public void test() {
        TopPlayers top = new TopPlayers(5);
        top.aggregate(goals("p1", 1));
        top.aggregate(goals("p5", 1));
        top.aggregate(goals("p6", 3));
        top.aggregate(goals("p4", 1));
        top.aggregate(goals("p5", 2));
        top.aggregate(goals("p3", 1));
        top.aggregate(goals("p6", 4));
        top.aggregate(goals("p4", 2));
        top.aggregate(goals("p6", 5));
        top.aggregate(goals("p4", 3));
        top.aggregate(goals("p5", 4));
        top.aggregate(goals("p3", 2));
        top.aggregate(goals("p6", 6));
        top.aggregate(goals("p5", 3));
        top.aggregate(goals("p4", 4));
        top.aggregate(goals("p2", 2));
        top.aggregate(goals("p10", 1));
        top.aggregate(goals("p5", 5));
        top.aggregate(goals("p3", 3));

        Object[] topPlayers = top.getPlayers().stream().map(PlayerGoals::getPlayerId).toArray();
        assertThat(topPlayers).containsSequence("p6", "p5", "p4", "p3", "p2");
    }

    private PlayerGoals goals(String playerId, int goals) {
        PlayerGoals playerGoals = new PlayerGoals(playerId, playerId);
        playerGoals.setGoals(goals);
        return playerGoals;
    }
}
