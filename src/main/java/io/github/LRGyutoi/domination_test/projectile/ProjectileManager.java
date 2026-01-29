package io.github.LRGyutoi.domination_test.projectile;

import io.github.LRGyutoi.domination_test.Domination_test;
import io.github.LRGyutoi.domination_test.gun.GunStats;
import io.github.LRGyutoi.domination_test.combat.CombatValidator;
import io.github.LRGyutoi.domination_test.combat.DamageCalculator;
import io.github.LRGyutoi.domination_test.combat.HitboxDetector;
import io.github.LRGyutoi.domination_test.config.ConfigManager;
import io.github.LRGyutoi.domination_test.team.Team;
import io.github.LRGyutoi.domination_test.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 弾丸管理クラス
 * 弾丸の生成、追跡、ヒット判定を行う
 * Arrowの代わりにカスタム弾道処理を使用
 */
public class ProjectileManager {
    
    private final Domination_test plugin;
    private final ConfigManager configManager;
    private final DamageCalculator damageCalculator;
    private final CombatValidator combatValidator;
    private final TeamManager teamManager;
    
    // アクティブな弾丸
    private final Map<UUID, ProjectileData> activeProjectiles = new ConcurrentHashMap<>();
    
    // 弾道更新タスク
    private BukkitRunnable updateTask;
    
    public ProjectileManager(Domination_test plugin, ConfigManager configManager,
                            DamageCalculator damageCalculator, CombatValidator combatValidator,
                            TeamManager teamManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.damageCalculator = damageCalculator;
        this.combatValidator = combatValidator;
        this.teamManager = teamManager;
        
        startUpdateTask();
    }
    
    /**
     * 弾丸を発射
     * @param shooter 射撃者
     * @param gun 使用した銃
     * @param origin 発射位置
     * @param direction 発射方向
     */
    public void fireProjectile(Player shooter, GunStats gun, Location origin, Vector direction) {
        UUID projectileId = UUID.randomUUID();
        
        ProjectileData projectile = new ProjectileData(
            projectileId,
            shooter.getUniqueId(),
            gun,
            origin.clone(),
            direction.clone().normalize(),
            System.currentTimeMillis()
        );
        
        activeProjectiles.put(projectileId, projectile);
        
        // 発射音
        origin.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        
        if (configManager.isDebugMode()) {
            plugin.getLogger().info("[Projectile] 弾丸発射: " + projectileId + " by " + shooter.getName());
        }
    }
    
    /**
     * 弾道更新タスクを開始
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateProjectiles();
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 1L); // 毎tick更新
    }
    
    /**
     * すべての弾丸を更新
     */
    private void updateProjectiles() {
        long currentTime = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, ProjectileData> entry : activeProjectiles.entrySet()) {
            UUID projectileId = entry.getKey();
            ProjectileData projectile = entry.getValue();
            
            // 寿命チェック
            long age = currentTime - projectile.getCreationTime();
            if (age > configManager.getBulletLifetimeTicks() * 50L) {
                toRemove.add(projectileId);
                continue;
            }
            
            // 弾丸を更新
            if (!updateProjectile(projectile)) {
                toRemove.add(projectileId);
            }
        }
        
