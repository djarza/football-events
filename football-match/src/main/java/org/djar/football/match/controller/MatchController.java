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
import org.djar.football.repo.ReadOnlyKeyValueStoreRepository;
import org.djar.football.stream.EventPublisher;
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
    private final ReadOnlyKeyValueStoreRepository<Match> matchRepository;
    private final ReadOnlyKeyValueStoreRepository<Player> playerRepository;

    public MatchController(EventPublisher publisher, ReadOnlyKeyValueStoreRepository<Match> matchRepository,
            ReadOnlyKeyValueStoreRepository<Player> playerRepository) {
        this.publisher = publisher;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
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
            Match match = matchRepository.find(matchId)
                    .orElseThrow(() -> new NotFoundException("Match not found", matchId));
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
        }).flatMap(publisher::fire);
    }

    @PostMapping("/matches/{matchId}/homeGoals")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> scoreGoalForHomeTeam(@PathVariable String matchId, @RequestBody GoalRequest goalReq) {
        return Mono.<Event>create(sink -> {
            Match match = findRelatedMatch(matchId);
            Player scorer = playerRepository.find(goalReq.getScorerId())
                    .orElseThrow(() -> new NotFoundException("Player not found", goalReq.getScorerId()));
            GoalScored event = new GoalScored(goalReq.getId(), matchId, goalReq.getMinute(), scorer.getId(),
                    match.getHomeTeam().getClubId());
            sink.success(event);
        }).flatMap(publisher::fire);
    }

    @PostMapping("/matches/{matchId}/awayGoals")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> scoreGoalForAwayTeam(@PathVariable String matchId, @RequestBody GoalRequest goalReq) {
        return Mono.<Event>create(sink -> {
            Match match = findRelatedMatch(matchId);
            Player scorer = playerRepository.find(goalReq.getScorerId())
                    .orElseThrow(() -> new NotFoundException("Player not found", goalReq.getScorerId()));
            GoalScored event = new GoalScored(goalReq.getId(), matchId, goalReq.getMinute(), scorer.getId(),
                    match.getAwayTeam().getClubId());
            sink.success(event);
        }).flatMap(publisher::fire);
    }

    @PostMapping("/matches/{matchId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> receiveCard(@PathVariable String matchId, @RequestBody CardRequest cardReq) {
        return Mono.<Event>create(sink -> {
            Match match = findRelatedMatch(matchId);
            Player receiver = playerRepository.find(cardReq.getReceiverId())
                    .orElseThrow(() -> new NotFoundException("Player not found", cardReq.getReceiverId()));
            CardReceived event = new CardReceived(cardReq.getId(), matchId, cardReq.getMinute(), receiver.getId(),
                    CardReceived.Type.valueOf(cardReq.getType()));
            sink.success(event);
        }).flatMap(publisher::fire);
    }

    private Match findRelatedMatch(String matchId) {
        Match match = matchRepository.find(matchId)
                .orElseThrow(() -> new InvalidRequestExeption("Match not found " + matchId));

        if (match.getState() != State.STARTED) {
            throw new InvalidRequestExeption("Match state must be STARTED instead of " + match.getState());
        }
        return match;
    }
}
