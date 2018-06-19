package org.djar.football.ui.controller;

import org.djar.football.model.view.MatchScore;
import org.djar.football.model.view.PlayerCards;
import org.djar.football.model.view.PlayerGoals;
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
    private final StateStoreRepository<PlayerCards> playerCardsRepo;
    private final StateStoreRepository<PlayerGoals> playerGoalsRepo;

    public StatisticsController(StateStoreRepository<MatchScore> matchScoreRepo,
            StateStoreRepository<TeamRanking> teamRankingRepo,
            StateStoreRepository<PlayerGoals> playerGoalsRepo,
            StateStoreRepository<PlayerCards> playerCardsRepo) {
        this.matchScoreRepo = matchScoreRepo;
        this.teamRankingRepo = teamRankingRepo;
        this.playerGoalsRepo = playerGoalsRepo;
        this.playerCardsRepo = playerCardsRepo;
    }

    @GetMapping("/matchScores")
    public Flux<MatchScore> getMatchScores() {
        return matchScoreRepo.findAll();
    }

    @GetMapping("/rankings")
    public Flux<TeamRanking> getRankings() {
        return teamRankingRepo.findAll();
    }

    @GetMapping("/goals")
    public Flux<PlayerGoals> getGoals() {
        return playerGoalsRepo.findAll();
    }

    @GetMapping("/cards")
    public Flux<PlayerCards> getCards() {
        return playerCardsRepo.findAll();
    }
}
