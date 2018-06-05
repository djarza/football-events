package org.djar.football.model.event;

public abstract class Event {

    private EventMetadata metadata;

    public abstract String getAggId();

    public EventMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(EventMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return String.valueOf(metadata);
    }
}
