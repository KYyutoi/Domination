package io.github.LRGyutoi.domination.domain.service;

import io.github.LRGyutoi.domination.domain.model.combat.DamageResult;
import io.github.LRGyutoi.domination.domain.model.combat.HitPart;
import io.github.LRGyutoi.domination.domain.model.weapon.WeaponStats;

/**
 * ダメージ計算ドメインサービス
 * 純粋な計算ロジックのみを持つ（外部依存なし）
 */
public class DamageCalculationService {

    private final double globalHeadshotMultiplier;
    private final boolean distanceFalloffEnabled;
    private final double falloffStartDistance;
    private final double falloffEndDistance;
    private final double minDamagePercent;

    public DamageCalculationService(
            double globalHeadshotMultiplier,
            boolean distanceFalloffEnabled,
            double falloffStartDistance,
            double falloffEndDistance,
            double minDamagePercent
    ) {
        this.globalHeadshotMultiplier = globalHeadshotMultiplier;
        this.distanceFalloffEnabled = distanceFalloffEnabled;
        this.falloffStartDistance = falloffStartDistance;
        this.falloffEndDistance = falloffEndDistance;
        this.minDamagePercent = minDamagePercent;
    }

    /**
     * ダメージを計算する
     *
     * @param weaponStats 使用武器のステータス
     * @param hitPart     被弾部位
     * @param distance    射撃距離
     * @param targetHealth ターゲットの現在HP
     * @return ダメージ計算結果
     */
    public DamageResult calculate(
            WeaponStats weaponStats,
            HitPart hitPart,
            double distance,
            double targetHealth
    ) {
        double baseDamage = weaponStats.baseDamage();
        double multiplier = 1.0;

        // ヘッドショット倍率
        if (hitPart.isHeadshot()) {
            multiplier *= globalHeadshotMultiplier * weaponStats.headshotMultiplier();
        }

        // 部位倍率
        multiplier *= hitPart.getBaseDamageMultiplier();

        // 距離減衰
        if (distanceFalloffEnabled) {
            multiplier *= calculateDistanceFalloff(distance);
        }

        double finalDamage = baseDamage * multiplier;

        return DamageResult.of(finalDamage, hitPart, multiplier, targetHealth);
    }

    /**
     * 距離減衰倍率を計算
     */
    private double calculateDistanceFalloff(double distance) {
        if (distance <= falloffStartDistance) {
            return 1.0;
        }

        if (distance >= falloffEndDistance) {
            return minDamagePercent;
        }

        double falloffRange = falloffEndDistance - falloffStartDistance;
        double distanceIntoFalloff = distance - falloffStartDistance;
        double falloffProgress = distanceIntoFalloff / falloffRange;

        return 1.0 - (falloffProgress * (1.0 - minDamagePercent));
    }

    /**
     * デフォルト設定で生成
     */
    public static DamageCalculationService withDefaults() {
        return new DamageCalculationService(
            2.0,    // globalHeadshotMultiplier
            false,  // distanceFalloffEnabled
            30.0,   // falloffStartDistance
            100.0,  // falloffEndDistance
            0.5     // minDamagePercent
        );
    }
}
