package io.github.LRGyutoi.domination.domain.service;

import io.github.LRGyutoi.domination.domain.model.combat.HitPart;

/**
 * ヘッドショット判定ドメインサービス
 * 座標ベースの純粋な計算ロジック
 * 
 * Minecraft座標系:
 * - Y座標は高さ（上が正）
 * - プレイヤーの足元が基準位置
 */
public class HeadshotDetectionService {

    // 立ち状態のヒットボックス（足元からの相対位置）
    private static final double STANDING_HEAD_BOTTOM = 1.4;
    private static final double STANDING_HEAD_TOP = 1.8;
    private static final double STANDING_BODY_BOTTOM = 0.4;
    private static final double STANDING_BODY_TOP = 1.4;

    // しゃがみ状態のヒットボックス
    private static final double SNEAKING_HEAD_BOTTOM = 1.15;
    private static final double SNEAKING_HEAD_TOP = 1.5;
    private static final double SNEAKING_BODY_BOTTOM = 0.4;
    private static final double SNEAKING_BODY_TOP = 1.15;

    // 頭の水平方向の判定半径
    private static final double HEAD_RADIUS = 0.25;

    /**
     * 被弾部位を判定
     *
     * @param hitY          弾がヒットしたY座標
     * @param hitX          弾がヒットしたX座標
     * @param hitZ          弾がヒットしたZ座標
     * @param targetFeetY   ターゲットの足元Y座標
     * @param targetX       ターゲットのX座標（中心）
     * @param targetZ       ターゲットのZ座標（中心）
     * @param isSneaking    ターゲットがしゃがんでいるか
     * @return 被弾部位
     */
    public HitPart detectHitPart(
            double hitY, double hitX, double hitZ,
            double targetFeetY, double targetX, double targetZ,
            boolean isSneaking
    ) {
        double relativeY = hitY - targetFeetY;

        // ヒットボックスの境界を取得
        double headBottom = isSneaking ? SNEAKING_HEAD_BOTTOM : STANDING_HEAD_BOTTOM;
        double headTop = isSneaking ? SNEAKING_HEAD_TOP : STANDING_HEAD_TOP;
        double bodyBottom = isSneaking ? SNEAKING_BODY_BOTTOM : STANDING_BODY_BOTTOM;
        double bodyTop = isSneaking ? SNEAKING_BODY_TOP : STANDING_BODY_TOP;

        // Y座標が頭の範囲内かチェック
        if (relativeY >= headBottom && relativeY <= headTop) {
            // 水平方向の判定（頭は体より細い）
            double horizontalDistance = calculateHorizontalDistance(hitX, hitZ, targetX, targetZ);
            if (horizontalDistance <= HEAD_RADIUS) {
                return HitPart.HEAD;
            }
        }

        // 胴体判定
        if (relativeY >= bodyBottom && relativeY <= bodyTop) {
            return HitPart.BODY;
        }

        // 脚判定
        if (relativeY >= 0 && relativeY < bodyBottom) {
            return HitPart.LEGS;
        }

        // デフォルトは胴体
        return HitPart.BODY;
    }

    /**
     * 水平距離を計算
     */
    private double calculateHorizontalDistance(
            double x1, double z1,
            double x2, double z2
    ) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * ヘッドショットかどうかを簡易判定
     */
    public boolean isHeadshot(
            double hitY,
            double targetFeetY,
            boolean isSneaking
    ) {
        double relativeY = hitY - targetFeetY;
        double headBottom = isSneaking ? SNEAKING_HEAD_BOTTOM : STANDING_HEAD_BOTTOM;
        double headTop = isSneaking ? SNEAKING_HEAD_TOP : STANDING_HEAD_TOP;

        return relativeY >= headBottom && relativeY <= headTop;
    }
}
