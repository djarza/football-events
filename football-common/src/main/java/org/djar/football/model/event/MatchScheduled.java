package org.djar.football.model.event;

import java.time.LocalDateTime;
import java.util.Objects;

public class MatchScheduled extends Event {

    private String matchId;
    private String seasonId;
    private LocalDateTime date;
    private String homeClubId;
    private String awayClubId;

    private MatchScheduled() {
    }

    public MatchScheduled(String matchId, String seasonId, LocalDateTime date, String homeClubId, String awayClubId) {
        this.matchId = Objects.requireNonNull(matchId);
        this.seasonId = seasonId;
        this.date = date;
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

    public String getSeasonId() {
        return seasonId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getHomeClubId() {
        return homeClubId;
    }

    public String getAwayClubId() {
        return awayClubId;
    }

    @Override
    public String toString() {
        return matchId + "," + seasonId + "," + homeClubId + " vs " + awayClubId;
    }
}
