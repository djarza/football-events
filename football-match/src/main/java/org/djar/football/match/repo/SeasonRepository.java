package org.djar.football.match.repo;

import org.djar.football.match.domain.Season;
import org.springframework.util.ReflectionUtils;

public class SeasonRepository {

    private final Season defaultSeason = create("1", "Season 1");

    public Season getDefault() {
        return defaultSeason;
    }

    // create instance using reflection in order not to create public constructor
    private static Season create(String id, String name) {
        try {
            return ReflectionUtils.accessibleConstructor(Season.class, String.class, String.class)
                    .newInstance(id, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
