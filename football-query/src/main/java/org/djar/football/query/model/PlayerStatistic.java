package org.djar.football.query.model;

import java.util.Objects;
import org.djar.football.event.CardReceived;
import org.djar.football.event.GoalScored;
import org.djar.football.event.PlayerStartedCareer;

public class PlayerStatistic {

    private String playerId;
    private String playerName;
    private int goals;
    private int yellowCards;
    private int redCards;

    private PlayerStatistic() {
    }

    public PlayerStatistic(PlayerStartedCareer player) {
        this.playerId = player.getPlayerId();
        this.playerName = player.getName();
    }

    public PlayerStatistic goal(GoalScored goal) {
        if (goal != null) {
            goals = 1;
        }
        return this;
    }

    public PlayerStatistic card(CardReceived card) {
        if (card != null) {
            if (card.getType() == CardReceived.Type.YELLOW) {
                yellowCards = 1;
            } else if (card.getType() == CardReceived.Type.RED) {
                redCards = 1;
            }
        }
        return this;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getGoals() {
        return goals;
    }

    public int getYellowCards() {
        return yellowCards;
    }

    public int getRedCards() {
        return redCards;
    }

    public PlayerStatistic join(PlayerStatistic other) {
        if (other != null) {
            assertPlayerId(other);

            if (this.yellowCards == 0) {
                this.yellowCards = other.yellowCards;
            }
            if (this.redCards == 0) {
                this.redCards = other.redCards;
            }
            if (this.goals == 0) {
                this.goals = other.goals;
            }
        }
        return this;
    }

    public PlayerStatistic aggregate(PlayerStatistic other) {
        assertPlayerId(other);
        this.goals += other.goals;
        this.yellowCards += other.yellowCards;
        this.redCards += other.redCards;
        return this;
    }

    private void assertPlayerId(PlayerStatistic other) {
        if (!Objects.equals(playerId, other.playerId)) {
            throw new IllegalArgumentException(playerId + " != " + other.playerId);
        }
    }

    @Override
    public String toString() {
        return playerName + " " + goals + " " + yellowCards + " " + redCards;
    }
}
