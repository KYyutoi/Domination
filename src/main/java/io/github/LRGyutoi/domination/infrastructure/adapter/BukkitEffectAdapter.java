package io.github.LRGyutoi.domination.infrastructure.adapter;

import io.github.LRGyutoi.domination.application.port.out.EffectAdapter;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * EffectAdapterのBukkit実装
 */
public class BukkitEffectAdapter implements EffectAdapter {

    @Override
    public void playHitEffect(double x, double y, double z, boolean isHeadshot) {
        Location loc = new Location(Bukkit.getWorlds().get(0), x, y, z);

        if (isHeadshot) {
            // ヘッドショットエフェクト（赤いパーティクル）
            loc.getWorld().spawnParticle(
                Particle.DUST,
                loc,
                10,
                0.1, 0.1, 0.1,
                0,
                new Particle.DustOptions(Color.RED, 1.5f)
            );
            // ヘッドショットサウンド
            loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.5f);
        } else {
            // 通常ヒットエフェクト
            loc.getWorld().spawnParticle(
                Particle.DUST,
                loc,
                5,
                0.1, 0.1, 0.1,
                0,
                new Particle.DustOptions(Color.WHITE, 1.0f)
            );
            loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 0.8f, 1.0f);
        }
    }

    @Override
    public void playMuzzleFlash(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        Location eyeLoc = player.getEyeLocation();
        Location flashLoc = eyeLoc.add(eyeLoc.getDirection().multiply(0.5));

        // マズルフラッシュ（火花）
        player.getWorld().spawnParticle(
            Particle.FLAME,
            flashLoc,
            3,
            0.05, 0.05, 0.05,
            0.02
        );

        // 発砲音
        player.getWorld().playSound(
            player.getLocation(),
            Sound.ENTITY_FIREWORK_ROCKET_BLAST,
            1.0f,
            0.5f
        );
    }

    @Override
    public void playReloadSound(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        player.playSound(
            player.getLocation(),
            Sound.BLOCK_IRON_DOOR_CLOSE,
            0.5f,
            1.2f
        );
    }

    @Override
    public void playKillSound(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        // キルサウンド（高めの音）
        player.playSound(
            player.getLocation(),
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            1.0f,
            1.0f
        );
    }

    @Override
    public void playDamageSound(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        player.playSound(
            player.getLocation(),
            Sound.ENTITY_PLAYER_HURT,
            0.8f,
            1.0f
        );
    }

    @Override
    public void playEmptySound(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return;

        player.playSound(
            player.getLocation(),
            Sound.UI_BUTTON_CLICK,
            0.5f,
            1.5f
        );
    }

    private Player getPlayer(PlayerId playerId) {
        UUID uuid = UUID.fromString(playerId.getValue());
        return Bukkit.getPlayer(uuid);
    }
}
