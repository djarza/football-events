package org.djar.football.model.view;

import java.util.Objects;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.PlayerStartedCareer;

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

    public static PlayerStatistic join(PlayerStatistic stat1, PlayerStatistic stat2) {
        if (stat1 == null) {
            return stat2;
        }
        if (stat2 != null) {
            stat1.assertPlayerId(stat2);

            if (stat1.yellowCards == 0) {
                stat1.yellowCards = stat2.yellowCards;
            }
            if (stat1.redCards == 0) {
                stat1.redCards = stat2.redCards;
            }
            if (stat1.goals == 0) {
                stat1.goals = stat2.goals;
            }
        }
        return stat1;
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
