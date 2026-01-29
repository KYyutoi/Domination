package io.github.LRGyutoi.domination.infrastructure.adapter;

import io.github.LRGyutoi.domination.application.port.out.ProjectileAdapter;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProjectileAdapterのBukkit実装
 */
public class BukkitProjectileAdapter implements ProjectileAdapter {

    private final Map<UUID, ProjectileData> projectileDataMap = new ConcurrentHashMap<>();

    @Override
    public UUID spawnProjectile(PlayerId shooterId, WeaponId weaponId, WeaponStats weaponStats) {
        Player shooter = getPlayer(shooterId);
        if (shooter == null) return null;

        Location eyeLocation = shooter.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // 弾丸として雪玉を使用（速度調整可能）
        Snowball projectile = shooter.launchProjectile(Snowball.class);
        projectile.setVelocity(direction.multiply(weaponStats.projectileSpeed()));

        // データを登録
        UUID projectileId = projectile.getUniqueId();
        ProjectileData data = new ProjectileData(
            shooterId,
            weaponId.getValue(),
            weaponStats.baseDamage(),
            System.currentTimeMillis()
        );

        projectileDataMap.put(projectileId, data);

        return projectileId;
    }

    @Override
    public void registerProjectile(UUID projectileId, ProjectileData data) {
        projectileDataMap.put(projectileId, data);
    }

    @Override
    public ProjectileData getProjectileData(UUID projectileId) {
        return projectileDataMap.get(projectileId);
    }

    @Override
    public void removeProjectile(UUID projectileId) {
        projectileDataMap.remove(projectileId);
    }

    private Player getPlayer(PlayerId playerId) {
        UUID uuid = UUID.fromString(playerId.getValue());
        return Bukkit.getPlayer(uuid);
    }

    /**
     * 古い弾丸データをクリーンアップ
     */
    public void cleanup(long maxAgeMs) {
        long now = System.currentTimeMillis();
        projectileDataMap.entrySet().removeIf(entry ->
            now - entry.getValue().spawnTime() > maxAgeMs
        );
    }
}
