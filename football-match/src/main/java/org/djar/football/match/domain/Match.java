package org.djar.football.match.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Match {

    public enum State {

        SCHEDULED(null), STARTED(SCHEDULED), FINISHED(STARTED), CANCELLED(STARTED);

        private final State previous;

        State(State previous) {
            this.previous = previous;
        }

        private boolean transitionAllowed(State next) {
            return next.previous == this;
        }
    }

    private String id;
    private LocalDateTime date;
    private Team homeTeam;
    private Team awayTeam;
    private State state;

    private List<Goal> goals = new ArrayList<>();
    private List<Card> cards = new ArrayList<>();

    private Match() {
    }

    Match(String id, LocalDateTime date, Team homeTeam, Team awayTeam) {
        this.id = Objects.requireNonNull(id);
        this.date = date;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.state = State.SCHEDULED;
    }

    public Goal newGoal(String goalId, int minute, String scorerId, String scoredForId) {
        Goal goal;

        if (scoredForId.equals(homeTeam.getClubId())) {
            goal = new Goal(goalId, id, minute, scorerId, homeTeam.getClubId());
        } else if (scoredForId.equals(awayTeam.getClubId())) {
            goal = new Goal(goalId, id, minute, scorerId, awayTeam.getClubId());
        } else {
            throw new IllegalArgumentException("Invalid team id: " + scoredForId);
        }
        goals.add(goal);
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<Goal> getGoals() {
        return goals;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void validateTransistionTo(State newState) {
        if (!state.transitionAllowed(newState)) {
            throw new IllegalStateException("Cannot change match state from " + state + " to " + newState
                + ", match id: " + id);
        }
    }

    public void setState(State newState) {
        validateTransistionTo(newState);
        this.state = newState;
    }
}
