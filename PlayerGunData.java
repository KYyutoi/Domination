package io.github.LRGyutoi.domination_test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerGunData {
    private final UUID playerId;
    private final Map<String, Integer> ammunition = new HashMap<>();
    private long lastShotTime = 0;
    private boolean isReloading = false;
    private String currentGun = null;

    public PlayerGunData(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getAmmo(String gunId) {
        return ammunition.getOrDefault(gunId, 0);
    }

    public boolean hasAmmoEntry(String gunId) {
        return ammunition.containsKey(gunId);
    }

    public void ensureAmmoInitialized(String gunId, int magazineSize) {
        ammunition.putIfAbsent(gunId, magazineSize);
    }

    public void setAmmo(String gunId, int amount) {
        ammunition.put(gunId, amount);
    }

    public void addAmmo(String gunId, int amount) {
        ammunition.put(gunId, getAmmo(gunId) + amount);
    }

    public void useAmmo(String gunId) {
        int current = getAmmo(gunId);
        if (current > 0) {
            ammunition.put(gunId, current - 1);
        }
    }

    public long getLastShotTime() {
        return lastShotTime;
    }

    public void setLastShotTime(long time) {
        this.lastShotTime = time;
    }

    public boolean isReloading() {
        return isReloading;
    }

    public void setReloading(boolean reloading) {
        isReloading = reloading;
    }

    public String getCurrentGun() {
        return currentGun;
    }

    public void setCurrentGun(String gunId) {
        this.currentGun = gunId;
    }
}
