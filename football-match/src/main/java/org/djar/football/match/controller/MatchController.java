package org.djar.football.match.controller;

import static org.djar.football.match.domain.Match.State;

import org.djar.football.event.CardReceived;
import org.djar.football.event.Event;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchFinished;
import org.djar.football.event.MatchScheduled;
import org.djar.football.event.MatchStarted;
import org.djar.football.match.domain.Match;
import org.djar.football.match.domain.Player;
import org.djar.football.match.snapshot.SnapshotBuilder;
import org.djar.football.stream.EventPublisher;
import org.djar.football.stream.ReadOnlyKeyValueStoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/command", produces = MediaType.APPLICATION_JSON_VALUE)
public class MatchController {

    private final EventPublisher publisher;
    private final ReadOnlyKeyValueStoreRepository repository;

    public MatchController(EventPublisher publisher, ReadOnlyKeyValueStoreRepository repository) {
        this.publisher = publisher;
        this.repository = repository;
    }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> scheduleMatch(@RequestBody NewMatchRequest match) {
        return publisher.fire(new MatchScheduled(match.getId(), match.getSeasonId(), match.getDate(),
                match.getHomeClubId(), match.getAwayClubId()));
    }

    @PatchMapping("/matches/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> setMatchState(@PathVariable String matchId, @RequestBody Match.State newState) {
        return Mono.<Event>create(sink -> {
            Match match = repository.find(matchId, SnapshotBuilder.MATCH_STORE);
            match.validateTransistionTo(newState);
            Event event;

            if (newState == State.STARTED) {
                event = new MatchStarted(matchId, match.getHomeTeam().getClubId(), match.getAwayTeam().getClubId());
            } else if (newState == State.FINISHED) {
                event = new MatchFinished(matchId, match.getHomeTeam().getClubId(), match.getAwayTeam().getClubId());
            } else {
                throw new UnsupportedOperationException("State " + newState + " not implemented yet");
            }
            sink.success(event);
        }).flatMap(event -> publisher.fire(event));
    }

    @PostMapping("/matches/{matchId}/homeGoals")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> scoreGoalForHomeTeam(@PathVariable String matchId, @RequestBody GoalRequest goalReq) {
        return Mono.<Event>create(sink -> {
            Match match = getMatch(matchId);
            Player scorer = repository.find(goalReq.getScorerId(), SnapshotBuilder.PLAYER_STORE);
            GoalScored event = new GoalScored(goalReq.getId(), matchId, goalReq.getMinute(), scorer.getId(),
                    match.getHomeTeam().getClubId());
            sink.success(event);
        }).flatMap(event -> publisher.fire(event));
    }

    @PostMapping("/matches/{matchId}/awayGoals")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> scoreGoalForAwayTeam(@PathVariable String matchId, @RequestBody GoalRequest goalReq) {
        return Mono.<Event>create(sink -> {
            Match match = getMatch(matchId);
            Player scorer = repository.find(goalReq.getScorerId(), SnapshotBuilder.PLAYER_STORE);
            GoalScored event = new GoalScored(goalReq.getId(), matchId, goalReq.getMinute(), scorer.getId(),
                    match.getAwayTeam().getClubId());
            sink.success(event);
        }).flatMap(event -> publisher.fire(event));
    }

    @PostMapping("/matches/{matchId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> receiveCard(@PathVariable String matchId, @RequestBody NewCardRequest cardReq) {
        return Mono.<Event>create(sink -> {
            Match match = getMatch(matchId);
            Player receiver = repository.find(cardReq.getReceiverId(), SnapshotBuilder.PLAYER_STORE);
            CardReceived event = new CardReceived(cardReq.getId(), matchId, cardReq.getMinute(), receiver.getId(),
                    CardReceived.Type.valueOf(cardReq.getType()));
            sink.success(event);
        }).flatMap(event -> publisher.fire(event));
    }

    private Match getMatch(String matchId) {
        Match match = repository.find(matchId, SnapshotBuilder.MATCH_STORE);

        if (match.getState() != State.STARTED) {
            throw new IllegalStateException("Match state must be STARTED instead of " + match.getState());
        }
        return match;
    }
}
