package io.github.LRGyutoi.domination_test;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DominationManager {
    private final Domination_test plugin;
    private final Map<String, CapturePoint> capturePoints = new HashMap<>();
    private BukkitRunnable updateTask;
    private boolean gameActive = false;
    private static final int TICKET_DRAIN_INTERVAL = 20; // 1秒ごと
    private int tickCounter = 0;

    public DominationManager(Domination_test plugin) {
        this.plugin = plugin;
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
        TeamManager.getInstance().resetGame();

        Bukkit.broadcastMessage("§a§l陣地取りゲームが開始されました！");
        Bukkit.broadcastMessage("§e拠点を占領してチケットを削りましょう！");

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameActive) {
                    this.cancel();
                    return;
                }

                updateCapturePoints();
                tickCounter++;

                // 1秒ごとにチケットを減らす
                if (tickCounter >= TICKET_DRAIN_INTERVAL) {
                    tickCounter = 0;
                    drainTickets();
                    updateScoreboard();
                }

                // ゲーム終了チェック
                if (TeamManager.getInstance().isGameOver()) {
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

        Bukkit.broadcastMessage("§c§l陣地取りゲームが停止されました");
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

        // 各チームが占領している拠点を数える
        for (CapturePoint point : capturePoints.values()) {
            Team owner = point.getOwningTeam();
            if (owner == Team.RED) {
                redPoints++;
            } else if (owner == Team.BLUE) {
                bluePoints++;
            }
        }

        // 占領されている拠点の数に応じてチケットを減らす
        if (redPoints > bluePoints) {
            int drain = redPoints - bluePoints;
            TeamManager.getInstance().reduceTickets(Team.BLUE, drain);
        } else if (bluePoints > redPoints) {
            int drain = bluePoints - redPoints;
            TeamManager.getInstance().reduceTickets(Team.RED, drain);
        }
    }

    private void updateScoreboard() {
        int redTickets = TeamManager.getInstance().getTickets(Team.RED);
        int blueTickets = TeamManager.getInstance().getTickets(Team.BLUE);

        String message = String.format("§c赤: %d §7| §9青: %d", redTickets, blueTickets);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(message);
        }
    }

    private void endGame() {
        gameActive = false;
        Team winner = TeamManager.getInstance().getWinner();

        Bukkit.broadcastMessage("§6§l================================");
        Bukkit.broadcastMessage("§a§lゲーム終了！");
        Bukkit.broadcastMessage(winner.getDisplayName() + " §lチームの勝利！");
        Bukkit.broadcastMessage("§6§l================================");

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

