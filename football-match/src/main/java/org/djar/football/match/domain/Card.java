package org.djar.football.match.domain;

import java.util.Objects;

public class Card {

    public enum Type {
        YELLOW, RED
    }

    private String id;
    private String matchId;
    private int minute;
    private String receiverId;
    private Type type;

    private Card() {
    }

    Card(String id, String matchId, int minute, String receiverId, Type type) {
        this.id = Objects.requireNonNull(id);
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

    public int getMinute() {
        return minute;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public Type getType() {
        return type;
    }
}
