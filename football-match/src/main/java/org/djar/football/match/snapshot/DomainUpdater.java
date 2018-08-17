package org.djar.football.match.snapshot;

import static org.djar.football.stream.StreamsUtils.addProcessor;
import static org.djar.football.stream.StreamsUtils.addStore;

import java.util.Objects;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueStore;
import org.djar.football.match.domain.Season;
import org.djar.football.match.domain.Match;
import org.djar.football.match.domain.Player;
import org.djar.football.match.repo.SeasonRepository;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.MatchFinished;
import org.djar.football.model.event.MatchScheduled;
import org.djar.football.model.event.MatchStarted;
import org.djar.football.model.event.PlayerStartedCareer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainUpdater {

    private static final Logger logger = LoggerFactory.getLogger(DomainUpdater.class);

    public static final String MATCH_STORE = "match_store";
    public static final String PLAYER_STORE = "player_store";

    private final SeasonRepository seasonRepository;

    public DomainUpdater(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    public void init(Topology topology) {
        addProcessor(topology, MatchScheduled.class, (eventId, event, store) -> {
            Season season = seasonRepository.getDefault();
            Match match = season.scheduleMatch(event.getMatchId(), event.getDate(), event.getHomeClubId(),
                    event.getAwayClubId());
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, MatchStarted.class, (eventId, event, store) -> {
            Match match = findMatch(store, event.getMatchId());
            match.start();
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, GoalScored.class, (eventId, event, store) -> {
            Match match = findMatch(store, event.getMatchId());
            match.newGoal(event.getGoalId(), event.getMinute(), event.getScorerId(),
                    event.getScoredFor());
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, CardReceived.class, (eventId, event, store) -> {
            Match match = findMatch(store, event.getMatchId());

            if (event.getType() == CardReceived.Type.RED) {
                match.newRedCard(event.getCardId(), event.getMinute(), event.getReceiverId());
            } else if (event.getType() == CardReceived.Type.YELLOW) {
                match.newYellowCard(event.getCardId(), event.getMinute(), event.getReceiverId());
            } else {
                throw new IllegalArgumentException("Invalid card type: " + event.getType());
            }
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, MatchFinished.class, (eventId, event, store) -> {
            Match match = (Match)Objects.requireNonNull(store.get(event.getMatchId()),
                    "Match not found: " + event.getMatchId());
            match.finish();
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, PlayerStartedCareer.class, (eventId, event, store) -> {
            Season season = seasonRepository.getDefault();
            Player player = season.startCareer(event.getPlayerId(), event.getName());
            store.put(player.getId(), player);
        }, PLAYER_STORE);

        addStore(topology, Match.class, MATCH_STORE, new Class[] {
                MatchScheduled.class, MatchStarted.class, MatchFinished.class, GoalScored.class, CardReceived.class});
        addStore(topology, Player.class, PLAYER_STORE, PlayerStartedCareer.class);
    }

    private Match findMatch(KeyValueStore<String, Object> store, String matchId) {
        return (Match)Objects.requireNonNull(store.get(matchId), "Match not found: " + matchId);
    }
}
