package org.djar.football.match.domain;

import java.util.HashSet;
import java.util.Set;

public class Team {

    private String clubId;
    private Set<String> memberIds = new HashSet<>();

    private Team() {
    }

    public Team(String clubId) {
        this.clubId = clubId;
    }

    public String getClubId() {
        return clubId;
    }

    public boolean isMember(Player player) {
        return memberIds.contains(player.getId());
    }

    public void addMember(Player player) {
        memberIds.add(player.getId());
    }
}
