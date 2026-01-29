package io.github.LRGyutoi.domination.domain.model.combat;

/**
 * ダメージ計算結果を表す値オブジェクト
 * 不変（immutable）で、計算結果をカプセル化
 */
public record DamageResult(
    double finalDamage,
    HitPart hitPart,
    double appliedMultiplier,
    boolean isFatal
) {
    /**
     * ヘッドショットかどうか
     */
    public boolean isHeadshot() {
        return hitPart.isHeadshot();
    }

    /**
     * 致命的なダメージかどうか
     */
    public boolean causedDeath() {
        return isFatal;
    }

    /**
     * 致死ダメージかどうか
     */
    public boolean isLethal() {
        return isFatal;
    }

    /**
     * ダメージ結果を生成するファクトリメソッド
     */
    public static DamageResult of(double damage, HitPart hitPart, double multiplier, double targetHealth) {
        boolean fatal = damage >= targetHealth;
        return new DamageResult(damage, hitPart, multiplier, fatal);
    }
}
