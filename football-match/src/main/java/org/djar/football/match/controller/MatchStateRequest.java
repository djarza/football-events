package org.djar.football.match.controller;

import java.time.LocalDateTime;

public class MatchStateRequest {

    private String newState;
    private LocalDateTime reqTimestamp;

    private MatchStateRequest() {
    }

    public MatchStateRequest(String newState, LocalDateTime reqTimestamp) {
        this.newState = newState;
        this.reqTimestamp = reqTimestamp;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public LocalDateTime getReqTimestamp() {
        return reqTimestamp;
    }

    public void setReqTimestamp(LocalDateTime reqTimestamp) {
        this.reqTimestamp = reqTimestamp;
    }
}
