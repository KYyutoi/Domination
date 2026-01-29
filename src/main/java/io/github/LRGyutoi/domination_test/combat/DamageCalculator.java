package io.github.LRGyutoi.domination_test.combat;

import io.github.LRGyutoi.domination_test.gun.GunStats;
import io.github.LRGyutoi.domination_test.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * サーバー権威型ダメージ計算クラス
 * ヘッドショット倍率、距離減衰、防具貫通などを計算
 */
public class DamageCalculator {
    
    private final ConfigManager configManager;
    
    public DamageCalculator(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * 最終ダメージを計算
     * @param gun 使用した銃
     * @param hitLocation ヒット位置
     * @param target ターゲットプレイヤー
     * @param shooter 射撃者
     * @param distance 射撃距離
     * @return 計算されたダメージ結果
     */
    public DamageResult calculateDamage(GunStats gun, Location hitLocation, 
                                         Player target, Player shooter, double distance) {
        double baseDamage = gun.getDamage();
        boolean isHeadshot = false;
        double multiplier = 1.0;
        
        // ヘッドショット判定
        if (HitboxDetector.isHeadshot(hitLocation, target)) {
            isHeadshot = true;
            multiplier *= configManager.getHeadshotMultiplier();
        }
        
        // 距離減衰（オプション）
        if (configManager.isDistanceFalloffEnabled()) {
            double falloffStart = configManager.getDistanceFalloffStart();
            double falloffEnd = configManager.getDistanceFalloffEnd();
            double minDamagePercent = configManager.getMinDamagePercent();
            
            if (distance > falloffStart) {
                double falloffRange = falloffEnd - falloffStart;
                double falloffProgress = Math.min(1.0, (distance - falloffStart) / falloffRange);
                double falloffMultiplier = 1.0 - (falloffProgress * (1.0 - minDamagePercent));
                multiplier *= falloffMultiplier;
            }
        }
        
        double finalDamage = baseDamage * multiplier;
        
        return new DamageResult(finalDamage, isHeadshot, multiplier);
    }
    
    /**
     * ダメージ計算結果
     */
    public static class DamageResult {
        private final double damage;
        private final boolean headshot;
        private final double multiplier;
        
        public DamageResult(double damage, boolean headshot, double multiplier) {
            this.damage = damage;
            this.headshot = headshot;
            this.multiplier = multiplier;
        }
        
        public double getDamage() { return damage; }
        public boolean isHeadshot() { return headshot; }
        public double getMultiplier() { return multiplier; }
    }
}
