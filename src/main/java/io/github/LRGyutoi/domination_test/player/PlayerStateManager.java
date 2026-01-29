package io.github.LRGyutoi.domination_test.player;

import io.github.LRGyutoi.domination_test.Domination_test;
import io.github.LRGyutoi.domination_test.config.ConfigManager;
import io.github.LRGyutoi.domination_test.gun.GunManager;
import io.github.LRGyutoi.domination_test.gun.GunStats;
import io.github.LRGyutoi.domination_test.gun.PlayerGunData;
import io.github.LRGyutoi.domination_test.team.Team;
import io.github.LRGyutoi.domination_test.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤー状態管理クラス
 * リスポーン、死亡処理、状態リセットなどを管理
 */
public class PlayerStateManager implements Listener {
    
    private final Domination_test plugin;
    private final ConfigManager configManager;
    private final TeamManager teamManager;
    private final GunManager gunManager;
    
    // プレイヤーの銃データ
    private final Map<UUID, PlayerGunData> playerGunDataMap = new ConcurrentHashMap<>();
    
    // リスポーン待機中のプレイヤー
    private final Set<UUID> respawningPlayers = ConcurrentHashMap.newKeySet();
    
    // チームスポーンポイント
    private final Map<Team, List<Location>> spawnPoints = new HashMap<>();
    
    // 統計
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    
    public PlayerStateManager(Domination_test plugin, ConfigManager configManager, 
                              TeamManager teamManager, GunManager gunManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.teamManager = teamManager;
        this.gunManager = gunManager;
        
        // デフォルトスポーンポイント初期化
        spawnPoints.put(Team.RED, new ArrayList<>());
        spawnPoints.put(Team.BLUE, new ArrayList<>());
        spawnPoints.put(Team.NEUTRAL, new ArrayList<>());
    }
    
    /**
     * プレイヤーの銃データを取得
     */
    public PlayerGunData getPlayerGunData(UUID playerId) {
        return playerGunDataMap.computeIfAbsent(playerId, PlayerGunData::new);
    }
    
    /**
     * プレイヤー参加時
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // 銃データを初期化
        playerGunDataMap.computeIfAbsent(playerId, PlayerGunData::new);
        
        // 統計を初期化
        playerStats.computeIfAbsent(playerId, k -> new PlayerStats());
    }
    
    /**
     * プレイヤー退出時
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // リスポーン待機から削除
        respawningPlayers.remove(playerId);
        
        // チームから削除
        teamManager.removePlayer(playerId);
    }
    
    /**
     * プレイヤー死亡時
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // 統計更新
        PlayerStats victimStats = getStats(victim.getUniqueId());
        victimStats.incrementDeaths();
        
        if (killer != null && !killer.equals(victim)) {
            PlayerStats killerStats = getStats(killer.getUniqueId());
            killerStats.incrementKills();
        }
        
        // デスメッセージをカスタマイズ（銃システムで処理するため）
        event.deathMessage(null);
        
        // リスポーン処理をスケジュール
        scheduleRespawn(victim);
    }
    
    /**
     * プレイヤーリスポーン時
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // チームスポーンポイントを設定
        Team team = teamManager.getPlayerTeam(player);
        Location spawnLocation = getTeamSpawnPoint(team);
        
        if (spawnLocation != null) {
            event.setRespawnLocation(spawnLocation);
        }
        
        // リスポーン後の状態リセット
        new BukkitRunnable() {
            @Override
            public void run() {
                resetPlayerState(player);
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * リスポーン処理をスケジュール
     */
    private void scheduleRespawn(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (respawningPlayers.contains(playerId)) {
            return; // 既にリスポーン待機中
        }
        
        respawningPlayers.add(playerId);
        
        int respawnDelayTicks = configManager.getRespawnDelayTicks();
        int respawnDelaySeconds = respawnDelayTicks / 20;
        
        // リスポーンカウントダウン
        new BukkitRunnable() {
            int countdown = respawnDelaySeconds;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    respawningPlayers.remove(playerId);
                    this.cancel();
                    return;
                }
                
                if (countdown <= 0) {
                    respawningPlayers.remove(playerId);
                    
                    // リスポーン実行
                    player.spigot().respawn();
                    
                    this.cancel();
                    return;
                }
                
                // カウントダウン表示
                player.showTitle(Title.title(
                    Component.text("リスポーン", NamedTextColor.RED),
                    Component.text(countdown + " 秒", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                ));
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * プレイヤー状態をリセット（リスポーン時）
     */
    public void resetPlayerState(Player player) {
        UUID playerId = player.getUniqueId();
        
        // HP回復
        if (configManager.isRespawnWithFullHealth()) {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
        
        // 弾薬リセット
        if (configManager.isRespawnWithFullAmmo()) {
            resetAllAmmo(playerId);
        }
        
        // エフェクトクリア
        player.getActivePotionEffects().forEach(effect -> 
            player.removePotionEffect(effect.getType())
        );
        
        // 炎を消す
        player.setFireTicks(0);
        
        player.sendMessage(Component.text("リスポーンしました！", NamedTextColor.GREEN));
    }
    
    /**
     * すべての銃の弾薬をリセット
     */
    public void resetAllAmmo(UUID playerId) {
        PlayerGunData data = getPlayerGunData(playerId);
        
        for (String gunId : gunManager.getGunIds()) {
            GunStats gun = gunManager.getGun(gunId);
            if (gun != null) {
                data.setAmmo(gunId, gun.getMagazineSize());
            }
        }
        
        data.setReloading(false);
    }
    
    /**
     * チームスポーンポイントを設定
     */
    public void addSpawnPoint(Team team, Location location) {
        spawnPoints.get(team).add(location);
    }
    
    /**
     * チームスポーンポイントを取得
     */
    public Location getTeamSpawnPoint(Team team) {
        List<Location> spawns = spawnPoints.get(team);
        if (spawns == null || spawns.isEmpty()) {
            // デフォルトのワールドスポーンを使用
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        
        // ランダムにスポーンポイントを選択
        return spawns.get(new Random().nextInt(spawns.size()));
    }
    
    /**
     * スポーンポイントをクリア
     */
    public void clearSpawnPoints(Team team) {
        spawnPoints.get(team).clear();
    }
    
    /**
     * プレイヤー統計を取得
     */
    public PlayerStats getStats(UUID playerId) {
        return playerStats.computeIfAbsent(playerId, k -> new PlayerStats());
    }
    
    /**
     * すべてのプレイヤーデータをクリア
     */
    public void clearAll() {
        playerGunDataMap.clear();
        respawningPlayers.clear();
        playerStats.clear();
    }
    
    /**
     * プレイヤー統計クラス
     */
    public static class PlayerStats {
        private int kills = 0;
        private int deaths = 0;
        private int headshots = 0;
        private int assists = 0;
        
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public int getHeadshots() { return headshots; }
        public int getAssists() { return assists; }
        
        public void incrementKills() { kills++; }
        public void incrementDeaths() { deaths++; }
        public void incrementHeadshots() { headshots++; }
        public void incrementAssists() { assists++; }
        
        public double getKDRatio() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }
        
        public void reset() {
            kills = 0;
            deaths = 0;
            headshots = 0;
            assists = 0;
        }
    }
}
