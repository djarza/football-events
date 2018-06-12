package org.djar.football.tests;

import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.Arrays;
import java.util.Comparator;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.djar.football.model.event.CardReceived;
import org.djar.football.model.event.GoalScored;
import org.djar.football.model.event.MatchFinished;
import org.djar.football.model.event.MatchScheduled;
import org.djar.football.model.event.MatchStarted;
import org.djar.football.model.event.PlayerStartedCareer;
import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerStatistic;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.tests.utils.DockerCompose;
import org.djar.football.tests.utils.Errors;
import org.djar.football.tests.utils.WebSocket;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.postgresql.Driver;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.client.RestTemplate;

public class BasicIntegrationTest extends BaseTest {

    @Rule
    public Errors errors = new Errors();

    @BeforeClass
    public static void setup()  {
        dockerCompose = new DockerCompose()
            .addHealthCheck("http://football-match:18081/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://football-player:18082/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://football-view:18083/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://football-ui:18080/actuator/health", "\\{\"status\":\"UP\"\\}")
            .addHealthCheck("http://connect:8083/connectors", "\\[.*\\]"); // match any response

        rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

        webSocket = new WebSocket("ws://football-ui:18080/dashboard");
        webSocket.subscribe("/topic/MatchScore", MatchScore.class);
        webSocket.subscribe("/topic/TeamRanking", TeamRanking.class);
        webSocket.subscribe("/topic/PlayerStatistic", PlayerStatistic.class);

        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 20000);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, BasicIntegrationTest.class.getName());
        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, BasicIntegrationTest.class.getName());

        dockerCompose.up();
        dockerCompose.waitUntilServicesAreAvailable(180, SECONDS);

        webSocket.connect();

        // create database with some initial data
        postgres = new JdbcTemplate(new SimpleDriverDataSource(new Driver(),
            "jdbc:postgresql://postgres:5432/postgres", "postgres", "postgres"));
        createPlayersTable();
        insertPlayer(101, "Player101");
        insertPlayer(102, "Player102");

        createConnector("http://connect:8083/connectors/", "football-connector.json");
    }

    @AfterClass
    public static void cleanup() {
        if (Errors.count() == 0) {
            dockerCompose.down();
        }
        webSocket.disconnect();
    }

    @Test
    public void playAMatch() throws Exception {
        // no matches and no scores
        query("http://football-ui:18080/ui/matchScores", MatchScore[].class, 0);

        // create another player
        insertPlayer(103, "Player103");
        waitForEvents(PlayerStartedCareer.class, 3);

        // schedule a new match
        assertThat(command("http://football-match:18081/command/matches",
            POST, "{\"id\":\"m1\", \"seasonId\":\"s1\", \"matchDate\":\"2018-05-26T15:00:00\"," +
                "\"homeClubId\":\"Man Utd\", \"awayClubId\":\"Liverpool\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(CREATED);
        // the request is processed asynchronously, so wait for the right waitForEvent before the next step
        assertThat(waitForEvent(MatchScheduled.class).getAggId()).isEqualTo("m1");

        // change match status from SCHEDULED to STARTED
        assertThat(command("http://football-match:18081/command/matches/m1",
            PATCH, "{\"newState\":\"STARTED\", \"reqTimestamp\":\"" + now() + "\"}", NOT_FOUND)).isEqualTo(NO_CONTENT);
        // waitForEvent and the initial score 0:0
        assertThat(waitForEvent(MatchStarted.class).getAggId()).isEqualTo("m1");
        assertThat(waitForWebSocket(MatchScore.class).getHomeGoals()).isEqualTo(0);

        goalsAndCards();

        // finish the match
        assertThat(command("http://football-match:18081/command/matches/m1",
            PATCH, "{\"newState\":\"FINISHED\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(NO_CONTENT);
        assertThat(waitForEvent(MatchFinished.class).getAggId()).isEqualTo("m1");
        waitForWebSocket(TeamRanking.class, 2);

        statistics();
    }

    private void goalsAndCards() {
        // some goals and cards during the match
        assertThat(command("http://football-match:18081/command/matches/m1/homeGoals",
            POST, "{\"id\":\"g1\", \"minute\":20, \"scorerId\":\"101\", \"reqTimestamp\":\""
                + now() + "\"}", UNPROCESSABLE_ENTITY)).isEqualTo(CREATED);
        assertThat(waitForEvent(GoalScored.class).getAggId()).isEqualTo("m1");
        assertThat(waitForWebSocket(MatchScore.class).getHomeGoals()).isEqualTo(1);
        assertThat(waitForWebSocket(PlayerStatistic.class).getGoals()).isEqualTo(1);

        assertThat(command("http://football-match:18081/command/matches/m1/awayGoals",
            POST, "{\"id\":\"g2\", \"minute\":30, \"scorerId\":\"102\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(CREATED);
        assertThat(waitForEvent(GoalScored.class).getAggId()).isEqualTo("m1");
        assertThat(waitForWebSocket(MatchScore.class).getAwayGoals()).isEqualTo(1);
        assertThat(waitForWebSocket(PlayerStatistic.class).getGoals()).isEqualTo(1);

        assertThat(command("http://football-match:18081/command/matches/m1/cards",
            POST, "{\"id\":\"c1\", \"minute\":40, \"receiverId\":\"102\", \"type\":\"YELLOW\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(CREATED);
        assertThat(waitForEvent(CardReceived.class).getMatchId()).isEqualTo("m1");
        assertThat(waitForWebSocket(PlayerStatistic.class).getYellowCards()).isEqualTo(1);

        assertThat(command("http://football-match:18081/command/matches/m1/cards",
            POST, "{\"id\":\"c1\", \"minute\":40, \"receiverId\":\"103\", \"type\":\"RED\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(CREATED);
        assertThat(waitForEvent(CardReceived.class).getAggId()).isEqualTo("m1");
        assertThat(waitForWebSocket(PlayerStatistic.class).getRedCards()).isEqualTo(1);

        assertThat(command("http://football-match:18081/command/matches/m1/homeGoals",
            POST, "{\"id\":\"g3\", \"minute\":50, \"scorerId\":\"101\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(CREATED);
        assertThat(waitForEvent(GoalScored.class).getAggId()).isEqualTo("m1");
        assertThat(waitForWebSocket(MatchScore.class).getHomeGoals()).isEqualTo(2);
        assertThat(waitForWebSocket(PlayerStatistic.class).getGoals()).isEqualTo(2);
    }

    private void statistics() throws Exception {
        // check the score
        MatchScore[] matchScores = query("http://football-ui:18080/ui/matchScores", MatchScore[].class, 1);
        assertThat(matchScores[0].getHomeClubId()).isEqualTo("Man Utd");
        assertThat(matchScores[0].getAwayClubId()).isEqualTo("Liverpool");
        assertThat(matchScores[0].getHomeGoals()).isEqualTo(2);
        assertThat(matchScores[0].getAwayGoals()).isEqualTo(1);

        // check players
        PlayerStatistic[] playerStats = query("http://football-ui:18080/ui/players", PlayerStatistic[].class, 3);
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
        TeamRanking[] teamRankings = query("http://football-ui:18080/ui/rankings", TeamRanking[].class, 2);
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
        assertThat(command("http://football-match:18081/command/matches/FAKE_MATCH",
            PATCH, "{\"newState\":\"STARTED\", \"reqTimestamp\":\"" + now() + "\"}")).isEqualTo(NOT_FOUND);
    }

    @Test
    public void scoreGoalInNonExistentMatch() {
        assertThat(command("http://football-match:18081/command/matches/FAKE_MATCH/homeGoals",
            POST, "{\"id\":\"g100\", \"minute\":20, \"scorerId\":\"101\", \"reqTimestamp\":\""
                + now() + "\"}")).isEqualTo(NOT_FOUND);
    }

    @Test
    public void scoreGoalInNotStartedMatch() {
        assertThat(command("http://football-match:18081/command/matches",
            POST, "{\"id\":\"NOT_STARTED_MATCH\", \"seasonId\":\"s1\", \"matchDate\":\"2018-05-26T15:00:00\"," +
                "\"homeClubId\":\"Man City\", \"awayClubId\":\"Chelsea\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(CREATED);

        assertThat(waitForEvent(MatchScheduled.class).getAggId()).isEqualTo("NOT_STARTED_MATCH");

        assertThat(command("http://football-match:18081/command/matches/NOT_STARTED_MATCH/homeGoals",
            POST, "{\"id\":\"g1000\", \"minute\":10, \"scorerId\":\"101\", \"reqTimestamp\":\"" + now() + "\"}"))
                .isEqualTo(UNPROCESSABLE_ENTITY);
    }
}
