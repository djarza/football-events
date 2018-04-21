package org.djar.football.event;

import java.time.LocalDate;
import java.util.Objects;

public class MatchScheduled extends Event {

    private String matchId;
    private String seasonId;
    private LocalDate startTime;
    private String homeClubId;
    private String awayClubId;

    private MatchScheduled() {
    }

    public MatchScheduled(String matchId, String seasonId, LocalDate startTime, String homeClubId, String awayClubId) {
        this.matchId = Objects.requireNonNull(matchId);
        this.seasonId = seasonId;
        this.startTime = startTime;
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public LocalDate getStartTime() {
        return startTime;
    }

    public String getHomeClubId() {
        return homeClubId;
    }

    public String getAwayClubId() {
        return awayClubId;
    }

    @Override
    public String toString() {
        return matchId + "," + seasonId + "," + homeClubId + "," + awayClubId;
    }
}
