package org.djar.football.match.controller;

import static org.djar.football.match.domain.Match.State;

import org.djar.football.match.domain.Match;
import org.djar.football.match.domain.Player;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.Event;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.MatchFinished;
import org.djar.football.model.event.MatchScheduled;
import org.djar.football.model.event.MatchStarted;
import org.djar.football.repo.StateStoreRepository;
import org.djar.football.stream.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(MatchController.class);

    private final EventPublisher publisher;
    private final StateStoreRepository<Match> matchRepository;
    private final StateStoreRepository<Player> playerRepository;

    public MatchController(EventPublisher publisher, StateStoreRepository<Match> matchRepository,
            StateStoreRepository<Player> playerRepository) {
        this.publisher = publisher;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
    }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> scheduleMatch(@RequestBody NewMatchRequest request) {
        Event event = new MatchScheduled(request.getId(), request.getSeasonId(), request.getMatchDate(),
                request.getHomeClubId(), request.getAwayClubId())
                .timestamp(request.getReqTimestamp());
        logger.debug("Scheduling a request: {}", event);
        return publisher.fire(event);
    }

    @PatchMapping("/matches/{matchId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> setMatchState(@PathVariable String matchId, @RequestBody MatchStateRequest request) {
        return Mono.<Event>create(sink -> {
            Match.State newState = Match.State.valueOf(request.getNewState());
            Match match = matchRepository.find(matchId).orElseThrow(
                    () -> new NotFoundException("Match not found", matchId));
            match.validateTransistionTo(newState);
            Event event;

            if (newState == State.STARTED) {
                event = new MatchStarted(matchId, match.getHomeTeam().getClubId(), match.getAwayTeam().getClubId());
                logger.debug("Starting the match {}", event);
            } else if (newState == State.FINISHED) {
                event = new MatchFinished(matchId);
                logger.debug("Finishing the match: {}", event);
            } else {
                throw new UnsupportedOperationException("State " + newState + " not implemented yet");
            }
            event.timestamp(request.getReqTimestamp());
            sink.success(event);
        }).flatMap(publisher::fire);
    }

    @PostMapping("/matches/{matchId}/homeGoals")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> scoreGoalForHomeTeam(@PathVariable String matchId, @RequestBody GoalRequest request) {
        return Mono.<Event>create(sink -> {
            Match match = findRelatedMatch(matchId);
            Player scorer = playerRepository.find(request.getScorerId()).orElseThrow(
                () -> new InvalidContentExeption("Player not found", request.getScorerId()));
            Event event = new GoalScored(request.getId(), matchId, request.getMinute(), scorer.getId(),
                    match.getHomeTeam().getClubId())
                    .timestamp(request.getReqTimestamp());
            logger.debug("Scoring a goal for the home team: {}", event);
            sink.success(event);
        }).flatMap(publisher::fire);
    }

    @PostMapping("/matches/{matchId}/awayGoals")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> scoreGoalForAwayTeam(@PathVariable String matchId, @RequestBody GoalRequest request) {
        return Mono.<Event>create(sink -> {
            Match match = findRelatedMatch(matchId);
            Player scorer = playerRepository.find(request.getScorerId()).orElseThrow(
                    () -> new InvalidContentExeption("Player not found", request.getScorerId()));
            Event event = new GoalScored(request.getId(), matchId, request.getMinute(), scorer.getId(),
                    match.getAwayTeam().getClubId())
                    .timestamp(request.getReqTimestamp());
            logger.debug("Scoring a goal for the away team: {}", event);
            sink.success(event);
        }).flatMap(publisher::fire);
    }

    @PostMapping("/matches/{matchId}/cards")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> receiveCard(@PathVariable String matchId, @RequestBody CardRequest request) {
        return Mono.<Event>create(sink -> {
            Match match = findRelatedMatch(matchId);
            Player receiver = playerRepository.find(request.getReceiverId()).orElseThrow(
                    () -> new InvalidContentExeption("Player not found", request.getReceiverId()));
            Event event = new CardReceived(request.getId(), matchId, request.getMinute(), receiver.getId(),
                    CardReceived.Type.valueOf(request.getType()))
                    .timestamp(request.getReqTimestamp());
            logger.debug("Showing a card {}", event);
            sink.success(event);
        }).flatMap(publisher::fire);
    }

    private Match findRelatedMatch(String matchId) {
        Match match = matchRepository.find(matchId).orElseThrow(
                () -> new NotFoundException("Match not found", matchId));

        if (match.getState() != State.STARTED) {
            throw new InvalidContentExeption("Match state must be " + State.STARTED + " instead of "
                    + match.getState(), matchId);
        }
        return match;
    }
}
