package org.djar.football.model.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TopPlayers {

    private List<PlayerGoals> players;
    private int limit;

    private TopPlayers() {
    }

    public TopPlayers(int limit) {
        this.limit = limit;
        this.players = new ArrayList<>(limit + 1);
    }

    public TopPlayers aggregate(PlayerGoals playerGoals) {
        upsert(playerGoals);
        players.sort(Comparator.comparingInt(PlayerGoals::getGoals).reversed());

        if (players.size() > limit) {
            players.remove(limit); // remove last
        }
        return this;
    }

    private void upsert(PlayerGoals playerGoals) {
        for (PlayerGoals player : players) {
            if (player.getPlayerId().equals(playerGoals.getPlayerId())) {
                player.setGoals(playerGoals.getGoals());
                return;
            }
        }
        players.add(playerGoals);
    }

    public List<PlayerGoals> getPlayers() {
        return players;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String toString() {
        return players.toString();
    }
}