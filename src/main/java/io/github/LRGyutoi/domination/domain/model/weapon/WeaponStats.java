package io.github.LRGyutoi.domination.domain.model.weapon;

/**
 * 武器のステータスを表す値オブジェクト
 * 不変で、武器の特性を定義
 */
public record WeaponStats(
    double baseDamage,
    int fireRateTicks,
    int magazineSize,
    int reloadTimeTicks,
    double projectileSpeed,
    double spread,
    boolean isAutomatic,
    int pelletsPerShot,
    double headshotMultiplier,
    double effectiveRange,
    double moveSpeedModifier
) {
    /**
     * リロード時間をミリ秒で取得（1tick = 50ms）
     */
    public long reloadTimeMs() {
        return reloadTimeTicks * 50L;
    }

    /**
     * 発射レートをミリ秒で取得（1tick = 50ms）
     */
    public long fireRateMs() {
        return fireRateTicks * 50L;
    }

    /**
     * ビルダーパターンで生成
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double baseDamage = 5.0;
        private int fireRateTicks = 10;
        private int magazineSize = 10;
        private int reloadTimeTicks = 60;
        private double projectileSpeed = 5.0;
        private double spread = 0.0;
        private boolean isAutomatic = false;
        private int pelletsPerShot = 1;
        private double headshotMultiplier = 1.0;
        private double effectiveRange = 100.0;
        private double moveSpeedModifier = 1.0;

        public Builder baseDamage(double v) { this.baseDamage = v; return this; }
        public Builder fireRateTicks(int v) { this.fireRateTicks = v; return this; }
        public Builder magazineSize(int v) { this.magazineSize = v; return this; }
        public Builder reloadTimeTicks(int v) { this.reloadTimeTicks = v; return this; }
        public Builder projectileSpeed(double v) { this.projectileSpeed = v; return this; }
        public Builder spread(double v) { this.spread = v; return this; }
        public Builder isAutomatic(boolean v) { this.isAutomatic = v; return this; }
        public Builder pelletsPerShot(int v) { this.pelletsPerShot = v; return this; }
        public Builder headshotMultiplier(double v) { this.headshotMultiplier = v; return this; }
        public Builder effectiveRange(double v) { this.effectiveRange = v; return this; }
        public Builder moveSpeedModifier(double v) { this.moveSpeedModifier = v; return this; }

        public WeaponStats build() {
            return new WeaponStats(
                baseDamage, fireRateTicks, magazineSize, reloadTimeTicks,
                projectileSpeed, spread, isAutomatic, pelletsPerShot,
                headshotMultiplier, effectiveRange, moveSpeedModifier
            );
        }
    }
}
