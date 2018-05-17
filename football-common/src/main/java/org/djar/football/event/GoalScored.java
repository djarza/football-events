package org.djar.football.event;

import java.util.Objects;

public class GoalScored extends Event {

    private String goalId;
    private String matchId;
    private int minute;
    private String scorerId;
    private String scoredFor;

    private GoalScored() {
    }

    public GoalScored(String goalId, String matchId, int minute, String scorerId, String scoredFor) {
        this.goalId = Objects.requireNonNull(goalId, "Null goal id");
        this.matchId = Objects.requireNonNull(matchId, "Null match id");
        this.minute = minute;
        this.scorerId = scorerId;
        this.scoredFor = scoredFor;
    }

    @Override
    public String getAggId() {
        return matchId;
    }

    public String getGoalId() {
        return goalId;
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

    @Override
    public String toString() {
        return matchId + "," + minute + "m," + scorerId + "," + scoredFor;
    }
}
