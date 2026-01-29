package io.github.LRGyutoi.domination_test.gun;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 銃の設定を管理するクラス
 */
public class GunManager {
    private final JavaPlugin plugin;
    private final Map<String, GunStats> guns = new HashMap<>();
    private FileConfiguration gunsConfig;
    
    public GunManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadGuns();
    }
    
    public void loadGuns() {
        guns.clear();
        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        
        if (!gunsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("guns.yml")) {
                if (in != null) {
                    Files.copy(in, gunsFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("guns.ymlの作成に失敗: " + e.getMessage());
                return;
            }
        }
        
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
        ConfigurationSection gunsSection = gunsConfig.getConfigurationSection("guns");
        
        if (gunsSection == null) {
            plugin.getLogger().warning("guns.ymlに銃の設定がありません");
            return;
        }
        
        for (String gunId : gunsSection.getKeys(false)) {
            ConfigurationSection gunSection = gunsSection.getConfigurationSection(gunId);
            if (gunSection == null) continue;
            
            String name = gunSection.getString("name", gunId);
            double damage = gunSection.getDouble("damage", 5.0);
            int fireRate = gunSection.getInt("fire-rate", 10);
            int magazineSize = gunSection.getInt("magazine-size", 10);
            int reloadTime = gunSection.getInt("reload-time", 60);
            double projectileSpeed = gunSection.getDouble("projectile-speed", 3.0);
            double spread = gunSection.getDouble("spread", 0.5);
            boolean autoFire = gunSection.getBoolean("auto-fire", false);
            int pellets = gunSection.getInt("pellets", 1);
            
            GunStats stats = new GunStats(gunId, name, damage, fireRate, magazineSize, 
                                         reloadTime, projectileSpeed, spread, autoFire, pellets);
            
            // 追加のステータスを読み込み
            stats.setHeadshotMultiplier(gunSection.getDouble("headshot-multiplier", 1.0));
            stats.setRange(gunSection.getDouble("range", 100.0));
            stats.setMoveSpeedMultiplier(gunSection.getDouble("move-speed-multiplier", 1.0));
            
            guns.put(gunId, stats);
        }
        
        plugin.getLogger().info(guns.size() + "種類の銃を読み込みました");
    }
    
    public GunStats getGun(String id) {
        return guns.get(id);
    }
    
    public Set<String> getGunIds() {
        return guns.keySet();
    }
    
    public Map<String, GunStats> getAllGuns() {
        return new HashMap<>(guns);
    }
    
    public boolean setGunProperty(String gunId, String property, Object value) {
        GunStats gun = guns.get(gunId);
        if (gun == null) {
            return false;
        }
        
        switch (property.toLowerCase()) {
            case "damage" -> gun.setDamage((Double) value);
            case "fire-rate" -> gun.setFireRate((Integer) value);
            case "magazine-size" -> gun.setMagazineSize((Integer) value);
            case "reload-time" -> gun.setReloadTime((Integer) value);
            case "projectile-speed" -> gun.setProjectileSpeed((Double) value);
            case "spread" -> gun.setSpread((Double) value);
            case "auto-fire" -> gun.setAutoFire((Boolean) value);
            case "pellets" -> gun.setPellets((Integer) value);
            case "headshot-multiplier" -> gun.setHeadshotMultiplier((Double) value);
            case "range" -> gun.setRange((Double) value);
            default -> { return false; }
        }
        
        // ファイルに保存
        return saveGunProperty(gunId, property, value);
    }
    
    private boolean saveGunProperty(String gunId, String property, Object value) {
        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
        gunsConfig.set("guns." + gunId + "." + property, value);
        
        try {
            gunsConfig.save(gunsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("guns.ymlの保存に失敗: " + e.getMessage());
            return false;
        }
    }
    
    // 旧APIとの互換性
    public boolean setGunDamage(String gunId, double damage) {
        return setGunProperty(gunId, "damage", damage);
    }
    
    public boolean setGunFireRate(String gunId, int fireRate) {
        return setGunProperty(gunId, "fire-rate", fireRate);
    }
    
    public boolean setGunMagazineSize(String gunId, int magazineSize) {
        return setGunProperty(gunId, "magazine-size", magazineSize);
    }
    
    public boolean setGunReloadTime(String gunId, int reloadTime) {
        return setGunProperty(gunId, "reload-time", reloadTime);
    }
    
    public void reloadConfig() {
        loadGuns();
    }
}
