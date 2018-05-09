package org.djar.football.event;

public abstract class Event {

    private EventMetadata metadata;

    public abstract String getAggId();

    public EventMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(EventMetadata metadata) {
        this.metadata = metadata;
    }

    public static <E extends Event> String eventName(Class<E> eventType) {
        return "fb.event." + eventType.getSimpleName().replaceAll("(.)(\\p{Upper}+)", "$1_$2").toLowerCase();
    }

    @Override
    public String toString() {
        return String.valueOf(metadata);
    }
}
