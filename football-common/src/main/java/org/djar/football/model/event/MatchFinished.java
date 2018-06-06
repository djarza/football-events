package org.djar.football.model.event;

import java.util.Objects;

public class MatchFinished extends Event {

    private String matchId;

    private MatchFinished() {
    }

    public MatchFinished(String matchId) {
        this.matchId = Objects.requireNonNull(matchId);
    }

    @Override
    public String getAggId() {
        return matchId;
    }

    public String getMatchId() {
        return matchId;
    }

    @Override
    public String toString() {
        return matchId;
    }
}
