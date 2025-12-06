package io.github.LRGyutoi.domination_test;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
public class GunManager {
    private final Domination_test plugin;
    private final Map<String, GunStats> guns = new HashMap<>();
    private FileConfiguration gunsConfig;
    public GunManager(Domination_test plugin) {
        this.plugin = plugin;
        loadGuns();
    }
    public void loadGuns() {
        guns.clear();
        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        if (!gunsFile.exists()) {
            boolean created = plugin.getDataFolder().mkdirs();
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

    public boolean setGunDamage(String gunId, double damage) {
        GunStats gun = guns.get(gunId);
        if (gun == null) {
            return false;
        }

        gun.setDamage(damage);

        // guns.ymlファイルに保存
        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
        gunsConfig.set("guns." + gunId + ".damage", damage);

        try {
            gunsConfig.save(gunsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("guns.ymlの保存に失敗: " + e.getMessage());
            return false;
        }
    }

    public boolean setGunFireRate(String gunId, int fireRate) {
        GunStats gun = guns.get(gunId);
        if (gun == null) {
            return false;
        }

        gun.setFireRate(fireRate);

        // guns.ymlファイルに保存
        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
        gunsConfig.set("guns." + gunId + ".fire-rate", fireRate);

        try {
            gunsConfig.save(gunsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("guns.ymlの保存に失敗: " + e.getMessage());
            return false;
        }
    }

    public boolean setGunMagazineSize(String gunId, int magazineSize) {
        GunStats gun = guns.get(gunId);
        if (gun == null) {
            return false;
        }

        gun.setMagazineSize(magazineSize);

        // guns.ymlファイルに保存
        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
        gunsConfig.set("guns." + gunId + ".magazine-size", magazineSize);

        try {
            gunsConfig.save(gunsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("guns.ymlの保存に失敗: " + e.getMessage());
            return false;
        }
    }

    public boolean setGunReloadTime(String gunId, int reloadTime) {
        GunStats gun = guns.get(gunId);
        if (gun == null) {
            return false;
        }

        gun.setReloadTime(reloadTime);

        // guns.ymlファイルに保存
        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
        gunsConfig.set("guns." + gunId + ".reload-time", reloadTime);

        try {
            gunsConfig.save(gunsFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("guns.ymlの保存に失敗: " + e.getMessage());
            return false;
        }
    }
}