package org.djar.football.stream;

import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.djar.football.event.Event;
import org.djar.football.event.EventMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

public class EventPublisher {

    private final KafkaProducer<String, Event> producer;
    private final String processId;
    private final int apiVersion;

    public EventPublisher(KafkaProducer<String, Event> producer, String processId, int apiVersion) {
        this.producer = producer;
        this.processId = processId;
        this.apiVersion = apiVersion;
    }

    public Mono<Void> fire(Event event) {
        return Mono.create(sink -> {
            long timestamp = System.currentTimeMillis();
            String eventId = generateId();
            event.setMetadata(new EventMetadata(eventId, processId, timestamp, apiVersion));
            String topic = Event.eventName(event.getClass());
            ProducerRecord<String, Event> record = new ProducerRecord<>(topic, 0, timestamp, event.getAggId(), event);

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    sink.success();
                } else {
                    sink.error(exception);
                }
            });
        });
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
