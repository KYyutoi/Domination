package io.github.LRGyutoi.domination.domain.model.weapon;

/**
 * 武器エンティティ
 * 武器の定義とそのステータスを保持
 */
public class Weapon {
    private final WeaponId id;
    private final String displayName;
    private final WeaponStats stats;

    public Weapon(WeaponId id, String displayName, WeaponStats stats) {
        this.id = id;
        this.displayName = displayName;
        this.stats = stats;
    }

    public WeaponId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public WeaponStats getStats() {
        return stats;
    }

    /**
     * この武器がフルオートかどうか
     */
    public boolean isAutomatic() {
        return stats.isAutomatic();
    }

    /**
     * 発射間隔（ミリ秒）
     */
    public long getFireIntervalMillis() {
        return stats.fireRateTicks() * 50L;
    }

    /**
     * 散弾銃かどうか
     */
    public boolean isShotgun() {
        return stats.pelletsPerShot() > 1;
    }
}
