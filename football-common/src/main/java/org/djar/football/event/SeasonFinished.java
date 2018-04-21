package org.djar.football.event;

public class SeasonFinished {

    private final String leagueId;

    public SeasonFinished(String leagueId) {
        this.leagueId = leagueId;
    }

    public String getLeagueId() {
        return leagueId;
    }
}
