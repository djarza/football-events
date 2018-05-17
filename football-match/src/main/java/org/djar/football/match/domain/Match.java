package org.djar.football.match.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private Match() {
    }

    public Match(String id, LocalDateTime date, Team homeTeam, Team awayTeam) {
        this.id = Objects.requireNonNull(id);
        this.date = date;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.state = State.SCHEDULED;
    }

    public Goal newGoalForHomeTeam(String goalId, int minute, Player scorer) {
        return new Goal(goalId, id, minute, scorer.getId(), homeTeam.getClubId());
    }

    public Goal newGoalForAwayTeam(String goalId, int minute, Player scorer) {
        return new Goal(goalId, id, minute, scorer.getId(), awayTeam.getClubId());
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
