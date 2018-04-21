package org.djar.football.event;

public class MatchFinished extends Event {

    private String matchId;
    private String homeClubId;
    private String awayClubId;

    private MatchFinished() {
    }

    public MatchFinished(String matchId, String homeClubId, String awayClubId) {
        this.matchId = matchId;
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
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
