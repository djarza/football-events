package org.djar.football.match.controller;

import java.time.LocalDateTime;

public class GoalRequest {

    private String id;
    private LocalDateTime reqTimestamp;
    private int minute;
    private String scorerId;

    private GoalRequest() {
    }

    public GoalRequest(String id, int minute, String scorerId, LocalDateTime reqTimestamp) {
        this.id = id;
        this.minute = minute;
        this.scorerId = scorerId;
        this.reqTimestamp = reqTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getScorerId() {
        return scorerId;
    }

    public void setScorerId(String scorerId) {
        this.scorerId = scorerId;
    }

    public LocalDateTime getReqTimestamp() {
        return reqTimestamp;
    }

    public void setReqTimestamp(LocalDateTime reqTimestamp) {
        this.reqTimestamp = reqTimestamp;
    }
}
