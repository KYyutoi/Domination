package io.github.LRGyutoi.domination_test.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 設定管理クラス
 * すべてのプラグイン設定を一元管理
 */
public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    // === ダメージ設定 ===
    private double headshotMultiplier = 2.0;
    private boolean distanceFalloffEnabled = false;
    private double distanceFalloffStart = 30.0;
    private double distanceFalloffEnd = 100.0;
    private double minDamagePercent = 0.5;
    
    // === フレンドリーファイア設定 ===
    private boolean friendlyFireEnabled = false;
    
    // === 不正対策設定 ===
    private long damageCooldownMs = 50;
    private boolean debugMode = false;
    
    // === リスポーン設定 ===
    private int respawnDelayTicks = 100; // 5秒
    private boolean respawnWithFullAmmo = true;
    private boolean respawnWithFullHealth = true;
    
    // === 弾丸設定 ===
    private boolean bulletTrailEnabled = true;
    private int bulletTrailParticleCount = 3;
    private double bulletMaxRange = 200.0;
    private int bulletLifetimeTicks = 100;
    
    // === ゲーム設定 ===
    private int initialTickets = 200;
    private int ticketDrainInterval = 20; // 1秒
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    // デフォルト設定を生成
                    createDefaultConfig(configFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("config.ymlの作成に失敗: " + e.getMessage());
                return;
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 設定値を読み込み
        loadValues();
        
        plugin.getLogger().info("設定ファイルを読み込みました");
    }
    
    private void loadValues() {
        // ダメージ設定
        headshotMultiplier = config.getDouble("damage.headshot-multiplier", 2.0);
        distanceFalloffEnabled = config.getBoolean("damage.distance-falloff.enabled", false);
        distanceFalloffStart = config.getDouble("damage.distance-falloff.start-distance", 30.0);
        distanceFalloffEnd = config.getDouble("damage.distance-falloff.end-distance", 100.0);
        minDamagePercent = config.getDouble("damage.distance-falloff.min-damage-percent", 0.5);
        
        // フレンドリーファイア
        friendlyFireEnabled = config.getBoolean("combat.friendly-fire", false);
        
        // 不正対策
        damageCooldownMs = config.getLong("anti-cheat.damage-cooldown-ms", 50);
        debugMode = config.getBoolean("debug.enabled", false);
        
        // リスポーン
        respawnDelayTicks = config.getInt("respawn.delay-ticks", 100);
        respawnWithFullAmmo = config.getBoolean("respawn.full-ammo", true);
        respawnWithFullHealth = config.getBoolean("respawn.full-health", true);
        
        // 弾丸
        bulletTrailEnabled = config.getBoolean("bullet.trail.enabled", true);
        bulletTrailParticleCount = config.getInt("bullet.trail.particle-count", 3);
        bulletMaxRange = config.getDouble("bullet.max-range", 200.0);
        bulletLifetimeTicks = config.getInt("bullet.lifetime-ticks", 100);
        
        // ゲーム
        initialTickets = config.getInt("game.initial-tickets", 200);
        ticketDrainInterval = config.getInt("game.ticket-drain-interval", 20);
    }
    
    private void createDefaultConfig(File configFile) throws IOException {
        YamlConfiguration defaultConfig = new YamlConfiguration();
        
        // ダメージ設定
        defaultConfig.set("damage.headshot-multiplier", 2.0);
        defaultConfig.set("damage.distance-falloff.enabled", false);
        defaultConfig.set("damage.distance-falloff.start-distance", 30.0);
        defaultConfig.set("damage.distance-falloff.end-distance", 100.0);
        defaultConfig.set("damage.distance-falloff.min-damage-percent", 0.5);
        
        // 戦闘設定
        defaultConfig.set("combat.friendly-fire", false);
        
        // 不正対策
        defaultConfig.set("anti-cheat.damage-cooldown-ms", 50);
        
        // デバッグ
        defaultConfig.set("debug.enabled", false);
        
        // リスポーン
        defaultConfig.set("respawn.delay-ticks", 100);
        defaultConfig.set("respawn.full-ammo", true);
        defaultConfig.set("respawn.full-health", true);
        
        // 弾丸
        defaultConfig.set("bullet.trail.enabled", true);
        defaultConfig.set("bullet.trail.particle-count", 3);
        defaultConfig.set("bullet.max-range", 200.0);
        defaultConfig.set("bullet.lifetime-ticks", 100);
        
        // ゲーム
        defaultConfig.set("game.initial-tickets", 200);
        defaultConfig.set("game.ticket-drain-interval", 20);
        
        defaultConfig.save(configFile);
    }
    
    public void saveConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("config.ymlの保存に失敗: " + e.getMessage());
        }
    }
    
    public void reloadConfig() {
        loadConfig();
    }
    
    // === ゲッター ===
    
    public double getHeadshotMultiplier() { return headshotMultiplier; }
    public boolean isDistanceFalloffEnabled() { return distanceFalloffEnabled; }
    public double getDistanceFalloffStart() { return distanceFalloffStart; }
    public double getDistanceFalloffEnd() { return distanceFalloffEnd; }
    public double getMinDamagePercent() { return minDamagePercent; }
    public boolean isFriendlyFireEnabled() { return friendlyFireEnabled; }
    public long getDamageCooldownMs() { return damageCooldownMs; }
    public boolean isDebugMode() { return debugMode; }
    public int getRespawnDelayTicks() { return respawnDelayTicks; }
    public boolean isRespawnWithFullAmmo() { return respawnWithFullAmmo; }
    public boolean isRespawnWithFullHealth() { return respawnWithFullHealth; }
    public boolean isBulletTrailEnabled() { return bulletTrailEnabled; }
    public int getBulletTrailParticleCount() { return bulletTrailParticleCount; }
    public double getBulletMaxRange() { return bulletMaxRange; }
    public int getBulletLifetimeTicks() { return bulletLifetimeTicks; }
    public int getInitialTickets() { return initialTickets; }
    public int getTicketDrainInterval() { return ticketDrainInterval; }
    
    // === セッター ===
    
    public void setHeadshotMultiplier(double value) {
        this.headshotMultiplier = value;
        config.set("damage.headshot-multiplier", value);
    }
    
    public void setFriendlyFireEnabled(boolean value) {
        this.friendlyFireEnabled = value;
        config.set("combat.friendly-fire", value);
    }
    
    public void setDebugMode(boolean value) {
        this.debugMode = value;
        config.set("debug.enabled", value);
    }
}
