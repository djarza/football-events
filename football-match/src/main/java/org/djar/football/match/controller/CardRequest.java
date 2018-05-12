package org.djar.football.match.controller;

public class CardRequest {

    private String id;
    private String matchId;
    private int minute;
    private String receiverId;
    private String type;

    private CardRequest() {
    }

    public CardRequest(String id, String matchId, int minute, String receiverId, String type) {
        this.id = id;
        this.matchId = matchId;
        this.minute = minute;
        this.receiverId = receiverId;
        this.type = type;
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

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
