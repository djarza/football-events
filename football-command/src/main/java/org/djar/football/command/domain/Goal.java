package org.djar.football.command.domain;

public class Goal {

    private String id;
    private String matchId;
    private int minute;
    private String scorerId;
    private String scoredFor;

    private Goal() {
    }

    public Goal(String id, String matchId, int minute, String scorerId, String scoredFor) {
        this.id = id;
        this.matchId = matchId;
        this.minute = minute;
        this.scorerId = scorerId;
        this.scoredFor = scoredFor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMatchId() {
        return matchId;
    }

    public int getMinute() {
        return minute;
    }

    public String getScorerId() {
        return scorerId;
    }

    public String getScoredFor() {
        return scoredFor;
    }
}
