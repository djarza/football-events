package org.djar.football.repo;

import java.util.Objects;
import java.util.Optional;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

public class ReadOnlyKeyValueStoreRepository<T> {

    private final KafkaStreams kafkaStreams;
    private final String storeName;

    public ReadOnlyKeyValueStoreRepository(KafkaStreams kafkaStreams, String storeName) {
        this.kafkaStreams = kafkaStreams;
        this.storeName = storeName;
    }

    public Optional<T> find(String id) {
        Objects.requireNonNull(id, "Null id");

        ReadOnlyKeyValueStore<String, T> store = kafkaStreams.store(
                storeName, QueryableStoreTypes.<String, T>keyValueStore());
        return Optional.ofNullable(store.get(id));
    }
}
