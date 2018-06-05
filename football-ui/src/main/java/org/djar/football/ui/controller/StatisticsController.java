package org.djar.football.ui.controller;

import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerStatistic;
import org.djar.football.model.view.TeamRanking;
import org.djar.football.repo.StateStoreRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/ui", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsController {

    private final StateStoreRepository<MatchScore> matchScoreRepo;
    private final StateStoreRepository<TeamRanking> teamRankingRepo;
    private final StateStoreRepository<PlayerStatistic> playerStatisticRepo;

    public StatisticsController(StateStoreRepository<MatchScore> matchScoreRepo,
            StateStoreRepository<TeamRanking> teamRankingRepo,
            StateStoreRepository<PlayerStatistic> playerStatisticRepo) {
        this.matchScoreRepo = matchScoreRepo;
        this.teamRankingRepo = teamRankingRepo;
        this.playerStatisticRepo = playerStatisticRepo;
    }

    @GetMapping("/matchScores")
    public Flux<MatchScore> getMatchScores() {
        return matchScoreRepo.findAll();
    }

    @GetMapping("/rankings")
    public Flux<TeamRanking> getRankings() {
        return teamRankingRepo.findAll();
    }

    @GetMapping("/players")
    public Flux<PlayerStatistic> getPlayers() {
        return playerStatisticRepo.findAll();
    }
}
