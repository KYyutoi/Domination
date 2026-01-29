package io.github.LRGyutoi.domination_test.domination;

import io.github.LRGyutoi.domination_test.team.Team;
import io.github.LRGyutoi.domination_test.team.TeamManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * 拠点（キャプチャポイント）クラス
 * 占領進行度、所有チームなどを管理
 */
public class CapturePoint {
    
    private final String id;
    private final String displayName;
    private final Location beaconLocation;
    private final double captureRadius;
    private Team owningTeam;
    private final Map<Team, Integer> capturingPlayers = new HashMap<>();
    private double captureProgress = 0.0; // -100 (青) ～ +100 (赤)
    
    private static final double CAPTURE_SPEED = 1.0; // 1プレイヤーあたり秒間1%
    private static final double CAPTURE_THRESHOLD = 75.0; // 占領完了の閾値
    
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
    
    public double getCaptureProgress() {
        return captureProgress;
    }
    
    /**
     * 拠点の状態を更新
     */
    public void update(List<Player> nearbyPlayers) {
        capturingPlayers.clear();
        TeamManager teamManager = TeamManager.getInstance();
        
        // 範囲内のプレイヤーをチームごとにカウント
        for (Player player : nearbyPlayers) {
            if (player.getLocation().distance(beaconLocation) <= captureRadius) {
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
        if (captureProgress >= CAPTURE_THRESHOLD) {
            owningTeam = Team.RED;
        } else if (captureProgress <= -CAPTURE_THRESHOLD) {
            owningTeam = Team.BLUE;
        } else {
            owningTeam = Team.NEUTRAL;
        }
        
        // 占領完了時の通知
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
    
    public int getPlayersInZone(Team team) {
        return capturingPlayers.getOrDefault(team, 0);
    }
    
    /**
     * 拠点をリセット
     */
    public void reset() {
        captureProgress = 0.0;
        owningTeam = Team.NEUTRAL;
        capturingPlayers.clear();
    }
}
