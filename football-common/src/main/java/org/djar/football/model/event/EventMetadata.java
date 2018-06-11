package org.djar.football.model.event;

public class EventMetadata {

    private String eventId;
    private String processId;
    private long timestamp;
    private int version;

    public EventMetadata() {
    }

    public EventMetadata(String eventId, String processId, long timestamp, int version) {
        this.eventId = eventId;
        this.processId = processId;
        this.timestamp = timestamp;
        this.version = version;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return String.valueOf(eventId);
    }
}
