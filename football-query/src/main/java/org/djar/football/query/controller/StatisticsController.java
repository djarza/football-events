package org.djar.football.query.controller;

import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.djar.football.query.model.MatchScore;
import org.djar.football.query.model.Ranking;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/query", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsController {

    private final KafkaStreams streams;
    private String matchScoreStoreName = "match-scores-store";
    private String rankingStoreName = "ranking-store";

    public StatisticsController(KafkaStreams streams) {
        this.streams = streams;
    }

    @GetMapping("/matchScores")
    public List<MatchScore> getMatchScores() {
        return read(streams.store(matchScoreStoreName, QueryableStoreTypes.<String, MatchScore>keyValueStore()));
    }

    @GetMapping("/rankings")
    public List<Ranking> getRankings() {
        return read(streams.store(rankingStoreName, QueryableStoreTypes.<String, Ranking>keyValueStore()));
    }

    private <T> List<T> read(ReadOnlyKeyValueStore<String, T> store) {
        KeyValueIterator<String, T> iterator = store.all();
        List<T> result = new ArrayList<>();

        while (iterator.hasNext()) {
            result.add(iterator.next().value);
        }
        iterator.close();
        return result;
    }
}
