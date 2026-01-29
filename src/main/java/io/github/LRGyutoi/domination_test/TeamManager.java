package io.github.LRGyutoi.domination_test;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamManager {
    private static TeamManager instance;
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private int redTickets = 200;
    private int blueTickets = 200;

    private TeamManager() {}

    public static TeamManager getInstance() {
        if (instance == null) {
            instance = new TeamManager();
        }
        return instance;
    }

    public void setPlayerTeam(Player player, Team team) {
        playerTeams.put(player.getUniqueId(), team);
        player.sendMessage("あなたは " + team.getDisplayName() + " チームに参加しました！");
    }

    public Team getPlayerTeam(Player player) {
        return playerTeams.getOrDefault(player.getUniqueId(), Team.NEUTRAL);
    }

    public void removePlayer(UUID playerId) {
        playerTeams.remove(playerId);
    }

    public int getTickets(Team team) {
        return switch (team) {
            case RED -> redTickets;
            case BLUE -> blueTickets;
            case NEUTRAL -> 0;
        };
    }

    public void setTickets(Team team, int tickets) {
        switch (team) {
            case RED -> redTickets = Math.max(0, tickets);
            case BLUE -> blueTickets = Math.max(0, tickets);
        }
    }

    public void reduceTickets(Team team, int amount) {
        setTickets(team, getTickets(team) - amount);
    }

    public void resetGame() {
        redTickets = 200;
        blueTickets = 200;
    }

    public boolean isGameOver() {
        return redTickets <= 0 || blueTickets <= 0;
    }

    public Team getWinner() {
        if (redTickets <= 0) return Team.BLUE;
        if (blueTickets <= 0) return Team.RED;
        return Team.NEUTRAL;
    }

    public void reset() {
        playerTeams.clear();
        resetGame();
    }
}

