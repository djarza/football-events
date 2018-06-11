package org.djar.football.model.event;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public abstract class Event {

    private final EventMetadata metadata = new EventMetadata();

    public abstract String getAggId();

    public EventMetadata getMetadata() {
        return metadata;
    }

    public Event timestamp(long timestamp) {
        metadata.setTimestamp(timestamp);
        return this;
    }

    public Event timestamp(LocalDateTime timestamp) {
        return timestamp(timestamp.toInstant(ZoneOffset.of("Z")).toEpochMilli());
    }

    @Override
    public String toString() {
        return String.valueOf(metadata);
    }
}
