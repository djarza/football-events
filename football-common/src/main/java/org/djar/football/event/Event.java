package org.djar.football.event;

public abstract class Event {

    private EventMetadata metadata;

    protected Event() {
    }

    public EventMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(EventMetadata metadata) {
        this.metadata = metadata;
    }

    public static <E extends Event> String eventName(Class<E> eventType) {
        return eventType.getSimpleName().replaceAll("(.)(\\p{Upper}+)", "$1-$2").toLowerCase() + "-event";
    }

    @Override
    public String toString() {
        return String.valueOf(metadata);
    }
}