        // 消えた弾丸を削除
        for (UUID id : toRemove) {
            removeProjectile(id);
        }
    }
    
    /**
     * 個別の弾丸を更新
     * @return 弾丸がまだアクティブな場合true
     */
    private boolean updateProjectile(ProjectileData projectile) {
        Location currentPos = projectile.getCurrentPosition();
        Vector direction = projectile.getDirection();
        double speed = projectile.getGun().getProjectileSpeed();
        
        // 次の位置を計算
        Vector movement = direction.clone().multiply(speed);
        Location nextPos = currentPos.clone().add(movement);
        
        // ブロックとの衝突判定
        if (nextPos.getBlock().getType().isSolid()) {
            spawnHitParticles(nextPos, null);
            return false; // 弾丸消滅
        }
        
        // プレイヤーとの当たり判定
        Player shooter = plugin.getServer().getPlayer(projectile.getShooterId());
        if (shooter == null) return false;
        
        // 発射地点からの距離チェック
        double distanceFromOrigin = projectile.getOrigin().distance(nextPos);
        if (distanceFromOrigin > configManager.getBulletMaxRange()) {
            return false; // 射程外
        }
        
        // 近くのプレイヤーをチェック
        Collection<Player> nearbyPlayers = nextPos.getWorld().getNearbyPlayers(nextPos, speed + 1);
        
        for (Player target : nearbyPlayers) {
            // 自分自身はスキップ
            if (target.equals(shooter)) continue;
            
            // レイキャストでヒット判定
            Location hitLocation = HitboxDetector.raycastToPlayer(
                currentPos, direction, target, speed
            );
            
            if (hitLocation != null) {
                // ダメージ検証
                if (!combatValidator.validateDamage(shooter, target, projectile.getId())) {
                    continue;
                }
                
                // ダメージ計算
                double distance = projectile.getOrigin().distance(hitLocation);
                DamageCalculator.DamageResult result = damageCalculator.calculateDamage(
                    projectile.getGun(), hitLocation, target, shooter, distance
                );
                
                // ダメージ適用
                applyDamage(shooter, target, result, projectile.getGun());
                
                // ヒットエフェクト
                spawnHitParticles(hitLocation, target);
                
                return false; // 最初にヒットした対象のみ
            }
        }
        
        // 弾道パーティクル
        if (configManager.isBulletTrailEnabled()) {
            spawnTrailParticles(currentPos, nextPos);
        }
        
        // 位置を更新
        projectile.setCurrentPosition(nextPos);
        
        return true;
    }
    
    /**
     * ダメージを適用
     */
    private void applyDamage(Player shooter, Player victim, 
                             DamageCalculator.DamageResult result, GunStats gun) {
        // サーバー権威型ダメージ適用
        double finalDamage = result.getDamage();
        
        // Minecraftのダメージシステムを使用（防具計算をスキップする場合はsetHealthを直接使用）
        victim.damage(finalDamage, shooter);
        
        // ヘッドショット通知
        if (result.isHeadshot()) {
            shooter.sendActionBar(Component.text("§c§lヘッドショット！ §7x" + 
                String.format("%.1f", result.getMultiplier())));
            shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
            
            // 被害者にも通知
            victim.sendMessage(Component.text(shooter.getName() + " にヘッドショットされました！", NamedTextColor.RED));
        }
        
        // ヒットマーカー
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.0f);
        
        // キルログ（死亡時）
        if (victim.getHealth() - finalDamage <= 0) {
            broadcastKill(shooter, victim, gun, result.isHeadshot());
        }
        
        if (configManager.isDebugMode()) {
            plugin.getLogger().info(String.format(
                "[Damage] %s -> %s: %.1f damage (headshot: %s)",
                shooter.getName(), victim.getName(), finalDamage, result.isHeadshot()
            ));
        }
    }
    
    /**
     * キルログをブロードキャスト
     */
    private void broadcastKill(Player killer, Player victim, GunStats gun, boolean headshot) {
        Team killerTeam = teamManager.getPlayerTeam(killer);
        Team victimTeam = teamManager.getPlayerTeam(victim);
        
        String headshotText = headshot ? " §e[HS]" : "";
        String message = killerTeam.getColor() + killer.getName() + " §7[" + 
                        gun.getName() + "§7]" + headshotText + " " + 
                        victimTeam.getColor() + victim.getName();
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(Component.text(message));
        }
    }
    
    /**
     * 弾道パーティクルを生成
     */
    private void spawnTrailParticles(Location from, Location to) {
        World world = from.getWorld();
        if (world == null) return;
        
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        direction.normalize();
        
        int particleCount = configManager.getBulletTrailParticleCount();
        double step = distance / particleCount;
        
        for (int i = 0; i < particleCount; i++) {
            Location particleLoc = from.clone().add(direction.clone().multiply(step * i));
            world.spawnParticle(
                Particle.CRIT,
                particleLoc,
                1,
                0, 0, 0,
                0
            );
        }
    }
    
    /**
     * ヒットパーティクルを生成
     */
    private void spawnHitParticles(Location location, Player victim) {
        World world = location.getWorld();
        if (world == null) return;
        
        if (victim != null) {
            // プレイヤーヒット
            world.spawnParticle(
                Particle.DAMAGE_INDICATOR,
                location,
                5,
                0.2, 0.2, 0.2,
                0
            );
        } else {
            // ブロックヒット
            world.spawnParticle(
                Particle.BLOCK,
                location,
                10,
                0.1, 0.1, 0.1,
                0,
                location.getBlock().getBlockData()
            );
            world.playSound(location, Sound.BLOCK_STONE_HIT, 0.5f, 1.0f);
        }
    }
    
    /**
     * 弾丸を削除
     */
    private void removeProjectile(UUID projectileId) {
        activeProjectiles.remove(projectileId);
        combatValidator.unregisterProjectile(projectileId);
    }
    
    /**
     * すべての弾丸をクリア
     */
    public void clearAll() {
        activeProjectiles.clear();
    }
    
    /**
     * タスクを停止
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        clearAll();
    }
    
    /**
     * アクティブな弾丸数を取得
     */
    public int getActiveProjectileCount() {
        return activeProjectiles.size();
    }
}
