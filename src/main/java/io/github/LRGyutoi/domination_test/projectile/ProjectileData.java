package io.github.LRGyutoi.domination_test.projectile;

import io.github.LRGyutoi.domination_test.gun.GunStats;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * 弾丸データクラス
 * 個別の弾丸の状態を保持
 */
public class ProjectileData {
    
    private final UUID id;
    private final UUID shooterId;
    private final GunStats gun;
    private final Location origin;
    private final Vector direction;
    private final long creationTime;
    
    private Location currentPosition;
    private int ticksAlive = 0;
    
    public ProjectileData(UUID id, UUID shooterId, GunStats gun, 
                          Location origin, Vector direction, long creationTime) {
        this.id = id;
        this.shooterId = shooterId;
        this.gun = gun;
        this.origin = origin.clone();
        this.direction = direction.clone().normalize();
        this.creationTime = creationTime;
        this.currentPosition = origin.clone();
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getShooterId() {
        return shooterId;
    }
    
    public GunStats getGun() {
        return gun;
    }
    
    public Location getOrigin() {
        return origin.clone();
    }
    
    public Vector getDirection() {
        return direction.clone();
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public Location getCurrentPosition() {
        return currentPosition.clone();
    }
    
    public void setCurrentPosition(Location position) {
        this.currentPosition = position.clone();
    }
    
    public int getTicksAlive() {
        return ticksAlive;
    }
    
    public void incrementTicks() {
        ticksAlive++;
    }
    
    /**
     * 移動距離を取得
     */
    public double getDistanceTraveled() {
        return origin.distance(currentPosition);
    }
}
