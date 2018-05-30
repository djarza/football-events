package org.djar.football;

import org.djar.football.event.Event;

public class Topics {

    public static final String TOPIC_NAME_PREFIX = "fb-";

    private Topics() {
    }

    public static <E extends Event> String topicName(Class<E> eventType) {
        return TOPIC_NAME_PREFIX + "event." + eventType.getSimpleName().replaceAll("(.)(\\p{Upper}+)", "$1-$2")
            .toLowerCase();
    }
}
