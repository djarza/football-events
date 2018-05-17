package org.djar.football.event;

import java.util.Objects;

public class PlayerStartedCareer extends Event {

    private String playerId;
    private String name;

    private PlayerStartedCareer() {
    }

    public PlayerStartedCareer(String playerId, String name) {
        this.playerId = Objects.requireNonNull(playerId);
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getAggId() {
        return playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return playerId + "," + name;
    }
}
