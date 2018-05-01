package org.djar.football.command.eventstore;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.djar.football.command.domain.Goal;
import org.djar.football.command.domain.Match;
import org.djar.football.command.domain.Team;
import org.djar.football.event.Event;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchScheduled;
import org.djar.football.event.MatchStarted;
import org.djar.football.stream.JsonPojoSerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandler {

    public static final String MATCH_STORE = "match_store";
    public static final String GOAL_STORE = "goal_store";

    private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);

    public void init(Topology topology) {
        addProcessor(topology, MatchScheduled.class, (eventId, event, store) -> {
            Match match = new Match(event.getMatchId(), event.getStartTime(), new Team(event.getHomeClubId()),
                        new Team(event.getAwayClubId()));
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, MatchStarted.class, (eventId, event, store) -> {
            Match match = (Match)store.get(event.getMatchId());

            if (match == null) {
                throw new RuntimeException("Match not found: " + event.getMatchId());
            }
            match.setState(Match.State.STARTED);
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, GoalScored.class, (eventId, event, store) -> {
            Goal goal = new Goal(event.getGoalId(), event.getMatchId(), event.getMinute(), event.getScorerId(),
                    event.getScoredFor());
            store.put(goal.getId(), goal);
        }, GOAL_STORE);

        addStore(topology, Match.class, MATCH_STORE, new Class[] {MatchScheduled.class, MatchStarted.class});
        addStore(topology, Goal.class, GOAL_STORE, GoalScored.class);
    }

    private <E extends Event> void addProcessor(Topology topology, Class<E> eventType, EventProcessor<E> proc,
            String store) {
        topology.addSource(eventType.getSimpleName() + "Source", Serdes.String().deserializer(),
                new JsonPojoSerde(eventType), Event.eventName(eventType))
                .addProcessor(eventType.getSimpleName() + "Process", () -> new ProcessorWrapper<E>(proc, store),
                eventType.getSimpleName() + "Source");
    }

    private <D, E extends Event> void addStore(Topology topology, Class<D> domainType, String store,
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
    private interface EventProcessor<E extends Event> {

        void process(String eventId, E event, KeyValueStore store);
    }

    private class ProcessorWrapper<E extends Event> extends AbstractProcessor<String, E> {

        private final String storeName;
        private final EventProcessor<E> processor;

        private KeyValueStore<String, Match> store;

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
