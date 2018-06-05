package org.djar.football.model.event;

public class EventMetadata {

    private String eventId;
    private String processId;
    private long timestamp;
    private int version;

    private EventMetadata() {
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

    public String getProcessId() {
        return processId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return String.valueOf(eventId);
    }
}
