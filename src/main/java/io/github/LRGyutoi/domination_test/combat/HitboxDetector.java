package io.github.LRGyutoi.domination_test.combat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * ヒットボックス判定クラス
 * プレイヤーの部位（頭、胴体）を判定
 */
public class HitboxDetector {
    
    // プレイヤーのヒットボックス定義（足元からの相対位置）
    // 立ち状態
    private static final double STANDING_HEAD_BOTTOM = 1.4;
    private static final double STANDING_HEAD_TOP = 1.8;
    private static final double STANDING_BODY_BOTTOM = 0.4;
    private static final double STANDING_BODY_TOP = 1.4;
    
    // しゃがみ状態
    private static final double SNEAKING_HEAD_BOTTOM = 1.15;
    private static final double SNEAKING_HEAD_TOP = 1.5;
    private static final double SNEAKING_BODY_BOTTOM = 0.4;
    private static final double SNEAKING_BODY_TOP = 1.15;
    
    // 頭の水平方向の判定範囲（中心からの距離）
    private static final double HEAD_RADIUS = 0.25;
    
    /**
     * ヘッドショットかどうかを判定
     * @param hitLocation 弾がヒットした位置
     * @param target ターゲットプレイヤー
     * @return ヘッドショットの場合true
     */
    public static boolean isHeadshot(Location hitLocation, Player target) {
        Location feetLocation = target.getLocation();
        double hitY = hitLocation.getY();
        double feetY = feetLocation.getY();
        double relativeY = hitY - feetY;
        
        // しゃがみ状態かどうかで判定範囲を変更
        double headBottom, headTop;
        if (target.isSneaking()) {
            headBottom = SNEAKING_HEAD_BOTTOM;
            headTop = SNEAKING_HEAD_TOP;
        } else {
            headBottom = STANDING_HEAD_BOTTOM;
            headTop = STANDING_HEAD_TOP;
        }
        
        // Y座標が頭の範囲内かチェック
        if (relativeY < headBottom || relativeY > headTop) {
            return false;
        }
        
        // 水平方向の判定（頭は体より細い）
        double headCenterX = feetLocation.getX();
        double headCenterZ = feetLocation.getZ();
        double hitX = hitLocation.getX();
        double hitZ = hitLocation.getZ();
        
        double horizontalDistance = Math.sqrt(
            Math.pow(hitX - headCenterX, 2) + Math.pow(hitZ - headCenterZ, 2)
        );
        
        return horizontalDistance <= HEAD_RADIUS;
    }
    
    /**
     * ヒット部位を取得
     * @param hitLocation 弾がヒットした位置
     * @param target ターゲットプレイヤー
     * @return ヒット部位
     */
    public static HitPart getHitPart(Location hitLocation, Player target) {
        if (isHeadshot(hitLocation, target)) {
            return HitPart.HEAD;
        }
        
        Location feetLocation = target.getLocation();
        double relativeY = hitLocation.getY() - feetLocation.getY();
        
        double bodyBottom, bodyTop;
        if (target.isSneaking()) {
            bodyBottom = SNEAKING_BODY_BOTTOM;
            bodyTop = SNEAKING_BODY_TOP;
        } else {
            bodyBottom = STANDING_BODY_BOTTOM;
            bodyTop = STANDING_BODY_TOP;
        }
        
        if (relativeY >= bodyBottom && relativeY <= bodyTop) {
            return HitPart.BODY;
        }
        
        if (relativeY < bodyBottom && relativeY >= 0) {
            return HitPart.LEGS;
        }
        
        return HitPart.UNKNOWN;
    }
    
    /**
     * レイキャストでプレイヤーとの交点を取得
     * @param origin レイの始点
     * @param direction レイの方向
     * @param target ターゲットプレイヤー
     * @param maxDistance 最大距離
     * @return 交点のLocation、ヒットしない場合はnull
     */
    public static Location raycastToPlayer(Location origin, Vector direction, 
                                            Player target, double maxDistance) {
        BoundingBox box = target.getBoundingBox();
        
        // レイとバウンディングボックスの交差判定
        Vector start = origin.toVector();
        Vector dir = direction.clone().normalize();
        
        double tMin = 0;
        double tMax = maxDistance;
        
        // X軸
        double invDirX = 1.0 / dir.getX();
        double t1 = (box.getMinX() - start.getX()) * invDirX;
        double t2 = (box.getMaxX() - start.getX()) * invDirX;
        
        if (invDirX < 0) { double temp = t1; t1 = t2; t2 = temp; }
        tMin = Math.max(tMin, t1);
        tMax = Math.min(tMax, t2);
        
        if (tMin > tMax) return null;
        
        // Y軸
        double invDirY = 1.0 / dir.getY();
        t1 = (box.getMinY() - start.getY()) * invDirY;
        t2 = (box.getMaxY() - start.getY()) * invDirY;
        
        if (invDirY < 0) { double temp = t1; t1 = t2; t2 = temp; }
        tMin = Math.max(tMin, t1);
        tMax = Math.min(tMax, t2);
        
        if (tMin > tMax) return null;
        
        // Z軸
        double invDirZ = 1.0 / dir.getZ();
        t1 = (box.getMinZ() - start.getZ()) * invDirZ;
        t2 = (box.getMaxZ() - start.getZ()) * invDirZ;
        
        if (invDirZ < 0) { double temp = t1; t1 = t2; t2 = temp; }
        tMin = Math.max(tMin, t1);
        tMax = Math.min(tMax, t2);
        
        if (tMin > tMax) return null;
        
        // 交点を計算
        Vector hitPoint = start.clone().add(dir.clone().multiply(tMin));
        return hitPoint.toLocation(origin.getWorld());
    }
    
    /**
     * ヒット部位の列挙
     */
    public enum HitPart {
        HEAD("頭", 1.0),
        BODY("胴体", 1.0),
        LEGS("脚", 0.8),
        UNKNOWN("不明", 1.0);
        
        private final String displayName;
        private final double damageMultiplier;
        
        HitPart(String displayName, double damageMultiplier) {
            this.displayName = displayName;
            this.damageMultiplier = damageMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public double getDamageMultiplier() { return damageMultiplier; }
    }
}
