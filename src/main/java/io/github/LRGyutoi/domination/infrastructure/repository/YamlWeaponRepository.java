package io.github.LRGyutoi.domination.infrastructure.repository;

import io.github.LRGyutoi.domination.domain.model.weapon.Weapon;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponStats;
import io.github.LRGyutoi.domination.domain.repository.WeaponRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * YAML設定ファイルからの武器リポジトリ実装
 */
public class YamlWeaponRepository implements WeaponRepository {

    private final Plugin plugin;
    private final Map<String, Weapon> weapons = new ConcurrentHashMap<>();
    private final Logger logger;

    public YamlWeaponRepository(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    @Override
    public void save(Weapon weapon) {
        weapons.put(weapon.getId().getValue(), weapon);
    }

    @Override
    public Optional<Weapon> findById(WeaponId id) {
        return Optional.ofNullable(weapons.get(id.getValue()));
    }

    @Override
    public Optional<Weapon> findByName(String name) {
        return weapons.values().stream()
            .filter(w -> w.getDisplayName().equalsIgnoreCase(name))
            .findFirst();
    }

    @Override
    public Collection<Weapon> findAll() {
        return weapons.values();
    }

    @Override
    public void reload() {
        weapons.clear();

        File gunsFile = new File(plugin.getDataFolder(), "guns.yml");
        if (!gunsFile.exists()) {
            plugin.saveResource("guns.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(gunsFile);
        ConfigurationSection gunsSection = config.getConfigurationSection("guns");

        if (gunsSection == null) {
            logger.warning("guns.yml に 'guns' セクションが見つかりません");
            return;
        }

        for (String key : gunsSection.getKeys(false)) {
            try {
                Weapon weapon = loadWeapon(key, gunsSection.getConfigurationSection(key));
                weapons.put(key, weapon);
                logger.info("武器を読み込み: " + key);
            } catch (Exception e) {
                logger.warning("武器の読み込みに失敗: " + key + " - " + e.getMessage());
            }
        }

        logger.info("合計 " + weapons.size() + " 個の武器を読み込みました");
    }

    @Override
    public boolean exists(WeaponId id) {
        return weapons.containsKey(id.getValue());
    }

    private Weapon loadWeapon(String id, ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("武器セクションがnull");
        }

        String name = section.getString("name", id);
        String material = section.getString("material", "CROSSBOW");
        double damage = section.getDouble("damage", 10.0);
        int magazineSize = section.getInt("magazine_size", 30);
        int reloadTimeMs = section.getInt("reload_time", 2000);
        int fireRateMs = section.getInt("fire_rate", 100);
        double headshotMultiplier = section.getDouble("headshot_multiplier", 2.0);
        double projectileSpeed = section.getDouble("projectile_speed", 4.0);
        double spread = section.getDouble("spread", 0.0);
        double effectiveRange = section.getDouble("effective_range", 50.0);

        // msからticks変換（1tick = 50ms）
        int reloadTimeTicks = reloadTimeMs / 50;
        int fireRateTicks = fireRateMs / 50;

        WeaponStats stats = WeaponStats.builder()
            .baseDamage(damage)
            .magazineSize(magazineSize)
            .reloadTimeTicks(reloadTimeTicks)
            .fireRateTicks(fireRateTicks)
            .headshotMultiplier(headshotMultiplier)
            .projectileSpeed(projectileSpeed)
            .spread(spread)
            .effectiveRange(effectiveRange)
            .build();

        return new Weapon(WeaponId.of(id), name, stats);
    }
}
