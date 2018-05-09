package org.djar.football.stream;

import java.util.Objects;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

public class ReadOnlyKeyValueStoreRepository {

    private final KafkaStreams kafkaStreams;

    public ReadOnlyKeyValueStoreRepository(KafkaStreams kafkaStreams) {
        this.kafkaStreams = kafkaStreams;
    }

    public <T> T find(String id, String storeName) {
        Objects.requireNonNull(id, "Null id");

        ReadOnlyKeyValueStore<String, T> store = kafkaStreams.store(
                storeName, QueryableStoreTypes.<String, T>keyValueStore());
        T result = store.get(id);

        if (result == null) {
            throw new RuntimeException("Item " + id + " not found in " + storeName);
        }
        return result;
    }
}
