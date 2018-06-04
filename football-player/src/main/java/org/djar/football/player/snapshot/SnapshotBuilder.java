package org.djar.football.player.snapshot;

import static org.djar.football.stream.StreamsUtils.addProcessor;
import static org.djar.football.stream.StreamsUtils.addStore;

import org.apache.kafka.streams.Topology;
import org.djar.football.event.PlayerStartedCareer;
import org.djar.football.player.domain.Player;

public class SnapshotBuilder {

    public static final String PLAYER_STORE = "player_store";

    public void init(Topology topology) {
        addProcessor(topology, PlayerStartedCareer.class, (eventId, event, store) -> {
            Player player = new Player(event.getPlayerId(), event.getName());
            store.put(player.getId(), player);
        }, PLAYER_STORE);

        addStore(topology, Player.class, PLAYER_STORE, PlayerStartedCareer.class);
    }
}
