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
import org.djar.football.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessorUtil {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorUtil.class);

    private ProcessorUtil() {
    }

    public static <E extends Event> void addProcessor(Topology topology, Class<E> eventType, EventProcessor<E> proc,
                                                      String store) {
        topology.addSource(eventType.getSimpleName() + "Source", Serdes.String().deserializer(),
                new JsonPojoSerde(eventType), Event.eventName(eventType))
                .addProcessor(eventType.getSimpleName() + "Process", () -> new ProcessorWrapper<E>(proc, store),
                eventType.getSimpleName() + "Source");
    }

    public static <D, E extends Event> void addStore(Topology topology, Class<D> domainType, String store,
            Class<E>... eventTypes) {
        StoreBuilder<KeyValueStore<String, D>> matchStoreBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(store), Serdes.String(), new JsonPojoSerde(domainType))
                .withLoggingDisabled();

        String[] processorNames = Stream.of(eventTypes)
            .map(event -> event.getSimpleName() + "Process")
            .collect(Collectors.toList()).toArray(new String[eventTypes.length]);

        topology.addStateStore(matchStoreBuilder, processorNames);
    }

    @FunctionalInterface
    public interface EventProcessor<E extends Event> {

        void process(String eventId, E event, KeyValueStore store);
    }

    private static class ProcessorWrapper<E extends Event> extends AbstractProcessor<String, E> {

        private final String storeName;
        private final EventProcessor<E> processor;

        private KeyValueStore<String, E> store;

        public ProcessorWrapper(EventProcessor processor, String storeName) {
            this.processor = processor;
            this.storeName = storeName;
        }

        @Override
        public void init(ProcessorContext context) {
            store = (KeyValueStore)context.getStateStore(storeName);
        }

        @Override
        public void process(String eventId, E event) {
            processor.process(eventId, event, store);
        }

        @Override
        public void close() {
            store.close();
        }
    }
}
