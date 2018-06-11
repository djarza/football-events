package org.djar.football.match.controller;

import java.time.LocalDateTime;

public class NewMatchRequest {

    private String id;
    private LocalDateTime requestTimestamp;
    private String seasonId;
    private LocalDateTime matchDate;
    private String homeClubId;
    private String awayClubId;

    private NewMatchRequest() {
    }

    public NewMatchRequest(String id, String seasonId, LocalDateTime matchDate, String homeClubId, String awayClubId,
            LocalDateTime requestTimestamp) {
        this.id = id;
        this.seasonId = seasonId;
        this.matchDate = matchDate;
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
        this.requestTimestamp = requestTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public LocalDateTime getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDateTime matchDate) {
        this.matchDate = matchDate;
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
