package org.djar.football.match.snapshot;

import static org.djar.football.stream.StreamsUtils.addProcessor;
import static org.djar.football.stream.StreamsUtils.addStore;

import java.util.Objects;
import org.apache.kafka.streams.Topology;
import org.djar.football.match.domain.Card;
import org.djar.football.match.domain.Goal;
import org.djar.football.match.domain.Match;
import org.djar.football.match.domain.Player;
import org.djar.football.match.domain.Team;
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
    public static final String GOAL_STORE = "goal_store";
    public static final String CARD_STORE = "card_store";
    public static final String PLAYER_STORE = "player_store";

    public void init(Topology topology) {
        addProcessor(topology, MatchScheduled.class, (eventId, event, store) -> {
            Match match = new Match(event.getMatchId(), event.getDate(), new Team(event.getHomeClubId()),
                    new Team(event.getAwayClubId()));
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, MatchStarted.class, (eventId, event, store) -> {
            Match match = (Match)Objects.requireNonNull(store.get(event.getMatchId()),
                    "Match not found: " + event.getMatchId());
            match.setState(Match.State.STARTED);
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, GoalScored.class, (eventId, event, store) -> {
            Goal goal = new Goal(event.getGoalId(), event.getMatchId(), event.getMinute(), event.getScorerId(),
                    event.getScoredFor());
            store.put(goal.getId(), goal);
        }, GOAL_STORE);

        addProcessor(topology, CardReceived.class, (eventId, event, store) -> {
            Card card = new Card(event.getCardId(), event.getMatchId(), event.getMinute(), event.getReceiverId(),
                    Card.Type.valueOf(event.getType().name()));
            store.put(card.getId(), card);
        }, CARD_STORE);

        addProcessor(topology, MatchFinished.class, (eventId, event, store) -> {
            Match match = (Match)Objects.requireNonNull(store.get(event.getMatchId()),
                    "Match not found: " + event.getMatchId());
            match.setState(Match.State.FINISHED);
            store.put(match.getId(), match);
        }, MATCH_STORE);

        addProcessor(topology, PlayerStartedCareer.class, (eventId, event, store) -> {
            Player player = new Player(event.getPlayerId(), event.getName());
            store.put(player.getId(), player);
        }, PLAYER_STORE);

        addStore(topology, Match.class, MATCH_STORE,
                new Class[] {MatchScheduled.class, MatchStarted.class, MatchFinished.class});
        addStore(topology, Goal.class, GOAL_STORE, GoalScored.class);
        addStore(topology, Card.class, CARD_STORE, CardReceived.class);
        addStore(topology, Player.class, PLAYER_STORE, PlayerStartedCareer.class);
    }
}
