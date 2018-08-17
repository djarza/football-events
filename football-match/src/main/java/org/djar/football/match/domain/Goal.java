package org.djar.football.match.domain;

import java.util.Objects;

public class Goal {

    private String id;
    private String matchId;
    private int minute;
    private String scorerId;
    private Team scoredFor;

    private Goal() {
    }

    Goal(String id, String matchId, int minute, String scorerId, Team scoredFor) {
        this.id = Objects.requireNonNull(id);
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

    public Team getScoredFor() {
        return scoredFor;
    }
}
