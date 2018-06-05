package org.djar.football.player.connect;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.djar.football.Topics;
import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.stream.EventPublisher;
import org.djar.football.stream.JsonNodeSerde;
import org.djar.football.stream.JsonPojoSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(PlayerEventProducer.class);

    private static final String CONNECT_PLAYERS_TOPIC = "fb-connect.public.players";
    private static final String PLAYER_STARTED_CAREER_TOPIC = Topics.eventTopicName(PlayerStartedCareer.class);

    private final EventPublisher eventPublisher;

    public PlayerEventProducer(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void build(StreamsBuilder builder) {
        KStream<byte[], JsonNode> playerSourceStream = builder.stream(
                CONNECT_PLAYERS_TOPIC, Consumed.with(Serdes.ByteArray(), new JsonNodeSerde()))
                .filter((id, json) -> creationOrSnapshot(json));

        playerSourceStream.foreach(this::debug);

        KStream<String, PlayerStartedCareer> playerReadyStream = playerSourceStream.map(
                (id, json) -> {
                    PlayerStartedCareer event = createEvent(json);
                    return KeyValue.pair(event.getAggId(), event);
                });

        playerReadyStream.to(PLAYER_STARTED_CAREER_TOPIC, Produced.with(
                Serdes.String(), new JsonPojoSerde<>(PlayerStartedCareer.class)));
    }

    private void debug(byte[] id, JsonNode json) {
        if (logger.isDebugEnabled()) {
            logger.debug("Message received from topic {}: {}->{}", CONNECT_PLAYERS_TOPIC, new String(id),
                    json.get("schema").get("name").textValue());
        }
    }

    private boolean creationOrSnapshot(JsonNode json) {
        char op = json.get("payload").get("op").textValue().charAt(0);

        // c - create (insert), r - read (in the case of a snapshot)
        if (op == 'c' || op == 'r') {
            return true;
        }
        logger.warn("Unsupported operation type '{}' - skipped", op);
        return false;
    }

    private PlayerStartedCareer createEvent(JsonNode json) {
        JsonNode after = json.get("payload").get("after");
        int playerId = after.get("id").intValue();
        String playerName = after.get("name").textValue();
        PlayerStartedCareer event = new PlayerStartedCareer(String.valueOf(playerId), playerName);
        eventPublisher.fillOut(event);
        logger.debug("New {} event created: {}", event.getClass().getSimpleName(), event.getAggId());
        return event;
    }
}
