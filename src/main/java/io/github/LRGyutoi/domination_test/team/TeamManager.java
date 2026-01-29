package io.github.LRGyutoi.domination_test.team;

import io.github.LRGyutoi.domination_test.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * チーム管理クラス
 * プレイヤーのチーム割り当て、チケット管理などを行う
 */
public class TeamManager {
    
    private static TeamManager instance;
    
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private int redTickets = 200;
    private int blueTickets = 200;
    private int initialTickets = 200;
    
    private TeamManager() {}
    
    public static TeamManager getInstance() {
        if (instance == null) {
            instance = new TeamManager();
        }
        return instance;
    }
    
    /**
     * ConfigManagerから初期チケット数を設定
     */
    public void setInitialTickets(int tickets) {
        this.initialTickets = tickets;
    }
    
    public void setPlayerTeam(Player player, Team team) {
        playerTeams.put(player.getUniqueId(), team);
        player.sendMessage("あなたは " + team.getDisplayName() + " チームに参加しました！");
    }
    
    public Team getPlayerTeam(Player player) {
        return playerTeams.getOrDefault(player.getUniqueId(), Team.NEUTRAL);
    }
    
    public Team getPlayerTeam(UUID playerId) {
        return playerTeams.getOrDefault(playerId, Team.NEUTRAL);
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
    
    public void addTickets(Team team, int amount) {
        setTickets(team, getTickets(team) + amount);
    }
    
    public void resetGame() {
        redTickets = initialTickets;
        blueTickets = initialTickets;
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
    
    /**
     * チームのプレイヤーリストを取得
     */
    public List<UUID> getTeamPlayers(Team team) {
        List<UUID> players = new ArrayList<>();
        for (Map.Entry<UUID, Team> entry : playerTeams.entrySet()) {
            if (entry.getValue() == team) {
                players.add(entry.getKey());
            }
        }
        return players;
    }
    
    /**
     * チームのプレイヤー数を取得
     */
    public int getTeamSize(Team team) {
        return (int) playerTeams.values().stream()
            .filter(t -> t == team)
            .count();
    }
    
    /**
     * 人数が少ないチームを取得（自動振り分け用）
     */
    public Team getSmallerTeam() {
        int redSize = getTeamSize(Team.RED);
        int blueSize = getTeamSize(Team.BLUE);
        
        if (redSize <= blueSize) {
            return Team.RED;
        } else {
            return Team.BLUE;
        }
    }
    
    /**
     * プレイヤーをバランス良くチームに振り分け
     */
    public void autoAssignTeam(Player player) {
        Team team = getSmallerTeam();
        setPlayerTeam(player, team);
    }
}
