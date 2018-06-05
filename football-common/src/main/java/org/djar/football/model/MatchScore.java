package org.djar.football.model;

import java.util.Objects;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchStarted;

public class MatchScore {

    private String homeClubId;
    private String awayClubId;
    private int homeGoals;
    private int awayGoals;

    public MatchScore() {
    }

    public MatchScore(MatchStarted match) {
        this.homeClubId = match.getHomeClubId();
        this.awayClubId = match.getAwayClubId();
    }

    public MatchScore aggregate(MatchScore other) {
        assertEquals(homeClubId, other.homeClubId, "homeClubId");
        assertEquals(awayClubId, other.awayClubId, "awayClubId");

        homeGoals += other.homeGoals;
        awayGoals += other.awayGoals;

        return this;
    }

    private void assertEquals(Object thisValue, Object otherValue, String name) {
        if (!Objects.equals(thisValue, otherValue)) {
            throw new IllegalArgumentException("Expected " + name + ": " + thisValue + ", found: " + otherValue);
        }
    }

    public MatchScore goal(GoalScored goal) {
        if (goal != null) {
            if (homeClubId.equals(goal.getScoredFor())) {
                homeGoals++;
            } else if (awayClubId.equals(goal.getScoredFor())) {
                awayGoals++;
            } else {
                throw new IllegalArgumentException("Goal is not assignet to match, home club: " + homeClubId
                    + ", away club: " + awayClubId + ", goal id: " + goal.getAggId());
            }
        }
        return this;
    }

    public TeamRanking homeRanking() {
        return ranking(homeGoals, awayGoals);
    }

    public TeamRanking awayRanking() {
        return ranking(awayGoals, homeGoals);
    }

    private TeamRanking ranking(int goalsFor, int goalsAgainst) {
        int result = goalsFor - goalsAgainst;
        int won = result > 0 ? 1 : 0;
        int drawn = result == 0 ? 1 : 0;
        int lose = result < 0 ? 1 : 0;
        return new TeamRanking(1, won, drawn, lose, goalsFor, goalsAgainst);
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

    public int getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(int homeGoals) {
        this.homeGoals = homeGoals;
    }

    public int getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(int awayGoals) {
        this.awayGoals = awayGoals;
    }

    @Override
    public String toString() {
        return homeClubId + " vs " + awayClubId + " " + homeGoals + ":" + awayGoals;
    }
}
