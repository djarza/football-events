package org.djar.football.event;

import java.util.Objects;

public class MatchFinished extends Event {

    private String matchId;
    private String homeClubId;
    private String awayClubId;

    private MatchFinished() {
    }

    public MatchFinished(String matchId, String homeClubId, String awayClubId) {
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
        return matchId;
    }
}
