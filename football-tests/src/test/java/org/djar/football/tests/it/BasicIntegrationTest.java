package org.djar.football.tests.it;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.Arrays;
import java.util.Comparator;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.MatchFinished;
import org.djar.football.model.event.MatchScheduled;
import org.djar.football.model.event.MatchStarted;
import org.djar.football.model.event.PlayerStartedCareer;
import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerStatistic;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.tests.FootballEcosystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class BasicIntegrationTest {

    @Rule
    public Errors errors = new Errors();

    private static FootballEcosystem fbApp;

    @BeforeClass
    public static void setup()  {
        fbApp = new FootballEcosystem();
        fbApp.start();
    }

    @AfterClass
    public static void cleanup() {
        // shutdown only if there were no errors
        if (fbApp.isStarted() && Errors.count() == 0) {
            fbApp.shutdown();
        }
    }

    @Test
    public void playAMatch() throws Exception {
        // no matches and no scores
        fbApp.query("http://football-ui:18080/ui/matchScores", MatchScore[].class, 0);

        // create some players
        fbApp.insertPlayer(101, "Player101");
        fbApp.insertPlayer(102, "Player102");
        fbApp.insertPlayer(103, "Player103");
        fbApp.waitForEvents(PlayerStartedCareer.class, 3);

        // schedule a new match
        assertThat(fbApp.command("http://football-match:18081/command/matches",
            POST, "{\"id\":\"m1\", \"seasonId\":\"s1\", \"matchDate\":\"2018-05-26T15:00:00\"," +
                "\"homeClubId\":\"Man Utd\", \"awayClubId\":\"Liverpool\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(ACCEPTED);
        // the request is processed asynchronously, so wait for the right waitForEvent before the next step
        assertThat(fbApp.waitForEvent(MatchScheduled.class).getAggId()).isEqualTo("m1");

        // change match status from SCHEDULED to STARTED
        assertThat(fbApp.command("http://football-match:18081/command/matches/m1",
            PATCH, "{\"newState\":\"STARTED\", \"reqTimestamp\":\"" + now() + "\"}", NOT_FOUND)).isEqualTo(ACCEPTED);
        // waitForEvent and the initial score 0:0
        assertThat(fbApp.waitForEvent(MatchStarted.class).getAggId()).isEqualTo("m1");
        assertThat(fbApp.waitForWebSocketEvent(MatchScore.class).getHomeGoals()).isEqualTo(0);

        goalsAndCards();

        // finish the match
        assertThat(fbApp.command("http://football-match:18081/command/matches/m1",
            PATCH, "{\"newState\":\"FINISHED\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(ACCEPTED);
        assertThat(fbApp.waitForEvent(MatchFinished.class).getAggId()).isEqualTo("m1");
        fbApp.waitForWebSocketEvent(TeamRanking.class, 2);

        statistics();
    }

    private void goalsAndCards() {
        // some goals and cards during the match
        assertThat(fbApp.command("http://football-match:18081/command/matches/m1/homeGoals",
            POST, "{\"id\":\"g1\", \"minute\":20, \"scorerId\":\"101\", \"reqTimestamp\":\""
                + now() + "\"}", UNPROCESSABLE_ENTITY)).isEqualTo(ACCEPTED);
        assertThat(fbApp.waitForEvent(GoalScored.class).getAggId()).isEqualTo("m1");
        assertThat(fbApp.waitForWebSocketEvent(MatchScore.class).getHomeGoals()).isEqualTo(1);
        assertThat(fbApp.waitForWebSocketEvent(PlayerStatistic.class).getGoals()).isEqualTo(1);

        assertThat(fbApp.command("http://football-match:18081/command/matches/m1/awayGoals",
            POST, "{\"id\":\"g2\", \"minute\":30, \"scorerId\":\"102\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(ACCEPTED);
        assertThat(fbApp.waitForEvent(GoalScored.class).getAggId()).isEqualTo("m1");
        assertThat(fbApp.waitForWebSocketEvent(MatchScore.class).getAwayGoals()).isEqualTo(1);
        assertThat(fbApp.waitForWebSocketEvent(PlayerStatistic.class).getGoals()).isEqualTo(1);

        assertThat(fbApp.command("http://football-match:18081/command/matches/m1/cards",
            POST, "{\"id\":\"c1\", \"minute\":40, \"receiverId\":\"102\", \"type\":\"YELLOW\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(ACCEPTED);
        assertThat(fbApp.waitForEvent(CardReceived.class).getMatchId()).isEqualTo("m1");
        assertThat(fbApp.waitForWebSocketEvent(PlayerStatistic.class).getYellowCards()).isEqualTo(1);

        assertThat(fbApp.command("http://football-match:18081/command/matches/m1/cards",
            POST, "{\"id\":\"c1\", \"minute\":40, \"receiverId\":\"103\", \"type\":\"RED\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(ACCEPTED);
        assertThat(fbApp.waitForEvent(CardReceived.class).getAggId()).isEqualTo("m1");
        assertThat(fbApp.waitForWebSocketEvent(PlayerStatistic.class).getRedCards()).isEqualTo(1);

        assertThat(fbApp.command("http://football-match:18081/command/matches/m1/homeGoals",
            POST, "{\"id\":\"g3\", \"minute\":50, \"scorerId\":\"101\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(ACCEPTED);
        assertThat(fbApp.waitForEvent(GoalScored.class).getAggId()).isEqualTo("m1");
        assertThat(fbApp.waitForWebSocketEvent(MatchScore.class).getHomeGoals()).isEqualTo(2);
        assertThat(fbApp.waitForWebSocketEvent(PlayerStatistic.class).getGoals()).isEqualTo(2);
    }

    private void statistics() throws Exception {
        // check the score
        MatchScore[] matchScores = fbApp.query("http://football-ui:18080/ui/matchScores", MatchScore[].class, 1);
        assertThat(matchScores[0].getHomeClubId()).isEqualTo("Man Utd");
        assertThat(matchScores[0].getAwayClubId()).isEqualTo("Liverpool");
        assertThat(matchScores[0].getHomeGoals()).isEqualTo(2);
        assertThat(matchScores[0].getAwayGoals()).isEqualTo(1);

        // check players
        PlayerStatistic[] playerStats = fbApp.query("http://football-ui:18080/ui/players", PlayerStatistic[].class, 3);
        Arrays.sort(playerStats, Comparator.comparing(PlayerStatistic::getPlayerName));
        assertThat(playerStats[0].getGoals()).isEqualTo(2);
        assertThat(playerStats[0].getYellowCards()).isEqualTo(0);
        assertThat(playerStats[0].getRedCards()).isEqualTo(0);
        assertThat(playerStats[1].getGoals()).isEqualTo(1);
        assertThat(playerStats[1].getYellowCards()).isEqualTo(1);
        assertThat(playerStats[1].getRedCards()).isEqualTo(0);
        assertThat(playerStats[2].getGoals()).isEqualTo(0);
        assertThat(playerStats[2].getYellowCards()).isEqualTo(0);
        assertThat(playerStats[2].getRedCards()).isEqualTo(1);

        // check teams
        TeamRanking[] teamRankings = fbApp.query("http://football-ui:18080/ui/rankings", TeamRanking[].class, 2);
        Arrays.sort(teamRankings, Comparator.comparing(TeamRanking::getClubId));
        assertThat(teamRankings[0].getGoalsFor()).isEqualTo(1);
        assertThat(teamRankings[0].getGoalsAgainst()).isEqualTo(2);
        assertThat(teamRankings[0].getWon()).isEqualTo(0);
        assertThat(teamRankings[1].getGoalsFor()).isEqualTo(2);
        assertThat(teamRankings[1].getGoalsAgainst()).isEqualTo(1);
        assertThat(teamRankings[1].getWon()).isEqualTo(1);
    }

    @Test
    public void startNonExistentMatch() {
        assertThat(fbApp.command("http://football-match:18081/command/matches/FAKE_MATCH",
            PATCH, "{\"newState\":\"STARTED\", \"reqTimestamp\":\"" + now() + "\"}")).isEqualTo(NOT_FOUND);
    }

    @Test
    public void scoreGoalInNonExistentMatch() {
        assertThat(fbApp.command("http://football-match:18081/command/matches/FAKE_MATCH/homeGoals",
            POST, "{\"id\":\"g100\", \"minute\":20, \"scorerId\":\"101\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(NOT_FOUND);
    }

    @Test
    public void scoreGoalInNotStartedMatch() {
        assertThat(fbApp.command("http://football-match:18081/command/matches",
            POST, "{\"id\":\"NOT_STARTED_MATCH\", \"seasonId\":\"s1\", \"matchDate\":\"2018-05-26T15:00:00\"," +
                "\"homeClubId\":\"Man City\", \"awayClubId\":\"Chelsea\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(ACCEPTED);

        assertThat(fbApp.waitForEvent(MatchScheduled.class).getAggId()).isEqualTo("NOT_STARTED_MATCH");

        assertThat(fbApp.command("http://football-match:18081/command/matches/NOT_STARTED_MATCH/homeGoals",
            POST, "{\"id\":\"g1000\", \"minute\":10, \"scorerId\":\"101\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(UNPROCESSABLE_ENTITY);
    }
}
