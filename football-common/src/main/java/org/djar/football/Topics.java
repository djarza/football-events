package org.djar.football;

import org.djar.football.event.Event;

public class Topics {

    public static final String TOPIC_NAME_PREFIX = "fb-";

    private Topics() {
    }

    public static <E extends Event> String eventTopicName(Class<E> eventType) {
        return topicName("event", eventType);
    }

    public static <T> String viewTopicName(Class<T> eventType) {
        return topicName("view", eventType);
    }

    private static String topicName(String prefix, Class eventType) {
        return TOPIC_NAME_PREFIX + prefix + "." + eventType.getSimpleName().replaceAll("(.)(\\p{Upper}+)", "$1-$2")
            .toLowerCase();
    }
}
