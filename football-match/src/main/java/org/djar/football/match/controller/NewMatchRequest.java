package org.djar.football.match.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NewMatchRequest {

    private String id;
    private String seasonId;
    private LocalDateTime date;
    private String homeClubId;
    private String awayClubId;

    private NewMatchRequest() {
    }

    public NewMatchRequest(String id, String seasonId, LocalDateTime date, String homeClubId, String awayClubId) {
        this.id = id;
        this.seasonId = seasonId;
        this.date = date;
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getHomeClubId() {
        return homeClubId;
    }

    public void setHomeClubId(String homeClubId) {
        this.homeClubId = homeClubId;
    }

    public String getAwayClubId() {
        return awayClubId;
    }

    public void setAwayClubId(String awayClubId) {
        this.awayClubId = awayClubId;
    }
}
