package io.github.LRGyutoi.domination.infrastructure.adapter;

import io.github.LRGyutoi.domination.application.port.out.WorldAdapter;
import io.github.LRGyutoi.domination.domain.model.player.PlayerId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * WorldAdapterのBukkit実装
 */
public class BukkitWorldAdapter implements WorldAdapter {

    @Override
    public double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public Position getPlayerPosition(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return new Position(0, 0, 0, "world");
        }

        Location loc = player.getLocation();
        return new Position(
            loc.getX(),
            loc.getY(),
            loc.getZ(),
            loc.getWorld().getName()
        );
    }

    @Override
    public Direction getPlayerDirection(PlayerId playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return new Direction(0, 0);
        }

        Location loc = player.getLocation();
        return new Direction(loc.getYaw(), loc.getPitch());
    }

    @Override
    public boolean isPlayerSneaking(PlayerId playerId) {
        Player player = getPlayer(playerId);
        return player != null && player.isSneaking();
    }

    @Override
    public boolean isPlayerOnline(PlayerId playerId) {
        Player player = getPlayer(playerId);
        return player != null && player.isOnline();
    }

    private Player getPlayer(PlayerId playerId) {
        UUID uuid = UUID.fromString(playerId.getValue());
        return Bukkit.getPlayer(uuid);
    }
}
