package org.djar.football.model.view;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TeamRanking {

    private String clubId;
    private int matchesPlayed;
    private int won;
    private int drawn;
    private int lose;
    private int goalsFor;
    private int goalsAgainst;

    private TeamRanking() {
    }

    public TeamRanking(String teamId, int matchesPlayed, int won, int drawn, int lose, int goalsFor, int goalsAgainst) {
        this.clubId = teamId;
        this.matchesPlayed = matchesPlayed;
        this.won = won;
        this.drawn = drawn;
        this.lose = lose;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
    }

    public TeamRanking aggregate(TeamRanking other) {
        matchesPlayed += other.matchesPlayed;
        won += other.won;
        drawn += other.drawn;
        lose += other.lose;
        goalsFor += other.goalsFor;
        goalsAgainst += other.goalsAgainst;
        return this;
    }

    public String getClubId() {
        return clubId;
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public int getWon() {
        return won;
    }

    public void setWon(int won) {
        this.won = won;
    }

    public int getDrawn() {
        return drawn;
    }

    public void setDrawn(int drawn) {
        this.drawn = drawn;
    }

    public int getLose() {
        return lose;
    }

    public void setLose(int lose) {
        this.lose = lose;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public void setGoalsFor(int goalsFor) {
        this.goalsFor = goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public void setGoalsAgainst(int goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
    }

    @JsonIgnore
    public int getGoalsDifference() {
        return goalsFor - goalsAgainst;
    }

    @JsonIgnore
    public int getPoints() {
        return won * 3 + drawn;
    }

    @Override
    public String toString() {
        return matchesPlayed + " " + won + " " + drawn + " " + lose + " " + goalsFor + " " + goalsAgainst + " "
            + getGoalsDifference() + " " + getPoints();
    }
}
