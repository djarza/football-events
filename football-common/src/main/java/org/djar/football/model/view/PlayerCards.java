package org.djar.football.model.view;

import java.util.Objects;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.PlayerStartedCareer;

public class PlayerCards {

    private String playerId;
    private String playerName;
    private int yellowCards;
    private int redCards;

    private PlayerCards() {
    }

    public PlayerCards(PlayerStartedCareer player) {
        this.playerId = player.getPlayerId();
        this.playerName = player.getName();
    }

    public PlayerCards card(CardReceived card) {
        if (card != null) {
            if (card.getType() == CardReceived.Type.YELLOW) {
                yellowCards = 1;
            } else if (card.getType() == CardReceived.Type.RED) {
                redCards = 1;
            }
        }
        return this;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getYellowCards() {
        return yellowCards;
    }

    public int getRedCards() {
        return redCards;
    }

    public static PlayerCards join(PlayerCards stat1, PlayerCards stat2) {
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
        }
        return stat1;
    }

    public PlayerCards aggregate(PlayerCards other) {
        assertPlayerId(other);
        this.yellowCards += other.yellowCards;
        this.redCards += other.redCards;
        return this;
    }

    private void assertPlayerId(PlayerCards other) {
        if (!Objects.equals(playerId, other.playerId)) {
            throw new IllegalArgumentException(playerId + " != " + other.playerId);
        }
    }

    @Override
    public String toString() {
        return playerName + " " + yellowCards + " " + redCards;
    }
}
