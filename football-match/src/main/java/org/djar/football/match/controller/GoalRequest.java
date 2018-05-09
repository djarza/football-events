package org.djar.football.match.controller;

public class GoalRequest {

    private String id;
    private int minute;
    private String scorerId;

    public GoalRequest() {
    }

    public GoalRequest(String id, int minute, String scorerId) {
        this.id = id;
        this.minute = minute;
        this.scorerId = scorerId;
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
}
