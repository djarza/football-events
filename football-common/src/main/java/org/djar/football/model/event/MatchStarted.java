package org.djar.football.model.event;

import java.util.Objects;

public class MatchStarted extends Event {

    private String matchId;
    private String homeClubId;
    private String awayClubId;

    private MatchStarted() {
    }

    public MatchStarted(String matchId, String homeClubId, String awayClubId) {
        this.matchId = Objects.requireNonNull(matchId);
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
    }

    @Override
    public String getAggId() {
        return matchId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getHomeClubId() {
        return homeClubId;
    }

    public String getAwayClubId() {
        return awayClubId;
    }

    @Override
    public String toString() {
        return matchId + "," + homeClubId + " vs" + awayClubId;
    }
}
