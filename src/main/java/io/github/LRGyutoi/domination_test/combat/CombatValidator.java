package io.github.LRGyutoi.domination_test.combat;

import io.github.LRGyutoi.domination_test.team.Team;
import io.github.LRGyutoi.domination_test.team.TeamManager;
import io.github.LRGyutoi.domination_test.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 戦闘の妥当性検証クラス
 * チート対策（高速連射、多重ヒット、ラグ悪用など）
 */
public class CombatValidator {
    
    private final ConfigManager configManager;
    private final TeamManager teamManager;
    
    // 連射チェック用: プレイヤーUUID -> 直近の発射時刻リスト
    private final Map<UUID, LinkedList<Long>> shotHistory = new ConcurrentHashMap<>();
    
    // 多重ヒット防止用: 弾丸ID -> ヒット済みプレイヤーセット
    private final Map<UUID, Set<UUID>> projectileHits = new ConcurrentHashMap<>();
    
    // ダメージクールダウン: 被害者UUID -> 最後にダメージを受けた時刻
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    
    // 不正検出カウンター
    private final Map<UUID, Integer> violationCount = new ConcurrentHashMap<>();
    
    private static final int SHOT_HISTORY_SIZE = 20;
    private static final int VIOLATION_THRESHOLD = 10;
    
    public CombatValidator(ConfigManager configManager, TeamManager teamManager) {
        this.configManager = configManager;
        this.teamManager = teamManager;
    }
    
    /**
     * 発射が有効かチェック
     * @param player 射撃者
     * @param expectedFireRateTicks 銃の連射速度（tick）
     * @return 有効な場合true
     */
    public boolean validateShot(Player player, int expectedFireRateTicks) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long expectedDelayMs = expectedFireRateTicks * 50L; // 1 tick = 50ms
        
        LinkedList<Long> history = shotHistory.computeIfAbsent(playerId, k -> new LinkedList<>());
        
        // 直近の発射時刻をチェック
        if (!history.isEmpty()) {
            long lastShot = history.getLast();
            long actualDelay = currentTime - lastShot;
            
            // 許容誤差（ラグ考慮）: 期待値の70%未満なら不正
            long minAllowedDelay = (long)(expectedDelayMs * 0.7);
            
            if (actualDelay < minAllowedDelay) {
                recordViolation(player, "高速連射検出: " + actualDelay + "ms (期待: " + expectedDelayMs + "ms)");
                return false;
            }
        }
        
        // 履歴に追加
        history.addLast(currentTime);
        if (history.size() > SHOT_HISTORY_SIZE) {
            history.removeFirst();
        }
        
        // 平均連射速度をチェック（マクロ対策）
        if (history.size() >= 5) {
            long totalTime = history.getLast() - history.getFirst();
            double avgDelay = (double) totalTime / (history.size() - 1);
            
            // 平均が期待値の60%未満なら不正の可能性
            if (avgDelay < expectedDelayMs * 0.6) {
                recordViolation(player, "不自然な連射パターン検出");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * ダメージが有効かチェック
     * @param shooter 射撃者
     * @param victim 被害者
     * @param projectileId 弾丸ID
     * @return 有効な場合true
     */
    public boolean validateDamage(Player shooter, Player victim, UUID projectileId) {
        // 自分自身へのダメージは無効
        if (shooter.equals(victim)) {
            return false;
        }
        
        // フレンドリーファイアチェック
        if (!configManager.isFriendlyFireEnabled()) {
            Team shooterTeam = teamManager.getPlayerTeam(shooter);
            Team victimTeam = teamManager.getPlayerTeam(victim);
            
            if (shooterTeam != Team.NEUTRAL && shooterTeam == victimTeam) {
                return false;
            }
        }
        
        // 多重ヒット防止
        if (projectileId != null) {
            Set<UUID> hits = projectileHits.computeIfAbsent(projectileId, k -> new HashSet<>());
            if (hits.contains(victim.getUniqueId())) {
                return false; // 既にこの弾丸でヒット済み
            }
            hits.add(victim.getUniqueId());
        }
        
        // ダメージクールダウン（同一プレイヤーへの超高速連続ダメージ防止）
        long currentTime = System.currentTimeMillis();
        Long lastDamage = lastDamageTime.get(victim.getUniqueId());
        if (lastDamage != null) {
            long timeSinceLastDamage = currentTime - lastDamage;
            if (timeSinceLastDamage < configManager.getDamageCooldownMs()) {
                return false;
            }
        }
        lastDamageTime.put(victim.getUniqueId(), currentTime);
        
        return true;
    }
    
    /**
     * 弾丸の登録解除（弾丸が消えた時に呼ぶ）
     */
    public void unregisterProjectile(UUID projectileId) {
        projectileHits.remove(projectileId);
    }
    
    /**
     * プレイヤーのデータをクリア
     */
    public void clearPlayerData(UUID playerId) {
        shotHistory.remove(playerId);
        lastDamageTime.remove(playerId);
        violationCount.remove(playerId);
    }
    
    /**
     * 違反を記録
     */
    private void recordViolation(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        int count = violationCount.merge(playerId, 1, Integer::sum);
        
        // ログ出力（デバッグ用）
        if (configManager.isDebugMode()) {
            player.getServer().getLogger().warning(
                "[CombatValidator] " + player.getName() + ": " + reason + " (違反: " + count + "/" + VIOLATION_THRESHOLD + ")"
            );
        }
        
        // 閾値を超えたら警告
        if (count >= VIOLATION_THRESHOLD) {
            player.getServer().getLogger().warning(
                "[CombatValidator] " + player.getName() + " が不正行為の疑いがあります！"
            );
            // 管理者に通知、もしくは自動キックなどの処理をここに追加可能
            violationCount.put(playerId, 0); // リセット
        }
    }
    
    /**
     * 距離が有効範囲内かチェック
     */
    public boolean validateDistance(Player shooter, Player victim, double maxRange) {
        double distance = shooter.getLocation().distance(victim.getLocation());
        
        // 最大射程の1.5倍まで許容（ラグ考慮）
        if (distance > maxRange * 1.5) {
            recordViolation(shooter, "射程外ヒット: " + String.format("%.1f", distance) + "m");
            return false;
        }
        
        return true;
    }
    
    /**
     * 定期的なクリーンアップ（古いデータの削除）
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 60000; // 1分
        
        // 古い弾丸ヒット記録を削除
        projectileHits.entrySet().removeIf(entry -> {
            // 簡易的に古いエントリを削除（本来はタイムスタンプを持つべき）
            return false; // この実装では自動削除しない（unregisterProjectileで削除）
        });
        
        // 古いダメージ記録を削除
        lastDamageTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > expireTime
        );
    }
}
