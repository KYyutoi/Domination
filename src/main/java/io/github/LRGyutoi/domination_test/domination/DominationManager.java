package io.github.LRGyutoi.domination_test.domination;

import io.github.LRGyutoi.domination_test.Domination_test;
import io.github.LRGyutoi.domination_test.config.ConfigManager;
import io.github.LRGyutoi.domination_test.team.Team;
import io.github.LRGyutoi.domination_test.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 陣地取りゲームを管理するクラス
 */
public class DominationManager {
    
    private final Domination_test plugin;
    private final ConfigManager configManager;
    private final TeamManager teamManager;
    
    private final Map<String, CapturePoint> capturePoints = new HashMap<>();
    private BukkitRunnable updateTask;
    private boolean gameActive = false;
    private int tickCounter = 0;
    
    public DominationManager(Domination_test plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.teamManager = TeamManager.getInstance();
    }
    
    public void addCapturePoint(String id, String displayName, Location location, double radius) {
        CapturePoint point = new CapturePoint(id, displayName, location, radius);
        capturePoints.put(id, point);
        plugin.getLogger().info("拠点を追加しました: " + displayName + " at " + location);
    }
    
    public CapturePoint getCapturePoint(String id) {
        return capturePoints.get(id);
    }
    
    public Map<String, CapturePoint> getAllCapturePoints() {
        return capturePoints;
    }
    
    public void startGame() {
        if (gameActive) {
            plugin.getLogger().warning("ゲームは既に開始されています");
            return;
        }
        
        gameActive = true;
        
        // 初期チケット数を設定
        teamManager.setInitialTickets(configManager.getInitialTickets());
        teamManager.resetGame();
        
        // すべての拠点をリセット
        for (CapturePoint point : capturePoints.values()) {
            point.reset();
        }
        
        Bukkit.broadcast(Component.text("陣地取りゲームが開始されました！", NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text("拠点を占領してチケットを削りましょう！", NamedTextColor.YELLOW));
        
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameActive) {
                    this.cancel();
                    return;
                }
                
                updateCapturePoints();
                tickCounter++;
                
                // チケットドレイン
                if (tickCounter >= configManager.getTicketDrainInterval()) {
                    tickCounter = 0;
                    drainTickets();
                    updateScoreboard();
                }
                
                // ゲーム終了チェック
                if (teamManager.isGameOver()) {
                    endGame();
                }
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 1L);
    }
    
    public void stopGame() {
        if (!gameActive) return;
        
        gameActive = false;
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        Bukkit.broadcast(Component.text("陣地取りゲームが停止されました", NamedTextColor.RED));
    }
    
    private void updateCapturePoints() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        for (CapturePoint point : capturePoints.values()) {
            point.update(onlinePlayers);
        }
    }
    
    private void drainTickets() {
        int redPoints = 0;
        int bluePoints = 0;
        
        for (CapturePoint point : capturePoints.values()) {
            Team owner = point.getOwningTeam();
            if (owner == Team.RED) {
                redPoints++;
            } else if (owner == Team.BLUE) {
                bluePoints++;
            }
        }
        
        // 占領している拠点の差分だけチケットを減らす
        if (redPoints > bluePoints) {
            int drain = redPoints - bluePoints;
            teamManager.reduceTickets(Team.BLUE, drain);
        } else if (bluePoints > redPoints) {
            int drain = bluePoints - redPoints;
            teamManager.reduceTickets(Team.RED, drain);
        }
    }
    
    private void updateScoreboard() {
        int redTickets = teamManager.getTickets(Team.RED);
        int blueTickets = teamManager.getTickets(Team.BLUE);
        
        Component message = Component.text()
            .append(Component.text("赤: " + redTickets, NamedTextColor.RED))
            .append(Component.text(" | ", NamedTextColor.GRAY))
            .append(Component.text("青: " + blueTickets, NamedTextColor.BLUE))
            .build();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(message);
        }
    }
    
    private void endGame() {
        gameActive = false;
        Team winner = teamManager.getWinner();
        
        Bukkit.broadcast(Component.text("================================", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("ゲーム終了！", NamedTextColor.GREEN));
        Bukkit.broadcast(Component.text(winner.getDisplayName() + " チームの勝利！"));
        Bukkit.broadcast(Component.text("================================", NamedTextColor.GOLD));
        
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
    
    public boolean isGameActive() {
        return gameActive;
    }
    
    public void removeCapturePoint(String id) {
        capturePoints.remove(id);
    }
    
    public void clearAllCapturePoints() {
        capturePoints.clear();
    }
}
