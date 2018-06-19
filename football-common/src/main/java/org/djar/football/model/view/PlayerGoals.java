package org.djar.football.model.view;

import java.util.Objects;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.PlayerStartedCareer;

public class PlayerGoals {

    private String playerId;
    private String playerName;
    private int goals;

    private PlayerGoals() {
    }

    public PlayerGoals(PlayerStartedCareer player) {
        this.playerId = player.getPlayerId();
        this.playerName = player.getName();
    }

    public PlayerGoals goal(GoalScored goal) {
        if (goal != null) {
            goals = 1;
        }
        return this;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getGoals() {
        return goals;
    }

    public void setGoals(int goals) {
        this.goals = goals;
    }

    public static PlayerGoals join(PlayerGoals stat1, PlayerGoals stat2) {
        if (stat1 == null) {
            return stat2;
        }
        if (stat2 != null) {
            stat1.assertPlayerId(stat2);

            if (stat1.goals == 0) {
                stat1.goals = stat2.goals;
            }
        }
        return stat1;
    }

    public PlayerGoals aggregate(PlayerGoals other) {
        assertPlayerId(other);
        this.goals += other.goals;
        return this;
    }

    private void assertPlayerId(PlayerGoals other) {
        if (!Objects.equals(playerId, other.playerId)) {
            throw new IllegalArgumentException(playerId + " != " + other.playerId);
        }
    }

    @Override
    public String toString() {
        return playerName + " " + goals;
    }
}
