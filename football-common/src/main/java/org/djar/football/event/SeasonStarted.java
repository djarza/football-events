package org.djar.football.event;

import java.time.LocalDate;

public class SeasonStarted {

    private final String leagueId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public SeasonStarted(String leagueId, LocalDate startDate, LocalDate endDate) {
        this.leagueId = leagueId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getLeagueId() {
        return leagueId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
