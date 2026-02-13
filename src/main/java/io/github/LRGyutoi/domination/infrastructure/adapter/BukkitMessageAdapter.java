package io.github.LRGyutoi.domination.infrastructure.adapter;

import io.github.LRGyutoi.domination.application.port.out.MessageAdapter;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

/**
 * MessageAdapterのBukkit実装
 */
public class BukkitMessageAdapter implements MessageAdapter {

    private static final String PREFIX = "§8[§6Domination§8] §f";
    private static final String ERROR_PREFIX = "§8[§cエラー§8] §c";
    private static final String SUCCESS_PREFIX = "§8[§a成功§8] §a";

    @Override
    public void sendMessage(PlayerId playerId, String message) {
        Player player = getPlayer(playerId);
        if (player != null) {
            player.sendMessage(PREFIX + message);
        }
    }

    @Override
    public void sendError(PlayerId playerId, String message) {
        Player player = getPlayer(playerId);
        if (player != null) {
            player.sendMessage(ERROR_PREFIX + message);
        }
    }

    @Override
    public void sendSuccess(PlayerId playerId, String message) {
        Player player = getPlayer(playerId);
        if (player != null) {
            player.sendMessage(SUCCESS_PREFIX + message);
        }
    }

    @Override
    public void broadcast(String message) {
        Bukkit.broadcast(deserializeColorCodes(PREFIX + message));
    }

    @Override
    public void sendActionBar(PlayerId playerId, String message) {
        Player player = getPlayer(playerId);
        if (player != null) {
            player.sendActionBar(deserializeColorCodes(message));
        }
    }

    @Override
    public void sendTitle(PlayerId playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = getPlayer(playerId);
        if (player != null) {
            Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
            );

            Title adventureTitle = Title.title(
                deserializeColorCodes(title),
                deserializeColorCodes(subtitle),
                times
            );

            player.showTitle(adventureTitle);
        }
    }

    @Override
    public void showKillFeed(PlayerId killerId, PlayerId victimId, String weaponName, boolean isHeadshot) {
        Player killer = getPlayer(killerId);
        Player victim = getPlayer(victimId);

        if (killer == null || victim == null) return;

        String killerName = killer.getName();
        String victimName = victim.getName();

        String headshotIcon = isHeadshot ? " §e✦ " : " ";
        String message = "§f" + killerName + headshotIcon + "§7[§f" + weaponName + "§7]" + headshotIcon + "§c" + victimName;

        broadcast(message);
    }

    private Player getPlayer(PlayerId playerId) {
        UUID uuid = UUID.fromString(playerId.getValue());
        return Bukkit.getPlayer(uuid);
    }

    /**
     * §カラーコードをAdventure Componentに変換
     * LegacyComponentSerializerを使用してセクション記号(§)ベースのカラーコードを正しくパース
     */
    private Component deserializeColorCodes(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }
}
