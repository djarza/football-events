package org.djar.football.stream;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.djar.football.Topics;
import org.djar.football.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessorUtil {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorUtil.class);

    private ProcessorUtil() {
    }

    public static <E extends Event, D> void addProcessor(Topology topology, Class<E> eventType,
            EventProcessor<E, D> proc, String store) {
        String topic = Topics.topicName(eventType);
        topology.addSource(eventType.getSimpleName() + "Source", Serdes.String().deserializer(),
                new JsonPojoSerde<E>(eventType), topic)
                .addProcessor(eventType.getSimpleName() + "Process",
                    () -> new ProcessorWrapper<E, D>(proc, topic, store),
                eventType.getSimpleName() + "Source");
    }

    public static <D, E extends Event> void addStore(Topology topology, Class<D> domainType, String store,
            Class<E>... eventTypes) {
        StoreBuilder<KeyValueStore<String, D>> matchStoreBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(store), Serdes.String(), new JsonPojoSerde<D>(domainType))
                .withLoggingDisabled();

        String[] processorNames = Stream.of(eventTypes)
            .map(event -> event.getSimpleName() + "Process")
            .collect(Collectors.toList()).toArray(new String[eventTypes.length]);

        topology.addStateStore(matchStoreBuilder, processorNames);
    }

    @FunctionalInterface
    public interface EventProcessor<E extends Event, D> {

        void process(String eventId, E event, KeyValueStore<String, D> store);
    }

    private static class ProcessorWrapper<E extends Event, D> extends AbstractProcessor<String, E> {

        private final String storeName;
        private final String topic;
        private final EventProcessor<E, D> processor;

        private KeyValueStore<String, D> store;

        private ProcessorWrapper(EventProcessor<E, D> processor, String topic, String storeName) {
            this.processor = processor;
            this.topic = topic;
            this.storeName = storeName;
        }

        @Override
        public void init(ProcessorContext context) {
            store = (KeyValueStore<String, D>)context.getStateStore(storeName);
        }

        @Override
        public void process(String eventId, E event) {
            logger.debug("Event received from topic {}: {}->{}", topic, eventId, event);
            processor.process(eventId, event, store);
        }

        @Override
        public void close() {
            store.close();
        }
    }
}
