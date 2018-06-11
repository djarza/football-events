package org.djar.football.match.controller;

import java.time.LocalDateTime;

public class CardRequest {

    private String id;
    private LocalDateTime requestTimestamp;
    private int minute;
    private String receiverId;
    private String type;

    private CardRequest() {
    }

    public CardRequest(String id, int minute, String receiverId, String type, LocalDateTime requestTimestamp) {
        this.id = id;
        this.minute = minute;
        this.receiverId = receiverId;
        this.type = type;
        this.requestTimestamp = requestTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
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
