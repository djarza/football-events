package org.djar.football.stream;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

public class JsonNodeSerdeTest {

    @Test
    public void deserialize() throws Exception {
        byte[] json = StreamUtils.copyToByteArray(getClass().getResourceAsStream("JsonNodeSerdeTest.json"));
        JsonNode node = new JsonNodeSerde().deserialize(null, json);

        assertThat(node.get("schema").get("name").textValue()).isEqualTo("fb-connect.public.players.Envelope");
        assertThat(node.get("payload").get("before").textValue()).isNull();
        assertThat(node.get("payload").get("after").get("id").intValue()).isEqualTo(1);
        assertThat(node.get("payload").get("after").get("name").textValue()).isEqualTo("Player One");
        assertThat(node.get("payload").get("op").textValue()).isEqualTo("c");
    }
}
