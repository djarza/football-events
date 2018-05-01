package org.djar.football.command.eventstore;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.djar.football.event.Event;
import org.djar.football.event.EventMetadata;

public class EventPublisher {

    private KafkaProducer<String, Event> producer;

    public EventPublisher(KafkaProducer<String, Event> producer) {
        this.producer = producer;
    }

    public void fire(Event event) {
        long timestamp = System.currentTimeMillis();
        String eventId = generateId();
        event.setMetadata(new EventMetadata(eventId, null, timestamp, 1));
        String topic = Event.eventName(event.getClass());
        ProducerRecord<String, Event> record = new ProducerRecord<>(topic, 0, timestamp, eventId, event);

        try {
            // send synchronously
            producer.send(record).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
