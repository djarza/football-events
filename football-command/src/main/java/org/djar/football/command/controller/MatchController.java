package org.djar.football.command.controller;

import static org.djar.football.command.domain.Match.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.command.domain.Goal;
import org.djar.football.command.domain.Match;
import org.djar.football.command.domain.Player;
import org.djar.football.command.eventstore.EventPublisher;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchScheduled;
import org.djar.football.event.MatchStarted;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/command", produces = MediaType.APPLICATION_JSON_VALUE)
public class MatchController {

    private final EventPublisher publisher;
    private final KafkaStreams kafkaStreams;

    public MatchController(EventPublisher publisher, KafkaStreams kafkaStreams) {
        this.publisher = publisher;
        this.kafkaStreams = kafkaStreams;
    }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public void scheduleMatch(@RequestBody NewMatchRequest match) {
        publisher.fire(new MatchScheduled(match.getId(), match.getSeasonId(), match.getDate(), match.getHomeClubId(),
                match.getAwayClubId()));
    }

    @PatchMapping("/matches/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setMatchState(@PathVariable String matchId, @RequestBody Match.State state) {
        Match match = findMatch(matchId);
        match.validateTransistionTo(state);

        if (state == State.STARTED) {
            publisher.fire(new MatchStarted(matchId, match.getHomeTeam().getClubId(), match.getAwayTeam().getClubId()));
        } else if (state == State.FINISHED) {
            publisher.fire(new MatchFinished(matchId, match.getHomeTeam().getClubId(),
                    match.getAwayTeam().getClubId()));
        } else {
            throw new UnsupportedOperationException("State not implemented yet: " + state);
        }
    }

    @PostMapping("/matches/{matchId}/homeGoals")
    @ResponseStatus(HttpStatus.CREATED)
    public void scoreGoalForHomeTeam(@PathVariable String matchId, @RequestBody NewGoalRequest goalReq) {
        Match match = findMatch(matchId);
        Player scorer = findPlayer(goalReq.getScorerId());
        publisher.fire(new GoalScored(goalReq.getId(), matchId, goalReq.getMinute(), scorer.getId(),
                match.getHomeTeam().getClubId()));
    }

    @PostMapping("/matches/{matchId}/awayGoals")
    @ResponseStatus(HttpStatus.CREATED)
    public void scoreGoalForAwayTeam(@PathVariable String matchId, @RequestBody NewGoalRequest goalReq) {
        Match match = findMatch(matchId);
        Player scorer = findPlayer(goalReq.getScorerId());
        publisher.fire(new GoalScored(goalReq.getId(), matchId, goalReq.getMinute(), scorer.getId(),
                match.getAwayTeam().getClubId()));
    }

    private Match findMatch(String matchId) {
        if (matchId == null) {
            throw new RuntimeException("Null matchId");
        }
        ReadOnlyKeyValueStore<String, Match> store = kafkaStreams.store(
                "match-store", QueryableStoreTypes.<String, Match>keyValueStore());
        Match match = store.get(matchId);

        if (match == null) {
            throw new RuntimeException("Match not found " + matchId);
        }
        return match;
    }

    private Player findPlayer(String playerId) {
        // should be read from store
        return new Player(playerId, playerId);
    }
}
