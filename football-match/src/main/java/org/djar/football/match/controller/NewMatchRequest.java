package org.djar.football.match.controller;

import java.time.LocalDateTime;

public class NewMatchRequest {

    private String id;
    private LocalDateTime reqTimestamp;
    private String seasonId;
    private LocalDateTime matchDate;
    private String homeClubId;
    private String awayClubId;

    private NewMatchRequest() {
    }

    public NewMatchRequest(String id, String seasonId, LocalDateTime matchDate, String homeClubId, String awayClubId,
            LocalDateTime reqTimestamp) {
        this.id = id;
        this.seasonId = seasonId;
        this.matchDate = matchDate;
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
        this.reqTimestamp = reqTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getReqTimestamp() {
        return reqTimestamp;
    }

    public void setReqTimestamp(LocalDateTime reqTimestamp) {
        this.reqTimestamp = reqTimestamp;
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
