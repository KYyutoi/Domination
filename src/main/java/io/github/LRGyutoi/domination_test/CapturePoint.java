package io.github.LRGyutoi.domination_test;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CapturePoint {
    private final String id;
    private final String displayName;
    private final Location beaconLocation;
    private final double captureRadius;
    private Team owningTeam;
    private final Map<Team, Integer> capturingPlayers = new HashMap<>();
    private double captureProgress = 0.0; // -100 (完全に青) から +100 (完全に赤)
    private static final double CAPTURE_SPEED = 1.0; // 1プレイヤーあたり秒間1%

    public CapturePoint(String id, String displayName, Location beaconLocation, double captureRadius) {
        this.id = id;
        this.displayName = displayName;
        this.beaconLocation = beaconLocation;
        this.captureRadius = captureRadius;
        this.owningTeam = Team.NEUTRAL;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getBeaconLocation() {
        return beaconLocation;
    }

    public Team getOwningTeam() {
        return owningTeam;
    }

    public double getCaptureRadius() {
        return captureRadius;
    }

    public void update(List<Player> nearbyPlayers) {
        capturingPlayers.clear();

        // 範囲内のプレイヤーをチームごとにカウント
        for (Player player : nearbyPlayers) {
            if (player.getLocation().distance(beaconLocation) <= captureRadius) {
                TeamManager teamManager = TeamManager.getInstance();
                Team team = teamManager.getPlayerTeam(player);
                if (team != Team.NEUTRAL) {
                    capturingPlayers.put(team, capturingPlayers.getOrDefault(team, 0) + 1);
                }
            }
        }

        int redPlayers = capturingPlayers.getOrDefault(Team.RED, 0);
        int bluePlayers = capturingPlayers.getOrDefault(Team.BLUE, 0);

        // 占領進行度を更新
        if (redPlayers > bluePlayers) {
            captureProgress += (redPlayers - bluePlayers) * CAPTURE_SPEED;
            if (captureProgress > 100) captureProgress = 100;
        } else if (bluePlayers > redPlayers) {
            captureProgress -= (bluePlayers - redPlayers) * CAPTURE_SPEED;
            if (captureProgress < -100) captureProgress = -100;
        }

        // 所有チームの更新
        Team previousOwner = owningTeam;
        if (captureProgress >= 75) {
            owningTeam = Team.RED;
        } else if (captureProgress <= -75) {
            owningTeam = Team.BLUE;
        } else {
            owningTeam = Team.NEUTRAL;
        }

        // 占領時の通知
        if (previousOwner != owningTeam && owningTeam != Team.NEUTRAL) {
            notifyCaptured();
        }

        updateBeacon();
        spawnParticles();
    }

    private void updateBeacon() {
        Block block = beaconLocation.getBlock();
        if (block.getType() == Material.BEACON) {
            Beacon beacon = (Beacon) block.getState();

            // ビーコンの色を変更
            org.bukkit.Color color = owningTeam.getBeaconColor();

            // ビーコンの効果を設定（色を示すため）
            if (owningTeam != Team.NEUTRAL) {
                beacon.setPrimaryEffect(PotionEffectType.SPEED);
                beacon.setSecondaryEffect(PotionEffectType.REGENERATION);
            } else {
                beacon.setPrimaryEffect(null);
                beacon.setSecondaryEffect(null);
            }

            beacon.update();
        }
    }

    private void spawnParticles() {
        if (owningTeam == Team.NEUTRAL) return;

        Location particleLoc = beaconLocation.clone().add(0.5, 1.5, 0.5);
        Particle.DustOptions dustOptions = new Particle.DustOptions(
            owningTeam.getBeaconColor(), 2.0f
        );

        beaconLocation.getWorld().spawnParticle(
            Particle.DUST,
            particleLoc,
            10,
            0.3, 0.3, 0.3,
            0,
            dustOptions
        );
    }

    private void notifyCaptured() {
        String message = owningTeam.getColor() + displayName + " が " +
                        owningTeam.getDisplayName() + owningTeam.getColor() + " チームに占領されました！";

        beaconLocation.getWorld().getPlayers().forEach(player -> {
            player.sendMessage(message);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        });
    }

    public boolean isContested() {
        int redPlayers = capturingPlayers.getOrDefault(Team.RED, 0);
        int bluePlayers = capturingPlayers.getOrDefault(Team.BLUE, 0);
        return redPlayers > 0 && bluePlayers > 0;
    }

    public int getCapturingPlayers(Team team) {
        return capturingPlayers.getOrDefault(team, 0);
    }
}

