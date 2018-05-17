package org.djar.football.event;

import java.util.Objects;

public class CardReceived extends Event {

    public enum Type {
        YELLOW, RED
    }

    private String cardId;
    private String matchId;
    private int minute;
    private String receiverId;
    private Type type;

    private CardReceived() {
    }

    public CardReceived(String cardId, String matchId, int minute, String receiverId, Type type) {
        this.cardId = Objects.requireNonNull(cardId);
        this.matchId = Objects.requireNonNull(matchId);
        this.minute = minute;
        this.receiverId = receiverId;
        this.type = type;
    }

    @Override
    public String getAggId() {
        return matchId;
    }

    public String getCardId() {
        return cardId;
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

    @Override
    public String toString() {
        return matchId + "," + minute + "m," + receiverId + "," + type;
    }
}
