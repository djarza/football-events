package org.djar.football.match.repo;

import org.djar.football.match.domain.League;
import org.springframework.util.ReflectionUtils;

public class LeagueRepository {

    private final League defaultLeague = create("1", "Default League");

    public League getDefault() {
        return defaultLeague;
    }

    // create instance using reflection in order not to create public constructor
    private static League create(String id, String name) {
        try {
            return ReflectionUtils.accessibleConstructor(League.class, String.class, String.class)
                    .newInstance(id, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
