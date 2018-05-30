package org.djar.football.stream;

import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.djar.football.Topics;
import org.djar.football.event.Event;
import org.djar.football.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

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
            fillOut(event);
            String topic = Topics.topicName(event.getClass());
            ProducerRecord<String, Event> record = new ProducerRecord<>(topic, 0, event.getMetadata().getTimestamp(),
                    event.getAggId(), event);
            logger.debug("New {} event created: {}", event.getClass().getSimpleName(), event.getAggId());

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    sink.success();
                } else {
                    sink.error(exception);
                }
            });
        });
    }

    public void fillOut(Event event) {
        long timestamp = System.currentTimeMillis();
        event.setMetadata(new EventMetadata(generateId(), processId, timestamp, apiVersion));
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
