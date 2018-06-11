package org.djar.football.match.controller;

import java.time.LocalDateTime;

public class MatchStateRequest {

    private String newState;
    private LocalDateTime requestTimestamp;

    private MatchStateRequest() {
    }

    public MatchStateRequest(String newState, LocalDateTime requestTimestamp) {
        this.newState = newState;
        this.requestTimestamp = requestTimestamp;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }
}
