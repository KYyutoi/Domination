package io.github.LRGyutoi.domination.infrastructure.adapter;

import io.github.LRGyutoi.domination.application.port.out.PlayerAdapter;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PlayerAdapterのBukkit実装
 */
public class BukkitPlayerAdapter implements PlayerAdapter {

    private final Map<String, Location> teamSpawnPoints = new HashMap<>();

    public BukkitPlayerAdapter() {
        // デフォルトスポーン地点（設定ファイルから読み込むべき）
        World world = Bukkit.getWorlds().get(0);
        teamSpawnPoints.put("RED", new Location(world, 0, 64, 50));
        teamSpawnPoints.put("BLUE", new Location(world, 0, 64, -50));
    }

    /**
     * スポーン地点を設定
     */
    public void setSpawnPoint(String teamId, Location location) {
        teamSpawnPoints.put(teamId, location);
    }

    @Override
    public void dealDamage(PlayerId playerId, double damage, PlayerId attackerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        // Minecraftの1ハートは2HP
        player.damage(damage);

        // ダメージソースを記録（キルログ用）
        // NOTE: Paper APIのカスタムダメージソースを使用可能
    }

    @Override
    public double getHealth(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return 0;
        return player.getHealth();
    }

    @Override
    public void setHealth(PlayerId playerId, double health) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.min(health, maxHealth));
    }

    @Override
    public void respawn(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        // 死亡状態なら強制リスポーン
        if (player.isDead()) {
            player.spigot().respawn();
        }
    }

    @Override
    public void teleportToSpawn(PlayerId playerId, String teamId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        Location spawnPoint = teamSpawnPoints.get(teamId);
        if (spawnPoint != null) {
            player.teleport(spawnPoint);
        }
    }

    @Override
    public void freeze(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        // 移動不可効果
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            Integer.MAX_VALUE,
            255,
            false,
            false,
            false
        ));

        // ジャンプ不可
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.JUMP_BOOST,
            Integer.MAX_VALUE,
            -128,
            false,
            false,
            false
        ));

        // 透明化
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY,
            Integer.MAX_VALUE,
            0,
            false,
            false,
            false
        ));
    }

    @Override
    public void unfreeze(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    private Player getPlayer(PlayerId playerId) {
        UUID uuid = UUID.fromString(playerId.getValue());
        return Bukkit.getPlayer(uuid);
    }
}
