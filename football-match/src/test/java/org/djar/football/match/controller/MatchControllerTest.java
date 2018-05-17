package org.djar.football.match.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.djar.football.event.CardReceived;
import org.djar.football.event.GoalScored;
import org.djar.football.event.MatchScheduled;
import org.djar.football.event.MatchStarted;
import org.djar.football.match.domain.Match;
import org.djar.football.match.domain.Player;
import org.djar.football.match.domain.Team;
import org.djar.football.repo.ReadOnlyKeyValueStoreRepository;
import org.djar.football.stream.EventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

public class MatchControllerTest {

    private MatchController controller;
    private ReadOnlyKeyValueStoreRepository<Match> matchRepository;
    private ReadOnlyKeyValueStoreRepository<Player> playerRepository;
    private EventPublisher publisher;

    @Before
    public void setUp() {
        publisher = mock(EventPublisher.class);
        when(publisher.fire(any())).thenReturn(Mono.empty());

        matchRepository = mock(ReadOnlyKeyValueStoreRepository.class);
        playerRepository = mock(ReadOnlyKeyValueStoreRepository.class);
        Match match = new Match("match1", LocalDateTime.now(), new Team("t1"), new Team("t2"));
        match.setState(Match.State.STARTED);
        when(matchRepository.find("match1")).thenReturn(Optional.of(match));
        when(matchRepository.find("FAKE_MATCH")).thenReturn(Optional.empty());
        when(playerRepository.find("player1")).thenReturn(
            Optional.of(new Player("player1", "Player Name")));

        controller = new MatchController(publisher, matchRepository, playerRepository);
    }

    @Test
    public void scheduleMatch() {
        controller.scheduleMatch(new NewMatchRequest("match1", "2017/2018", LocalDateTime.now(), "c1", "c2")).block();

        ArgumentCaptor<MatchScheduled> captor = ArgumentCaptor.forClass(MatchScheduled.class);
        verify(publisher).fire(captor.capture());
        assertThat(captor.getValue().getMatchId()).isEqualTo("match1");
    }

    @Test
    public void setMatchState() {
        when(matchRepository.find("match0")).thenReturn(
                Optional.of(new Match("match0", LocalDateTime.now(), new Team("t1"), new Team("t2"))));
        controller.setMatchState("match0", Match.State.STARTED).block();

        ArgumentCaptor<MatchStarted> captor = ArgumentCaptor.forClass(MatchStarted.class);
        verify(publisher).fire(captor.capture());
        assertThat(captor.getValue().getMatchId()).isEqualTo("match0");
    }

    @Test
    public void scoreGoalForHomeTeam() {
        controller.scoreGoalForHomeTeam("match1", new GoalRequest("goal1", 22, "player1")).block();

        ArgumentCaptor<GoalScored> captor = ArgumentCaptor.forClass(GoalScored.class);
        verify(publisher).fire(captor.capture());
        GoalScored event = captor.getValue();
        assertThat(event.getMatchId()).isEqualTo("match1");
        assertThat(event.getScoredFor()).isEqualTo("t1");
    }

    @Test
    public void scoreGoalForAwayTeam() {
        controller.scoreGoalForAwayTeam("match1", new GoalRequest("goal1", 22, "player1")).block();

        ArgumentCaptor<GoalScored> captor = ArgumentCaptor.forClass(GoalScored.class);
        verify(publisher).fire(captor.capture());
        GoalScored event = captor.getValue();
        assertThat(event.getMatchId()).isEqualTo("match1");
        assertThat(event.getScoredFor()).isEqualTo("t2");
    }

    @Test(expected = InvalidRequestExeption.class)
    public void scoreGoalInNonExistentMatch() {
        controller.scoreGoalForHomeTeam("FAKE_MATCH", new GoalRequest("goal1", 22, "player1")).block();
    }

    @Test
    public void newCardRequest() {
        controller.receiveCard("match1", new CardRequest("card1", "match1", 33, "player1", "RED")).block();

        ArgumentCaptor<CardReceived> captor = ArgumentCaptor.forClass(CardReceived.class);
        verify(publisher).fire(captor.capture());
        CardReceived event = captor.getValue();
        assertThat(event.getMatchId()).isEqualTo("match1");
        assertThat(event.getType()).isEqualTo(CardReceived.Type.RED);
    }
}
