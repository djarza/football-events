package org.djar.football.stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JsonNodeSerde implements Serde<JsonNode>, Serializer<JsonNode>, Deserializer<JsonNode> {

    private final ObjectMapper mapper;

    public JsonNodeSerde() {
        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        // noinspection deprecation
        mapper.registerModule(new JSR310Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, JsonNode data) {
        try {
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing " + data.getClass() + " " + data, e);
        }
    }

    @Override
    public JsonNode deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return mapper.readTree(data);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing from " + new String(data), e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<JsonNode> serializer() {
        return this;
    }

    @Override
    public Deserializer<JsonNode> deserializer() {
        return this;
    }
}