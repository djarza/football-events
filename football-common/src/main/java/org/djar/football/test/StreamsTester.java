package org.djar.football.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.errors.StreamsException;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.djar.football.model.event.Event;
import org.djar.football.stream.JsonPojoSerde;
import org.djar.football.util.Topics;
import org.springframework.util.FileSystemUtils;

public class StreamsTester {

    private static final String KAFKA_URL = "localhost:9092";

    private final Path kafkaTempDir;
    private final Properties streamsProps;
    private TopologyTestDriver testDriver;

    public StreamsTester(String applicationId) {
        this(KAFKA_URL, applicationId);
    }

    public StreamsTester(String bootstrapServer, String applicationId) {
        try {
            kafkaTempDir = Files.createTempDirectory("kafka_streams_" + getClass().getSimpleName());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Kafka temp dir", e);
        }
        streamsProps = new Properties();
        streamsProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        streamsProps.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        streamsProps.put(StreamsConfig.STATE_DIR_CONFIG, kafkaTempDir.toString());
    }

    public void setUp(Topology topology) {
        testDriver = new TopologyTestDriver(topology, streamsProps);
    }

    public <T extends Event> void sendEvents(URL sourceFile, Class<T> eventType) {
        sendEvents(load(sourceFile, eventType));
    }

    public void sendEvents(Event[] events) {
        ConsumerRecordFactory<String, Event> factory = new ConsumerRecordFactory<>(
                new StringSerializer(), new JsonPojoSerde<Event>());
        int eventSeq = 1;

        for (Event event : events) {
            String topic = Topics.eventTopicName(event.getClass());
            ConsumerRecord<byte[], byte[]> record = factory.create(topic, event.getAggId(), event);
            testDriver.pipeInput(record);
        }
    }

    public <T> void sendStringMessage(T key, String value, String topic) {
        ConsumerRecordFactory<T, String> factory = new ConsumerRecordFactory<>(
                (Serializer<T>)Serdes.serdeFrom(key.getClass()).serializer(), new StringSerializer());
        ConsumerRecord<byte[], byte[]> record = factory.create(topic, key, value);
        testDriver.pipeInput(record);
    }

    public <K, V> ProducerRecord<K, V> read(String topic, Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer) {
        return testDriver.readOutput(topic, keyDeserializer, valueDeserializer);
    }

    public <K, V> KeyValueStore<K, V> getStore(String name) {
        KeyValueStore<K, V> store = testDriver.getKeyValueStore(name);

        if (store == null) {
            throw new IllegalArgumentException("Store not found: " + name);
        }
        return store;
    }

    public void close() throws IOException {
        if (testDriver != null) {
            try {
                testDriver.close();
            } catch (StreamsException e) {
                // temporary workaround for https://github.com/apache/kafka/pull/4713
                if (!(e.getCause() instanceof DirectoryNotEmptyException)) {
                    throw e;
                }
            }
        }
        FileSystemUtils.deleteRecursively(kafkaTempDir);
    }

    public int count(ReadOnlyKeyValueStore store) {
        int count = 0;
        KeyValueIterator<?, ?> iterator;

        for (iterator = store.all(); iterator.hasNext(); iterator.next()) {
            count++;
        }
        iterator.close();
        return count;
    }

    private <T> T[] load(URL resource, Class<T> type) {
        Objects.requireNonNull(resource, "Null resource");

        ObjectMapper mapper = new ObjectMapper();
        // noinspection deprecation
        mapper.registerModule(new JSR310Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Class<T[]> arrayType = (Class<T[]>) Array.newInstance(type, 0).getClass();

        try {
            return mapper.readValue(resource.toURI().toURL(), arrayType);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
