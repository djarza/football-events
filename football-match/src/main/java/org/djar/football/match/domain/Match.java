package org.djar.football.match.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Match {

    public enum State {
        SCHEDULED, STARTED, FINISHED, CANCELLED;
    }

    private String id;
    private String leagueId;
    private LocalDateTime date;
    private Team homeTeam;
    private Team awayTeam;
    private State state;

    private List<Goal> homeGoals = new ArrayList<>();
    private List<Goal> awayGoals = new ArrayList<>();
    private List<Card> cards = new ArrayList<>();

    private Match() {
    }

    Match(String id, String leagueId, LocalDateTime date, Team homeTeam, Team awayTeam) {
        this.id = Objects.requireNonNull(id);
        this.leagueId = Objects.requireNonNull(leagueId);
        this.date = Objects.requireNonNull(date);
        this.homeTeam = Objects.requireNonNull(homeTeam);
        this.awayTeam = Objects.requireNonNull(awayTeam);
        this.state = State.SCHEDULED;
    }

    public String getId() {
        return id;
    }

    public String getLeagueId() {
        return leagueId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public State getState() {
        return state;
    }

    public List<Goal> getHomeGoals() {
        return homeGoals;
    }

    public List<Goal> getAwayGoals() {
        return awayGoals;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void start() {
        if (state != State.SCHEDULED) {
            throw new IllegalStateException("Cannot start " + state + " match");
        }
        state = State.STARTED;
    }

    public void finish() {
        if (state != State.STARTED) {
            throw new IllegalStateException("Cannot finish " + state + " match");
        }
        state = State.FINISHED;
    }

    public void cancel() {
        if (state == State.FINISHED) {
            throw new IllegalStateException("Cannot cancel match that is already finished");
        }
        state = State.CANCELLED;
    }

    public Goal newGoal(String goalId, int minute, String scorerId, String scoredForId) {
        Goal goal;

        if (scoredForId.equals(homeTeam.getClubId())) {
            goal = new Goal(goalId, id, minute, scorerId, homeTeam);
            homeGoals.add(goal);
        } else if (scoredForId.equals(awayTeam.getClubId())) {
            goal = new Goal(goalId, id, minute, scorerId, awayTeam);
            awayGoals.add(goal);
        } else {
            throw new IllegalArgumentException("Invalid team id: " + scoredForId);
        }
        return goal;
    }

    public Card newRedCard(String cardId, int minute, String receiveId) {
        Card card = new Card(cardId, id, minute, receiveId, Card.Type.RED);
        cards.add(card);
        return card;
    }

    public Card newYellowCard(String cardId, int minute, String receiveId) {
        Card card = new Card(cardId, id, minute, receiveId, Card.Type.YELLOW);
        cards.add(card);
        return card;
    }
}
