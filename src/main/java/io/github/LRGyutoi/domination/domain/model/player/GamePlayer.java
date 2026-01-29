package io.github.LRGyutoi.domination.domain.model.player;

import io.github.LRGyutoi.domination.domain.model.team.TeamId;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ゲーム中のプレイヤーを表すエンティティ
 * Minecraft APIに依存しない純粋なドメインモデル
 */
public class GamePlayer {
    private final PlayerId id;
    private final String name;
    private TeamId teamId;
    private final PlayerStats stats;

    // 武器状態
    private WeaponId currentWeaponId;
    private final Map<WeaponId, Integer> ammunition;
    private boolean isReloading;
    private long lastShotTimeMillis;

    // 戦闘状態
    private boolean isAlive;
    private long deathTimeMillis;
    private boolean inGame;

    public GamePlayer(PlayerId id, String name) {
        this.id = id;
        this.name = name;
        this.teamId = TeamId.NEUTRAL;
        this.stats = new PlayerStats();
        this.ammunition = new HashMap<>();
        this.isReloading = false;
        this.lastShotTimeMillis = 0;
        this.isAlive = true;
        this.deathTimeMillis = 0;
        this.inGame = false;
    }

    /**
     * ファクトリメソッド
     */
    public static GamePlayer create(PlayerId id, String name) {
        return new GamePlayer(id, name);
    }

    // === クエリ ===

    public PlayerId getId() { return id; }
    public String getName() { return name; }
    public TeamId getTeamId() { return teamId; }
    public PlayerStats getStats() { return stats; }
    public boolean isReloading() { return isReloading; }
    public boolean isAlive() { return isAlive; }
    public long getLastShotTimeMillis() { return lastShotTimeMillis; }

    /**
     * ゲームに参加しているかどうか（チームに所属しているか）
     */
    public boolean isInGame() {
        return inGame && !teamId.isNeutral();
    }

    public Optional<WeaponId> getCurrentWeaponId() {
        return Optional.ofNullable(currentWeaponId);
    }

    public int getAmmo(WeaponId weaponId) {
        return ammunition.getOrDefault(weaponId, 0);
    }

    public boolean hasAmmo(WeaponId weaponId) {
        return getAmmo(weaponId) > 0;
    }

    public boolean isOnTeam() {
        return !teamId.isNeutral();
    }

    public boolean isSameTeam(GamePlayer other) {
        return this.teamId.equals(other.teamId) && !this.teamId.isNeutral();
    }

    /**
     * 発射可能かどうかを判定
     * @param fireIntervalMillis 発射間隔（ミリ秒）
     */
    public boolean canFire(long fireIntervalMillis) {
        if (isReloading || !isAlive) {
            return false;
        }
        if (currentWeaponId == null) {
            return false;
        }
        if (!hasAmmo(currentWeaponId)) {
            return false;
        }
        long now = System.currentTimeMillis();
        return (now - lastShotTimeMillis) >= fireIntervalMillis;
    }

    // === コマンド ===

    public void assignToTeam(TeamId teamId) {
        this.teamId = teamId;
    }

    /**
     * チームに参加
     */
    public void joinTeam(TeamId teamId) {
        this.teamId = teamId;
        this.inGame = true;
    }

    /**
     * ゲームから離脱
     */
    public void leaveGame() {
        this.teamId = TeamId.NEUTRAL;
        this.inGame = false;
        resetForNewGame();
    }

    public void equipWeapon(WeaponId weaponId, int magazineSize) {
        this.currentWeaponId = weaponId;
        // 初回装備時のみ弾薬を設定
        ammunition.putIfAbsent(weaponId, magazineSize);
    }

    public void unequipWeapon() {
        this.currentWeaponId = null;
    }

    public void consumeAmmo() {
        if (currentWeaponId != null) {
            int current = ammunition.getOrDefault(currentWeaponId, 0);
            if (current > 0) {
                ammunition.put(currentWeaponId, current - 1);
            }
        }
    }

    public void recordShot() {
        this.lastShotTimeMillis = System.currentTimeMillis();
    }

    public void startReloading() {
        this.isReloading = true;
    }

    public void finishReloading(int magazineSize) {
        if (currentWeaponId != null) {
            ammunition.put(currentWeaponId, magazineSize);
        }
        this.isReloading = false;
    }

    public void cancelReloading() {
        this.isReloading = false;
    }

    public void die() {
        this.isAlive = false;
        this.deathTimeMillis = System.currentTimeMillis();
        this.stats.recordDeath();
        cancelReloading();
    }

    public void respawn() {
        this.isAlive = true;
        this.isReloading = false;
        this.lastShotTimeMillis = 0;
    }

    /**
     * リスポーン時に弾薬をリセット
     */
    public void resetAmmunition(Map<WeaponId, Integer> fullAmmo) {
        ammunition.clear();
        ammunition.putAll(fullAmmo);
    }

    public void resetForNewGame() {
        this.teamId = TeamId.NEUTRAL;
        this.stats.reset();
        this.ammunition.clear();
        this.isReloading = false;
        this.lastShotTimeMillis = 0;
        this.isAlive = true;
        this.currentWeaponId = null;
    }
}
